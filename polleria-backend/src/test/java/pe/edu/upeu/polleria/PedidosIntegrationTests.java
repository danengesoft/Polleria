package pe.edu.upeu.polleria;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.edu.upeu.polleria.catalogo.categoria.dto.CategoriaRequest;
import pe.edu.upeu.polleria.catalogo.categoria.service.CategoriaService;
import pe.edu.upeu.polleria.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.polleria.catalogo.producto.service.ProductoService;
import pe.edu.upeu.polleria.clientes.cliente.dto.ClienteRequest;
import pe.edu.upeu.polleria.clientes.cliente.service.ClienteService;
import pe.edu.upeu.polleria.pedidos.pedido.dto.*;
import pe.edu.upeu.polleria.pedidos.pedido.entity.*;
import pe.edu.upeu.polleria.pedidos.pedido.service.PedidoService;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.security.test.context.support.WithMockUser(username = "regression", roles = "ADMINISTRADOR")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class PedidosIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private CategoriaService categorias;
    @Autowired private ProductoService productos;
    @Autowired private ClienteService clientes;
    @Autowired private PedidoService pedidos;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    private final ObjectMapper json = new ObjectMapper();
    private Long categoriaId;
    private Long productoId;
    private Long clienteId;

    @BeforeEach
    void preparar() {
        CategoriaRequest categoria = new CategoriaRequest();
        categoria.setNombre("Pedido-test " + UUID.randomUUID());categoria.setActivo(true);
        categoriaId = categorias.crear(categoria).getId();
        productoId = productos.crear(producto("20.00", true)).getId();
        clienteId = clientes.crear(cliente("Dirección original")).getId();
    }

    private ProductoRequest producto(String precio, boolean disponible) {
        ProductoRequest request = new ProductoRequest();
        request.setNombre("Pollo de prueba");request.setPrecio(new BigDecimal(precio));
        request.setDisponible(disponible);request.setCategoriaId(categoriaId);
        return request;
    }

    private ClienteRequest cliente(String direccion) {
        ClienteRequest request = new ClienteRequest();
        request.setNombres("Cliente");request.setApellidos("Pedido");request.setTelefono("912345678");
        request.setDireccionPrincipal(direccion);
        return request;
    }

    private DetallePedidoRequest linea(Long producto, int cantidad) {
        DetallePedidoRequest request = new DetallePedidoRequest();
        request.setProductoId(producto);request.setCantidad(cantidad);return request;
    }

    private PedidoRequest request(TipoPedido tipo, DetallePedidoRequest... lineas) {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(clienteId);request.setTipoPedido(tipo);request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setDetalles(List.of(lineas));return request;
    }

    private PedidoResponse crear() { return pedidos.crear(request(TipoPedido.RECOJO, linea(productoId, 2))); }

    private void estado(Long id, EstadoPedido estado, int esperado) throws Exception {
        mvc.perform(patch("/api/v1/pedidos/{id}/estado", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + estado + "\"}"))
                .andExpect(status().is(esperado));
    }

    @Test
    void creaPedidoValidoConEstadosFechaYCalculosDelServidor() throws Exception {
        mvc.perform(post("/api/v1/pedidos").header("X-Trace-ID", "pedido-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request(TipoPedido.PRESENCIAL, linea(productoId, 2)))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(header().string("X-Trace-ID", "pedido-test"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.estadoPago").value("PENDIENTE"))
                .andExpect(jsonPath("$.fecha").isNotEmpty())
                .andExpect(jsonPath("$.cliente.id").value(clienteId))
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(20.00))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(40.00))
                .andExpect(jsonPath("$.total").value(40.00));
    }

    @Test
    void multiplesDetallesSumanImportesExactos() {
        Long bebida = productos.crear(producto("5.50", true)).getId();
        PedidoResponse pedido = pedidos.crear(request(TipoPedido.RECOJO, linea(productoId, 3), linea(bebida, 2)));
        assertThat(pedido.getDetalles()).hasSize(2);
        assertThat(pedido.getDetalles().get(0).getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(pedido.getDetalles().get(1).getSubtotal()).isEqualByComparingTo("11.00");
        assertThat(pedido.getTotal()).isEqualByComparingTo("71.00");
    }

    @Test
    void conservaPrecioYNombreHistoricosTrasCambiarCatalogo() {
        PedidoResponse pedido = crear();
        ProductoRequest cambio = producto("25.00", true);cambio.setNombre("Nombre nuevo");
        productos.actualizar(productoId, cambio);
        em.clear();
        PedidoResponse anterior = pedidos.obtener(pedido.getId());
        assertThat(anterior.getDetalles().getFirst().getPrecioUnitario()).isEqualByComparingTo("20.00");
        assertThat(anterior.getDetalles().getFirst().getNombreProducto()).isEqualTo("Pollo de prueba");
        assertThat(anterior.getTotal()).isEqualByComparingTo("40.00");
        assertThat(productos.obtener(productoId).getPrecio()).isEqualByComparingTo("25.00");
    }

    @Test
    void conservaDireccionYReferenciaDelPedidoTrasModificarCliente() {
        PedidoRequest request = request(TipoPedido.DELIVERY, linea(productoId, 1));
        request.setDireccionEntrega("Dirección al ordenar");request.setReferenciaEntrega("Puerta azul");
        PedidoResponse pedido = pedidos.crear(request);
        clientes.actualizar(clienteId, cliente("Dirección nueva"));em.clear();
        assertThat(pedidos.obtener(pedido.getId()).getDireccionEntrega()).isEqualTo("Dirección al ordenar");
        assertThat(pedidos.obtener(pedido.getId()).getReferenciaEntrega()).isEqualTo("Puerta azul");
    }

    @Test
    void clienteInexistenteEs404() throws Exception {
        PedidoRequest request = request(TipoPedido.RECOJO, linea(productoId, 1));request.setClienteId(Long.MAX_VALUE);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void productoInexistenteEs404() throws Exception {
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request(TipoPedido.RECOJO, linea(Long.MAX_VALUE, 1)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void productoNoDisponibleEs409() throws Exception {
        productos.actualizar(productoId, producto("20.00", false));
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request(TipoPedido.RECOJO, linea(productoId, 1)))))
                .andExpect(status().isConflict());
    }

    @Test
    void cantidadesCeroYNegativasSon400() throws Exception {
        for (int cantidad : new int[]{0, -1}) {
            mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(request(TipoPedido.RECOJO, linea(productoId, cantidad)))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void sinDetallesONulosEs400() throws Exception {
        PedidoRequest request = request(TipoPedido.RECOJO);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        request.setDetalles(null);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deliverySinDireccionOVaciaEs400() throws Exception {
        PedidoRequest request = request(TipoPedido.DELIVERY, linea(productoId, 1));
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        request.setDireccionEntrega("   ");
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recojoSinDireccionPermitido() {
        assertThat(crear().getDireccionEntrega()).isNull();
    }

    @Test
    void flujoPresencialYRecojoPermitidoSinRetrocesos() throws Exception {
        for (TipoPedido tipo : new TipoPedido[]{TipoPedido.PRESENCIAL, TipoPedido.RECOJO}) {
            Long id = pedidos.crear(request(tipo, linea(productoId, 1))).getId();
            for (EstadoPedido estado : new EstadoPedido[]{EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO, EstadoPedido.ENTREGADO}) {
                estado(id, estado, 200);
            }
            estado(id, EstadoPedido.PENDIENTE, 409);
            estado(id, EstadoPedido.CANCELADO, 409);
        }
    }

    @Test
    void rechazaSaltosYRepeticiones() throws Exception {
        Long id = crear().getId();
        estado(id, EstadoPedido.LISTO, 409);estado(id, EstadoPedido.PENDIENTE, 409);
    }

    @Test
    void deliveryNoPermiteEntregaDirectaNiDespachoDesdeEstaApi() throws Exception {
        PedidoRequest request = request(TipoPedido.DELIVERY, linea(productoId, 1));request.setDireccionEntrega("Calle 123");
        Long id = pedidos.crear(request).getId();
        estado(id, EstadoPedido.CONFIRMADO, 200);estado(id, EstadoPedido.EN_PREPARACION, 200);estado(id, EstadoPedido.LISTO, 200);
        estado(id, EstadoPedido.ENTREGADO, 409);estado(id, EstadoPedido.DESPACHADO, 409);
    }

    @Test
    void permiteCancelarPendienteYConfirmadoYCanceladoEsFinal() throws Exception {
        Long pendiente = crear().getId();estado(pendiente, EstadoPedido.CANCELADO, 200);
        estado(pendiente, EstadoPedido.CONFIRMADO, 409);
        Long confirmado = crear().getId();estado(confirmado, EstadoPedido.CONFIRMADO, 200);estado(confirmado, EstadoPedido.CANCELADO, 200);
    }

    @Test
    void cancelacionEnPreparacionOListoQuedaPendienteDeSeguridad() throws Exception {
        Long id = crear().getId();estado(id, EstadoPedido.CONFIRMADO, 200);estado(id, EstadoPedido.EN_PREPARACION, 200);
        estado(id, EstadoPedido.CANCELADO, 409);estado(id, EstadoPedido.LISTO, 200);estado(id, EstadoPedido.CANCELADO, 409);
    }

    @Test
    void pedidoPagadoNoSeCancela() throws Exception {
        Long id = crear().getId();Pedido pedido = em.find(Pedido.class, id);pedido.setEstadoPago(EstadoPago.PAGADO);em.flush();
        estado(id, EstadoPedido.CANCELADO, 409);
    }

    @Test
    void pedidoInexistenteEs404EnConsultaYEstado() throws Exception {
        mvc.perform(get("/api/v1/pedidos/9223372036854775807")).andExpect(status().isNotFound());
        estado(Long.MAX_VALUE, EstadoPedido.CONFIRMADO, 404);
    }

    @Test
    void noExisteDeleteNiPutGenerico() throws Exception {
        Long id = crear().getId();
        mvc.perform(delete("/api/v1/pedidos/{id}", id)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/pedidos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteReferenciadoNoSeBorraYNoCreaDependenciaInversa() throws Exception {
        crear();em.clear();
        mvc.perform(delete("/api/v1/clientes/{id}", clienteId)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cliente con operaciones asociadas; puede desactivarlo"));
    }

    @Test
    void clienteReferenciadoPuedeDesactivarseSinPerderPedido() {
        Long id = crear().getId();ClienteRequest cambio = cliente(null);cambio.setActivo(false);
        clientes.actualizar(clienteId, cambio);
        assertThat(pedidos.obtener(id).getCliente().id()).isEqualTo(clienteId);
        assertThat(clientes.obtener(clienteId).getActivo()).isFalse();
    }

    @Test
    void valoresObligatoriosYEstadosDesconocidosSon400() throws Exception {
        PedidoRequest request = request(TipoPedido.RECOJO, linea(productoId, 1));request.setClienteId(null);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/pedidos/1/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importesDelClienteHttpNoSonAutoritativos() throws Exception {
        String body = json.writeValueAsString(request(TipoPedido.RECOJO, linea(productoId, 2)));
        body = body.replace("\"cantidad\":2", "\"cantidad\":2,\"precioUnitario\":0.01,\"nombreProducto\":\"falso\",\"subtotal\":0.02");
        body = body.substring(0, body.length()-1) + ",\"total\":0.02}";
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(40.00))
                .andExpect(jsonPath("$.detalles[0].nombreProducto").value("Pollo de prueba"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackRealSinTransaccionExternaNiFilasParciales() throws Exception {
        // Los fixtures se crearon en transacciones confirmadas de sus servicios.
        // La petición fallida ejecuta su propia transacción; comprobar desde JDBC posterior.
        long cabeceras = jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.PEDIDOS", Long.class);
        long detalles = jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.DETALLE_PEDIDOS", Long.class);
        try {
            mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(request(TipoPedido.RECOJO, linea(productoId, 1), linea(Long.MAX_VALUE, 1)))))
                    .andExpect(status().isNotFound());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.PEDIDOS", Long.class)).isEqualTo(cabeceras);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.DETALLE_PEDIDOS", Long.class)).isEqualTo(detalles);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.PEDIDOS WHERE CLIENTE_ID=?", Long.class, clienteId)).isZero();
        } finally {
            // Solo recursos de este caso, incluso si una regresión dejara datos parciales.
            jdbc.update("DELETE FROM POLLERIA.DETALLE_PEDIDOS WHERE PEDIDO_ID IN (SELECT ID FROM POLLERIA.PEDIDOS WHERE CLIENTE_ID=?)", clienteId);
            jdbc.update("DELETE FROM POLLERIA.PEDIDOS WHERE CLIENTE_ID=?", clienteId);
            clientes.eliminar(clienteId);productos.eliminar(productoId);categorias.eliminar(categoriaId);
        }
    }

    @Test void rechazaCantidadDecimalSinTruncarlaNiCrearPedidoParcial() throws Exception {
        long antes = jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.PEDIDOS", Long.class);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
            .content("{\"clienteId\":"+clienteId+",\"tipoPedido\":\"RECOJO\",\"metodoPago\":\"EFECTIVO\",\"detalles\":[{\"productoId\":"+productoId+",\"cantidad\":1.5},{\"productoId\":9223372036854770000,\"cantidad\":1}]}"))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.PEDIDOS", Long.class)).isEqualTo(antes);
    }
}
