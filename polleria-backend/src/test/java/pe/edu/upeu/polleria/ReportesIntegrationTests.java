package pe.edu.upeu.polleria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ReportesIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private CategoriaService categorias;
    @Autowired private ProductoService productos;
    @Autowired private ClienteService clientes;
    @Autowired private PedidoService pedidos;
    @Autowired private EntityManager em;
    private Long clienteId;
    private Long otroClienteId;
    private Long productoId;
    private Long categoriaId;
    private Long primero;
    private Long ultimoDelDia;

    @BeforeEach
    void preparar() {
        CategoriaRequest categoria = new CategoriaRequest();categoria.setNombre("Reporte " + UUID.randomUUID());categoria.setActivo(true);
        categoriaId = categorias.crear(categoria).getId();
        productoId = productos.crear(producto("A histórico", "20.00")).getId();
        clienteId = crearCliente();otroClienteId = crearCliente();
        primero = crear(clienteId, TipoPedido.PRESENCIAL, EstadoPedido.ENTREGADO, EstadoPago.PAGADO,
                "2026-04-05T00:00:00", productoId, 2);
        productos.actualizar(productoId, producto("Z renombrado", "25.00"));
        ultimoDelDia = crear(clienteId, TipoPedido.RECOJO, EstadoPedido.ENTREGADO, EstadoPago.PAGADO,
                "2026-04-05T23:59:59.999999", productoId, 1);
        crear(clienteId, TipoPedido.RECOJO, EstadoPedido.CANCELADO, EstadoPago.PAGADO,
                "2026-04-05T12:00:00", productoId, 8);
        crear(clienteId, TipoPedido.PRESENCIAL, EstadoPedido.PENDIENTE, EstadoPago.PAGADO,
                "2026-04-05T12:00:00", productoId, 4);
        crear(clienteId, TipoPedido.DELIVERY, EstadoPedido.ENTREGADO, EstadoPago.PENDIENTE,
                "2026-04-05T12:00:00", productoId, 2);
        crear(clienteId, TipoPedido.DELIVERY, EstadoPedido.ENTREGADO, EstadoPago.PAGADO,
                "2026-04-06T00:00:00", productoId, 3);
        crear(otroClienteId, TipoPedido.RECOJO, EstadoPedido.PENDIENTE, EstadoPago.PENDIENTE,
                "2026-04-05T12:00:00", productoId, 1);
        em.flush();em.clear();
    }

    private Long crearCliente() {
        ClienteRequest cliente = new ClienteRequest();cliente.setNombres("Prueba");cliente.setApellidos("Reporte");cliente.setTelefono("912345678");
        return clientes.crear(cliente).getId();
    }

    private ProductoRequest producto(String nombre, String precio) {
        ProductoRequest producto = new ProductoRequest();producto.setNombre(nombre);producto.setPrecio(new BigDecimal(precio));
        producto.setDisponible(true);producto.setCategoriaId(categoriaId);return producto;
    }

    private Long crear(Long cliente, TipoPedido tipo, EstadoPedido estado, EstadoPago pago, String fecha, Long producto, int cantidad) {
        DetallePedidoRequest detalle = new DetallePedidoRequest();detalle.setProductoId(producto);detalle.setCantidad(cantidad);
        PedidoRequest request = new PedidoRequest();request.setClienteId(cliente);request.setTipoPedido(tipo);request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setDetalles(List.of(detalle));if (tipo == TipoPedido.DELIVERY) request.setDireccionEntrega("Dirección de prueba");
        Long id = pedidos.crear(request).getId();
        // Fixtures de reportes: no se crea un endpoint de pago ni se amplían transiciones de la API.
        Pedido entidad = em.find(Pedido.class, id);entidad.setEstado(estado);entidad.setEstadoPago(pago);entidad.setFecha(LocalDateTime.parse(fecha));
        em.flush();return id;
    }

    private PedidoFiltro filtro() {
        PedidoFiltro filtro = new PedidoFiltro();filtro.setClienteId(clienteId);return filtro;
    }

    private PedidoFiltro dia() {
        PedidoFiltro filtro = filtro();filtro.setFechaDesde(LocalDate.of(2026,4,5));filtro.setFechaHasta(LocalDate.of(2026,4,5));return filtro;
    }

    @Test
    void filtraPorEstado() {
        PedidoFiltro filtro = filtro();filtro.setEstado(EstadoPedido.CANCELADO);
        assertThat(pedidos.buscar(filtro,"fecha","DESC")).singleElement().extracting(PedidoResponse::getEstado).isEqualTo(EstadoPedido.CANCELADO);
    }

    @Test
    void filtraPorTipo() {
        PedidoFiltro filtro = filtro();filtro.setTipoPedido(TipoPedido.RECOJO);
        assertThat(pedidos.buscar(filtro,"fecha","DESC")).hasSize(2).allMatch(p -> p.getTipoPedido()==TipoPedido.RECOJO);
    }

    @Test
    void filtraPorClienteSinConsultarSuRepository() {
        PedidoFiltro filtro = new PedidoFiltro();filtro.setClienteId(otroClienteId);
        assertThat(pedidos.buscar(filtro,"fecha","DESC")).singleElement().extracting(p -> p.getCliente().id()).isEqualTo(otroClienteId);
    }

    @Test
    void fechasIncluyenPrimerYUltimoInstanteDelDiaPeroNoDiaSiguiente() {
        List<PedidoResponse> lista = pedidos.buscar(dia(),"fecha","ASC");
        assertThat(lista).hasSize(5);
        assertThat(lista.getFirst().getId()).isEqualTo(primero);
        assertThat(lista.getLast().getId()).isEqualTo(ultimoDelDia);
    }

    @Test
    void admiteLimitesUnilaterales() {
        PedidoFiltro desde = filtro();desde.setFechaDesde(LocalDate.of(2026,4,6));
        assertThat(pedidos.buscar(desde,"fecha","ASC")).hasSize(1);
        PedidoFiltro hasta = filtro();hasta.setFechaHasta(LocalDate.of(2026,4,5));
        assertThat(pedidos.buscar(hasta,"fecha","ASC")).hasSize(5);
    }

    @Test
    void combinaTodosLosFiltros() throws Exception {
        mvc.perform(get("/api/v1/pedidos").param("estado","ENTREGADO").param("tipoPedido","RECOJO")
                        .param("clienteId",clienteId.toString()).param("fechaDesde","2026-04-05").param("fechaHasta","2026-04-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ultimoDelDia));
    }

    @Test
    void consultaSinResultados() {
        PedidoFiltro filtro = new PedidoFiltro();filtro.setClienteId(Long.MAX_VALUE);
        assertThat(pedidos.buscar(filtro,"fecha","DESC")).isEmpty();
    }

    @Test
    void ordenaTotalYFechaConDesempateDeterminista() {
        List<PedidoResponse> asc = pedidos.buscar(dia(),"total","ASC");
        assertThat(asc).extracting(PedidoResponse::getTotal).isSorted();
        List<PedidoResponse> desc = pedidos.buscar(dia(),"fecha","DESC");
        assertThat(desc.getFirst().getId()).isEqualTo(ultimoDelDia);
    }

    @Test
    void rechazaCampoODireccionDeOrdenArbitrarios() throws Exception {
        mvc.perform(get("/api/v1/pedidos").param("ordenarPor","cliente.nombres")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/pedidos").param("direccion","cualquier-cosa")).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaFechasInvalidasInvertidasEIdInvalido() throws Exception {
        mvc.perform(get("/api/v1/pedidos").param("fechaDesde","2026-04-06").param("fechaHasta","2026-04-05"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/pedidos").param("fechaDesde","no-es-fecha")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/pedidos").param("clienteId","-1")).andExpect(status().isBadRequest());
    }

    @Test
    void resumenCuentaSoloVentasEntregadasYPagadasExcluyendoCancelados() {
        PedidoResumenReporte resumen = pedidos.resumen(dia());
        assertThat(resumen.cantidadPedidos()).isEqualTo(2);
        assertThat(resumen.totalMonetario()).isEqualByComparingTo("65.00");
        assertThat(resumen.ticketPromedio()).isEqualByComparingTo("32.50");
        PedidoFiltro cancelados = dia();cancelados.setEstado(EstadoPedido.CANCELADO);
        assertThat(pedidos.resumen(cancelados).cantidadPedidos()).isZero();
        assertThat(pedidos.resumen(cancelados).totalMonetario()).isEqualByComparingTo("0.00");
    }

    @Test
    void resumenVacioDaCeroSinDivisionPorCero() {
        PedidoFiltro filtro = new PedidoFiltro();filtro.setClienteId(Long.MAX_VALUE);
        PedidoResumenReporte resumen = pedidos.resumen(filtro);
        assertThat(resumen.cantidadPedidos()).isZero();assertThat(resumen.totalMonetario()).isEqualByComparingTo("0.00");
        assertThat(resumen.ticketPromedio()).isEqualByComparingTo("0.00");
    }

    @Test
    void ticketPromedioRedondeaHalfUpADosDecimales() {
        productos.actualizar(productoId, producto("Z renombrado","20.01"));
        crear(clienteId,TipoPedido.RECOJO,EstadoPedido.ENTREGADO,EstadoPago.PAGADO,"2026-04-05T13:00:00",productoId,1);
        assertThat(pedidos.resumen(dia()).ticketPromedio()).isEqualByComparingTo("28.34");
    }

    @Test
    void agrupaEstadosOperativosIncluyendoCanceladosYCeros() {
        List<EstadoPedidoReporte> reporte = pedidos.porEstado(dia());
        assertThat(reporte).hasSize(7);
        assertThat(reporte.stream().filter(r -> r.estado()==EstadoPedido.ENTREGADO).findFirst().orElseThrow().cantidadPedidos()).isEqualTo(3);
        assertThat(reporte.stream().filter(r -> r.estado()==EstadoPedido.CANCELADO).findFirst().orElseThrow().cantidadPedidos()).isEqualTo(1);
        assertThat(reporte.stream().mapToLong(EstadoPedidoReporte::cantidadPedidos).sum()).isEqualTo(5);
    }

    @Test
    void agrupaTipos() {
        List<TipoPedidoReporte> reporte = pedidos.porTipo(dia());
        assertThat(reporte).hasSize(3);
        assertThat(reporte.stream().filter(r -> r.tipoPedido()==TipoPedido.PRESENCIAL).findFirst().orElseThrow().cantidadPedidos()).isEqualTo(2);
        assertThat(reporte.stream().filter(r -> r.tipoPedido()==TipoPedido.RECOJO).findFirst().orElseThrow().cantidadPedidos()).isEqualTo(2);
        assertThat(reporte.stream().filter(r -> r.tipoPedido()==TipoPedido.DELIVERY).findFirst().orElseThrow().cantidadPedidos()).isEqualTo(1);
    }

    @Test
    void productosAgrupadosPorIdConImporteYNombreHistoricos() {
        ProductoVendidoReporte fila = pedidos.productosMasVendidos(dia()).getFirst();
        assertThat(fila.productoId()).isEqualTo(productoId);assertThat(fila.nombreProducto()).isEqualTo("A histórico");
        assertThat(fila.cantidadVendida()).isEqualTo(3);assertThat(fila.importeGenerado()).isEqualByComparingTo("65.00");
        assertThat(productos.obtener(productoId).getNombre()).isEqualTo("Z renombrado");
        assertThat(productos.obtener(productoId).getPrecio()).isEqualByComparingTo("25.00");
        assertThat(pedidos.productosMasVendidos(dia())).hasSize(1);
    }

    @Test
    void rankingOrdenaPorCantidadVendida() {
        Long bebida = productos.crear(producto("Bebida prueba","5.00")).getId();
        crear(clienteId,TipoPedido.RECOJO,EstadoPedido.ENTREGADO,EstadoPago.PAGADO,"2026-04-05T15:00:00",bebida,4);
        List<ProductoVendidoReporte> filas = pedidos.productosMasVendidos(dia());
        assertThat(filas.getFirst().productoId()).isEqualTo(bebida);
        assertThat(filas.getFirst().cantidadVendida()).isEqualTo(4);
        assertThat(filas.getFirst().importeGenerado()).isEqualByComparingTo("20.00");
    }

    @Test
    void reportesPorHttpTipadosMantienenTraceId() throws Exception {
        mvc.perform(get("/api/v1/pedidos/reportes/resumen").param("clienteId",clienteId.toString())
                        .param("fechaDesde","2026-04-05").param("fechaHasta","2026-04-05").header("X-Trace-ID","reporte-test"))
                .andExpect(status().isOk()).andExpect(header().string("X-Trace-ID","reporte-test"))
                .andExpect(jsonPath("$.cantidadPedidos").value(2)).andExpect(jsonPath("$.totalMonetario").value(65.00));
        mvc.perform(get("/api/v1/pedidos/reportes/productos-mas-vendidos").param("clienteId",clienteId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productoId").value(productoId));
    }

    @Test
    void corsPermiteSoloOrigenConfiguradoYExponeTraceId() throws Exception {
        mvc.perform(options("/api/v1/pedidos").header("Origin","http://localhost:4200")
                        .header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","X-Trace-ID"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mvc.perform(get("/api/v1/pedidos").param("clienteId",clienteId.toString()).header("Origin","http://localhost:4200"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Expose-Headers","X-Trace-ID, Location"));
        mvc.perform(options("/api/v1/pedidos").header("Origin","https://no-permitido.example")
                        .header("Access-Control-Request-Method","GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
