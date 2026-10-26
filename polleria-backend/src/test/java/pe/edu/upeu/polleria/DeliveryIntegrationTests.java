package pe.edu.upeu.polleria;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.upeu.polleria.catalogo.categoria.dto.CategoriaRequest;
import pe.edu.upeu.polleria.catalogo.categoria.service.CategoriaService;
import pe.edu.upeu.polleria.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.polleria.catalogo.producto.service.ProductoService;
import pe.edu.upeu.polleria.clientes.cliente.dto.ClienteRequest;
import pe.edu.upeu.polleria.clientes.cliente.service.ClienteService;
import pe.edu.upeu.polleria.pedidos.pedido.dto.*;
import pe.edu.upeu.polleria.pedidos.pedido.entity.*;
import pe.edu.upeu.polleria.pedidos.pedido.service.PedidoService;
import pe.edu.upeu.polleria.delivery.repartidor.service.RepartidorService;
import pe.edu.upeu.polleria.delivery.entrega.service.EntregaService;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;
import pe.edu.upeu.polleria.delivery.entrega.entity.EstadoEntrega;
import pe.edu.upeu.polleria.exception.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

// Committed fixtures allow verifying real transaction rollback and concurrent connections.
// Cleanup deletes ONLY the exact IDs created by each test, in FK order.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DeliveryIntegrationTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CategoriaService categorias;
    @Autowired
    ProductoService productos;
    @Autowired
    ClienteService clientes;
    @Autowired
    PedidoService pedidos;
    @Autowired
    RepartidorService repartidores;
    @Autowired
    EntregaService entregas;
    String subA, subB;
    Long repartidorA, repartidorB, categoria, producto, cliente;
    List<Long> pedidosCreados;

    RequestPostProcessor rol(String rol, String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("name", "Nombre desde JWT"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @BeforeEach
    void preparar() {
        pedidosCreados = new ArrayList<>();
        subA = "f13-a-" + UUID.randomUUID();
        subB = "f13-b-" + UUID.randomUUID();
        repartidorA = repartidores.activar(subA, "Repartidor A F13").id();
        repartidorB = repartidores.activar(subB, "Repartidor B F13").id();
        CategoriaRequest c = new CategoriaRequest();
        c.setNombre("F13 " + UUID.randomUUID());
        c.setActivo(true);
        categoria = categorias.crear(c).getId();
        ProductoRequest p = new ProductoRequest();
        p.setNombre("F13 Pollo");
        p.setPrecio(new BigDecimal("20.00"));
        p.setDisponible(true);
        p.setCategoriaId(categoria);
        producto = productos.crear(p).getId();
        ClienteRequest cl = new ClienteRequest();
        cl.setNombres("Cliente");
        cl.setApellidos("F13");
        cl.setTelefono("999000013");
        cl.setDireccionPrincipal("Dirección actual");
        cliente = clientes.crear(cl).getId();
    }

    @AfterEach
    void limpiar() {
        for (Long id : pedidosCreados) {
            jdbc.update("DELETE FROM POLLERIA.ENTREGAS WHERE PEDIDO_ID=?", id);
            jdbc.update("DELETE FROM POLLERIA.DETALLE_PEDIDOS WHERE PEDIDO_ID=?", id);
            jdbc.update("DELETE FROM POLLERIA.PEDIDOS WHERE ID=?", id);
        }
        if (producto != null)
            jdbc.update("DELETE FROM POLLERIA.PRODUCTOS WHERE ID=?", producto);
        if (categoria != null)
            jdbc.update("DELETE FROM POLLERIA.CATEGORIAS WHERE ID=?", categoria);
        if (cliente != null)
            jdbc.update("DELETE FROM POLLERIA.CLIENTES WHERE ID=?", cliente);
        jdbc.update("DELETE FROM POLLERIA.REPARTIDORES WHERE KEYCLOAK_SUB IN (?,?)", subA, subB);
    }

    Long pedido(TipoPedido tipo, MetodoPago pago, boolean listo) {
        PedidoRequest p = new PedidoRequest();
        p.setClienteId(cliente);
        p.setTipoPedido(tipo);
        p.setMetodoPago(pago);
        p.setDireccionEntrega("Dirección histórica F13");
        p.setReferenciaEntrega("Referencia histórica");
        DetallePedidoRequest d = new DetallePedidoRequest();
        d.setProductoId(producto);
        d.setCantidad(1);
        p.setDetalles(List.of(d));
        Long id = pedidos.crear(p).getId();
        pedidosCreados.add(id);
        if (listo)
            for (EstadoPedido e : List.of(EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO)) {
                EstadoPedidoRequest r = new EstadoPedidoRequest();
                r.setEstado(e);
                pedidos.cambiarEstado(id, r);
            }
        return id;
    }

    EntregaResponse asignada() {
        return entregas
                .asignar(new AsignacionRequest(pedido(TipoPedido.DELIVERY, MetodoPago.EFECTIVO, true), repartidorA));
    }

    void estadoPedido(Long id, String estado) {
        assertThat(pedidos.obtener(id).getEstado().name()).isEqualTo(estado);
    }

    @Test
    void perfilSoloDelJwtNoDelRequest() throws Exception {
        String sub = "f13-c-" + UUID.randomUUID();
        try {
            mvc.perform(post("/api/v1/delivery/repartidores/me").with(rol("REPARTIDOR", sub))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"keycloakSub\":\"ajeno\",\"nombreVisible\":\"Impostor\"}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.nombreVisible").value("Nombre desde JWT"))
                    .andExpect(jsonPath("$.keycloakSub").doesNotExist());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.REPARTIDORES WHERE KEYCLOAK_SUB=?",
                    Integer.class, sub)).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM POLLERIA.REPARTIDORES WHERE KEYCLOAK_SUB=?", sub);
        }
    }

    @Test
    void perfilDuplicado409YUnicidadReal() {
        assertThatThrownBy(() -> repartidores.activar(subA, "Otro")).isInstanceOf(BusinessConflictException.class);
        assertThatThrownBy(
                () -> jdbc.update("UPDATE POLLERIA.REPARTIDORES SET KEYCLOAK_SUB=? WHERE ID=?", subA, repartidorB))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void listaSoloActivosYNoExponeSub() throws Exception {
        jdbc.update("UPDATE POLLERIA.REPARTIDORES SET ACTIVO=0 WHERE ID=?", repartidorB);
        assertThat(repartidores.listarActivos()).extracting(r -> r.id()).contains(repartidorA)
                .doesNotContain(repartidorB);
        mvc.perform(get("/api/v1/delivery/repartidores/me").with(rol("REPARTIDOR", subA))).andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakSub").doesNotExist());
    }

    @Test
    void asignaListoConFechasServidorYVistaMinima() throws Exception {
        var e = asignada();
        assertThat(e.estado()).isEqualTo(EstadoEntrega.ASIGNADO);
        assertThat(e.fechaAsignacion()).isNotNull();
        assertThat(e.fechaSalida()).isNull();
        assertThat(e.pedido().telefono()).isEqualTo("999000013");
        mvc.perform(get("/api/v1/delivery/entregas/mias/{id}", e.id()).with(rol("REPARTIDOR", subA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.correo").doesNotExist())
                .andExpect(jsonPath("$.repartidor.keycloakSub").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = TipoPedido.class, names = { "PRESENCIAL", "RECOJO" })
    void rechazaTiposSinDelivery(TipoPedido tipo) {
        Long id = pedido(tipo, MetodoPago.EFECTIVO, true);
        assertThatThrownBy(() -> entregas.asignar(new AsignacionRequest(id, repartidorA)))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void rechazaDeliveryNoListo() {
        Long id = pedido(TipoPedido.DELIVERY, MetodoPago.EFECTIVO, false);
        assertThatThrownBy(() -> entregas.asignar(new AsignacionRequest(id, repartidorA)))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void rechazaPedidoYRepartidorInexistentes() {
        assertThatThrownBy(() -> entregas.asignar(new AsignacionRequest(Long.MAX_VALUE, repartidorA)))
                .isInstanceOf(ResourceNotFoundException.class);
        Long id = pedido(TipoPedido.DELIVERY, MetodoPago.EFECTIVO, true);
        assertThatThrownBy(() -> entregas.asignar(new AsignacionRequest(id, Long.MAX_VALUE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rechazaRepartidorInactivo() {
        jdbc.update("UPDATE POLLERIA.REPARTIDORES SET ACTIVO=0 WHERE ID=?", repartidorA);
        Long id = pedido(TipoPedido.DELIVERY, MetodoPago.EFECTIVO, true);
        assertThatThrownBy(() -> entregas.asignar(new AsignacionRequest(id, repartidorA)))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void segundaEntregaYUniqueOracleRechazados() {
        var e = asignada();
        assertThatThrownBy(() -> entregas.asignar(new AsignacionRequest(e.pedidoId(), repartidorB)))
                .isInstanceOf(BusinessConflictException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO POLLERIA.ENTREGAS(PEDIDO_ID,REPARTIDOR_ID,FECHA_ASIGNACION,ESTADO) VALUES(?,?,SYSTIMESTAMP,'ASIGNADO')",
                e.pedidoId(), repartidorB)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void otroRepartidorNoVeNiModificaEntrega() throws Exception {
        var e = asignada();
        mvc.perform(get("/api/v1/delivery/entregas/mias/{id}", e.id()).with(rol("REPARTIDOR", subB)))
                .andExpect(status().isNotFound());
        for (String op : List.of("iniciar", "entregar", "no-entregado"))
            mvc.perform(patch("/api/v1/delivery/entregas/mias/{id}/" + op, e.id()).with(rol("REPARTIDOR", subB))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"No autorizado\"}"))
                    .andExpect(status().isNotFound());
        assertThat(entregas.mias(subB)).isEmpty();
    }

    @Test
    void iniciarAtomicoConFechaSalida() {
        var e = asignada();
        var r = entregas.iniciar(subA, e.id());
        assertThat(r.estado()).isEqualTo(EstadoEntrega.EN_CAMINO);
        assertThat(r.fechaSalida()).isNotNull();
        estadoPedido(e.pedidoId(), "DESPACHADO");
    }

    @Test
    void efectivoEntregadoMarcaPagadoAtomico() {
        var e = asignada();
        entregas.iniciar(subA, e.id());
        var r = entregas.entregar(subA, e.id());
        assertThat(r.estado()).isEqualTo(EstadoEntrega.ENTREGADO);
        assertThat(r.fechaEntrega()).isNotNull();
        assertThat(r.pedido().estadoPedido()).isEqualTo("ENTREGADO");
        assertThat(r.pedido().estadoPago()).isEqualTo("PAGADO");
    }

    @ParameterizedTest
    @EnumSource(value = MetodoPago.class, names = { "YAPE", "PLIN", "TARJETA" })
    void noConfirmaPagoDigitalAlEntregar(MetodoPago metodo) throws Exception {
        var e = entregas.asignar(new AsignacionRequest(pedido(TipoPedido.DELIVERY, metodo, true), repartidorA));
        entregas.iniciar(subA, e.id());
        var r = entregas.entregar(subA, e.id());
        assertThat(r.pedido().estadoPago()).isEqualTo("PENDIENTE");
        mvc.perform(patch("/api/v1/pedidos/{id}/confirmar-pago", e.pedidoId()).with(rol("REPARTIDOR", subA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void incidenciaExigeObservacionYNoCancela() {
        var e = asignada();
        entregas.iniciar(subA, e.id());
        assertThatThrownBy(() -> entregas.noEntregado(subA, e.id(), new IncidenciaRequest("  ")))
                .isInstanceOf(InvalidRequestException.class);
        var r = entregas.noEntregado(subA, e.id(), new IncidenciaRequest("Cliente ausente"));
        assertThat(r.estado()).isEqualTo(EstadoEntrega.NO_ENTREGADO);
        assertThat(r.pedido().estadoPedido()).isEqualTo("DESPACHADO");
        assertThat(r.pedido().estadoPago()).isEqualTo("PENDIENTE");
    }

    @Test
    void reasignaYSegundoIntentoEntregaConservaIncidencia() {
        var e = asignada();
        entregas.iniciar(subA, e.id());
        entregas.noEntregado(subA, e.id(), new IncidenciaRequest("Incidencia inicial"));
        var r = entregas.reasignar(e.id(), new ReasignacionRequest(repartidorB));
        assertThat(r.estado()).isEqualTo(EstadoEntrega.ASIGNADO);
        assertThat(r.repartidor().id()).isEqualTo(repartidorB);
        assertThat(r.fechaSalida()).isNull();
        assertThat(r.fechaEntrega()).isNull();
        assertThat(r.observacion()).contains("Incidencia inicial");
        estadoPedido(e.pedidoId(), "DESPACHADO");
        entregas.iniciar(subB, e.id());
        estadoPedido(e.pedidoId(), "DESPACHADO");
        assertThat(entregas.entregar(subB, e.id()).pedido().estadoPedido()).isEqualTo("ENTREGADO");
        assertThat(entregas.mias(subA)).isEmpty();
    }

    @Test
    void noReasignaAsignadoNiPermiteSaltos() {
        var e = asignada();
        assertThatThrownBy(() -> entregas.reasignar(e.id(), new ReasignacionRequest(repartidorB)))
                .isInstanceOf(BusinessConflictException.class);
        assertThatThrownBy(() -> entregas.entregar(subA, e.id())).isInstanceOf(BusinessConflictException.class);
        assertThatThrownBy(() -> entregas.noEntregado(subA, e.id(), new IncidenciaRequest("x")))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void iniciarFallaPedidoYRevierteEntregaFlusheada() {
        var e = asignada();
        jdbc.update("UPDATE POLLERIA.PEDIDOS SET ESTADO='CANCELADO' WHERE ID=?", e.pedidoId());
        assertThatThrownBy(() -> entregas.iniciar(subA, e.id())).isInstanceOf(BusinessConflictException.class);
        assertThat(jdbc.queryForObject("SELECT ESTADO FROM POLLERIA.ENTREGAS WHERE ID=?", String.class, e.id()))
                .isEqualTo("ASIGNADO");
        assertThat(jdbc.queryForObject("SELECT FECHA_SALIDA FROM POLLERIA.ENTREGAS WHERE ID=?",
                java.sql.Timestamp.class, e.id())).isNull();
    }

    @Test
    void entregarFallaPedidoYRevierteEntregaFlusheada() {
        var e = asignada();
        entregas.iniciar(subA, e.id());
        jdbc.update("UPDATE POLLERIA.PEDIDOS SET ESTADO='LISTO' WHERE ID=?", e.pedidoId());
        assertThatThrownBy(() -> entregas.entregar(subA, e.id())).isInstanceOf(BusinessConflictException.class);
        assertThat(jdbc.queryForObject("SELECT ESTADO FROM POLLERIA.ENTREGAS WHERE ID=?", String.class, e.id()))
                .isEqualTo("EN_CAMINO");
        assertThat(jdbc.queryForObject("SELECT FECHA_ENTREGA FROM POLLERIA.ENTREGAS WHERE ID=?",
                java.sql.Timestamp.class, e.id())).isNull();
    }

    @Test
    void direccionHistoricaNoSeReconstruyeDelPerfil() {
        var e = asignada();
        jdbc.update("UPDATE POLLERIA.CLIENTES SET DIRECCION_PRINCIPAL='Nueva dirección actual' WHERE ID=?", cliente);
        assertThat(entregas.mia(subA, e.id()).pedido().direccionEntrega()).isEqualTo("Dirección histórica F13");
    }

    @Test
    void confirmaPagoDigitalInternoUnaSolaVez() throws Exception {
        Long id = pedido(TipoPedido.DELIVERY, MetodoPago.YAPE, true);
        mvc.perform(patch("/api/v1/pedidos/{id}/confirmar-pago", id).with(rol("CAJERO", "cajero")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoPago").value("PAGADO"));
        mvc.perform(patch("/api/v1/pedidos/{id}/confirmar-pago", id).with(rol("ADMINISTRADOR", "admin")))
                .andExpect(status().isConflict());
    }

    @Test
    void sinToken401YRolesAjenos403() throws Exception {
        mvc.perform(get("/api/v1/delivery/entregas")).andExpect(status().isUnauthorized());
        for (String role : List.of("CLIENTE", "ABASTECIMIENTO")) {
            mvc.perform(get("/api/v1/delivery/entregas").with(rol(role, "ajeno"))).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/delivery/repartidores/me").with(rol(role, "ajeno")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/delivery/entregas").with(rol("REPARTIDOR", subA))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = { "CAJERO", "ADMINISTRADOR" })
    void rolesInternosAsignan(String role) throws Exception {
        Long id = pedido(TipoPedido.DELIVERY, MetodoPago.EFECTIVO, true);
        mvc.perform(post("/api/v1/delivery/entregas").with(rol(role, "interno")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pedidoId\":" + id + ",\"repartidorId\":" + repartidorA + ",\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ASIGNADO"));
    }

    void concurrentes(Supplier<?> operacion) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++)
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        operacion.get();
                        return true;
                    } catch (BusinessConflictException ex) {
                        return false;
                    }
                }));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int exitos = 0;
            for (var f : futures)
                if (f.get(15, TimeUnit.SECONDS))
                    exitos++;
            assertThat(exitos).isEqualTo(1);
        }
    }

    @Test
    void asignacionConcurrenteSoloUnaFila() throws Exception {
        Long id = pedido(TipoPedido.DELIVERY, MetodoPago.EFECTIVO, true);
        concurrentes(() -> entregas.asignar(new AsignacionRequest(id, repartidorA)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.ENTREGAS WHERE PEDIDO_ID=?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void inicioConcurrenteUnaTransicion() throws Exception {
        var e = asignada();
        concurrentes(() -> entregas.iniciar(subA, e.id()));
        estadoPedido(e.pedidoId(), "DESPACHADO");
    }

    @Test
    void entregaConcurrenteUnaTransicion() throws Exception {
        var e = asignada();
        entregas.iniciar(subA, e.id());
        concurrentes(() -> entregas.entregar(subA, e.id()));
        estadoPedido(e.pedidoId(), "ENTREGADO");
    }
}
