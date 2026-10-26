package pe.edu.upeu.polleria;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.compra.entity.Compra;
import pe.edu.upeu.polleria.abastecimiento.compra.service.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.InsumoRequest;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;
import pe.edu.upeu.polleria.abastecimiento.insumo.service.InsumoService;
import pe.edu.upeu.polleria.abastecimiento.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.polleria.abastecimiento.proveedor.service.ProveedorService;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.service.InventarioService;
import pe.edu.upeu.polleria.delivery.entrega.entity.*;
import pe.edu.upeu.polleria.delivery.entrega.service.EntregaReporteService;
import pe.edu.upeu.polleria.delivery.repartidor.entity.Repartidor;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ReportesAmpliadosIntegrationTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;
    @Autowired
    CompraReporteService reportes;
    @Autowired
    CompraService compras;
    @Autowired
    InsumoService insumos;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    pe.edu.upeu.polleria.clientes.cliente.service.ClienteService clientes;
    @Autowired
    InventarioService inventario;
    @Autowired
    EntregaReporteService delivery;

    BigDecimal n(String v) {
        return new BigDecimal(v);
    }

    ReporteFiltro dia() {
        var f = new ReporteFiltro();
        f.setFechaDesde(LocalDate.of(2025, 2, 3));
        f.setFechaHasta(LocalDate.of(2025, 2, 3));
        return f;
    }

    Long a, b, proveedor, compra;

    void prepararCompras() {
        a = insumos.crear(new InsumoRequest("Aceite", null, UnidadMedida.L, true)).id();
        b = insumos.crear(new InsumoRequest("Papas", null, UnidadMedida.KG, true)).id();
        proveedor = proveedores.crear(new ProveedorRequest("Proveedor reportes", null, null, null, null, true)).id();
        compra = compras
                .crear(new CompraRequest(proveedor, List.of(new DetalleCompraRequest(a, n("2.500"), n("12.3456")),
                        new DetalleCompraRequest(b, n("3.125"), n("4.2000")))))
                .id();
        em.find(Compra.class, compra).setFecha(LocalDateTime.of(2025, 2, 3, 23, 59, 59, 999999000));
        var fuera = compras.crear(new CompraRequest(proveedor, List.of(new DetalleCompraRequest(a, n("1"), n("5")))));
        em.find(Compra.class, fuera.id()).setFecha(LocalDateTime.of(2025, 2, 4, 0, 0));
        em.flush();
    }

    @Test
    void resumenComprasIncluyeDiaCompletoYCalculaPromedioExacto() {
        prepararCompras();
        var r = reportes.resumen(dia());
        assertThat(r.cantidadCompras()).isEqualTo(1);
        assertThat(r.totalComprado()).isEqualByComparingTo("43.99");
        assertThat(r.compraPromedio()).isEqualByComparingTo("43.99");
        var f = dia();
        f.setFechaHasta(LocalDate.of(2025, 2, 4));
        r = reportes.resumen(f);
        assertThat(r.cantidadCompras()).isEqualTo(2);
        assertThat(r.totalComprado()).isEqualByComparingTo("48.99");
        assertThat(r.compraPromedio()).isEqualByComparingTo("24.50");
    }

    @Test
    void proveedorAgrupaSinRucYUsaRazonSocialVigente() {
        prepararCompras();
        var filas = reportes.porProveedor(dia());
        assertThat(filas).singleElement().satisfies(r -> {
            assertThat(r.proveedorId()).isEqualTo(proveedor);
            assertThat(r.razonSocial()).isEqualTo("Proveedor reportes");
            assertThat(r.cantidadCompras()).isEqualTo(1);
            assertThat(r.totalComprado()).isEqualByComparingTo("43.99");
        });
    }

    @Test
    void insumosCompradosConservanFotografiaYSeparanUnidades() {
        prepararCompras();
        insumos.actualizar(a, new InsumoRequest("Aceite vegetal", null, UnidadMedida.L, true));
        var filas = reportes.insumosComprados(dia());
        assertThat(filas).hasSize(2);
        assertThat(filas).anySatisfy(r -> {
            assertThat(r.insumoId()).isEqualTo(a);
            assertThat(r.nombreInsumo()).isEqualTo("Aceite");
            assertThat(r.unidadMedida()).isEqualTo(UnidadMedida.L);
            assertThat(r.cantidadComprada()).isEqualByComparingTo("2.500");
            assertThat(r.importeComprado()).isEqualByComparingTo("30.86");
        });
        assertThat(filas).anySatisfy(r -> {
            assertThat(r.insumoId()).isEqualTo(b);
            assertThat(r.unidadMedida()).isEqualTo(UnidadMedida.KG);
            assertThat(r.cantidadComprada()).isEqualByComparingTo("3.125");
        });
    }

    @Test
    void movimientosCuentaSinSumarKgConLitrosYStockActualIndependienteDelRango() {
        prepararCompras();
        inventario.salida(new SalidaRequest(a, n("0.5"), "Uso"));
        inventario.ajuste(new AjusteRequest(a, n("1.25"), "Conteo"));
        for (var m : em.createQuery("select m from MovimientoInventario m where m.insumo.id in :ids",
                MovimientoInventario.class).setParameter("ids", List.of(a, b)).getResultList())
            m.setFecha(LocalDateTime.of(2025, 2, 3, 12, 0));
        em.flush();
        var filas = reportes.movimientos(dia());
        assertThat(filas).hasSize(3);
        assertThat(filas).anySatisfy(r -> {
            assertThat(r.tipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
            assertThat(r.cantidadMovimientos()).isEqualTo(3);
        });
        assertThat(filas).filteredOn(r -> r.tipoMovimiento() != TipoMovimiento.ENTRADA)
                .allSatisfy(r -> assertThat(r.cantidadMovimientos()).isEqualTo(1));
        assertThat(reportes.stockActual()).anySatisfy(r -> {
            assertThat(r.id()).isEqualTo(a);
            assertThat(r.stockActual()).isEqualByComparingTo("1.250");
            assertThat(r.unidadMedida()).isEqualTo(UnidadMedida.L);
        });
    }

    @Test
    void sinComprasDevuelveCerosSinNull() {
        var f = dia();
        f.setFechaDesde(LocalDate.of(1000, 1, 1));
        f.setFechaHasta(LocalDate.of(1000, 1, 1));
        var r = reportes.resumen(f);
        assertThat(r.cantidadCompras()).isZero();
        assertThat(r.totalComprado()).isEqualByComparingTo("0");
        assertThat(r.compraPromedio()).isEqualByComparingTo("0");
        assertThat(reportes.porProveedor(f)).isEmpty();
        assertThat(reportes.insumosComprados(f)).isEmpty();
        assertThat(reportes.movimientos(f)).allSatisfy(x -> assertThat(x.cantidadMovimientos()).isZero());
    }

    @Test
    void deliveryAgrupaCuatroEstadosYFechaAsignacionVigente() {
        var rep = new Repartidor();
        rep.setKeycloakSub("f15-" + UUID.randomUUID());
        rep.setNombreVisible("Prueba reportes");
        em.persist(rep);
        var cr = new pe.edu.upeu.polleria.clientes.cliente.dto.ClienteRequest();
        cr.setNombres("Reporte");
        cr.setApellidos("Delivery");
        cr.setTelefono("999000015");
        var cliente = clientes.crear(cr).getId();
        for (var estado : EstadoEntrega.values()) {
            var p = new pe.edu.upeu.polleria.pedidos.pedido.entity.Pedido();
            p.setClienteId(cliente);
            p.setFecha(LocalDateTime.of(2025, 2, 3, 12, 0));
            p.setTipoPedido(pe.edu.upeu.polleria.pedidos.pedido.entity.TipoPedido.DELIVERY);
            p.setEstado(pe.edu.upeu.polleria.pedidos.pedido.entity.EstadoPedido.LISTO);
            p.setMetodoPago(pe.edu.upeu.polleria.pedidos.pedido.entity.MetodoPago.EFECTIVO);
            p.setEstadoPago(pe.edu.upeu.polleria.pedidos.pedido.entity.EstadoPago.PENDIENTE);
            p.setDireccionEntrega("Prueba");
            p.setTotal(n("20"));
            em.persist(p);
            var e = new Entrega();
            e.setPedidoId(p.getId());
            e.setRepartidor(rep);
            e.setEstado(estado);
            e.setFechaAsignacion(LocalDateTime.of(2025, 2, 3, 23, 59, 59));
            if (estado != EstadoEntrega.ASIGNADO)
                e.setFechaSalida(LocalDateTime.of(2025, 2, 4, 0, 0));
            if (estado == EstadoEntrega.ENTREGADO)
                e.setFechaEntrega(LocalDateTime.of(2025, 2, 4, 0, 1));
            if (estado == EstadoEntrega.NO_ENTREGADO)
                e.setObservacion("Ausente");
            em.persist(e);
        }
        em.flush();
        var f = new pe.edu.upeu.polleria.delivery.entrega.dto.ReporteFiltro();
        f.setFechaDesde(LocalDate.of(2025, 2, 3));
        f.setFechaHasta(LocalDate.of(2025, 2, 3));
        assertThat(delivery.porEstado(f)).hasSize(4).allSatisfy(x -> assertThat(x.cantidadEntregas()).isEqualTo(1));
        assertThat(delivery.resumen().entregasAbiertas()).hasSize(3)
                .noneMatch(x -> x.estado() == EstadoEntrega.ENTREGADO);
        assertThat(delivery.resumen().repartidoresActivos()).isPositive();
    }

    static final List<String> URLS = List.of("pedidos/reportes/resumen", "pedidos/reportes/ventas-por-dia",
            "pedidos/reportes/metodos-pago", "pedidos/reportes/clientes", "pedidos/reportes/productos-mas-vendidos",
            "pedidos/reportes/por-estado", "pedidos/reportes/por-tipo", "delivery/reportes/por-estado",
            "delivery/reportes/resumen", "abastecimiento/reportes/resumen-compras",
            "abastecimiento/reportes/compras-por-proveedor", "abastecimiento/reportes/insumos-comprados",
            "abastecimiento/reportes/movimientos", "abastecimiento/reportes/stock-actual");

    @Test
    void administradorAccedeTodosLosReportes() throws Exception {
        for (var p : URLS)
            mvc.perform(get("/api/v1/" + p).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                    .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = { "CAJERO", "ABASTECIMIENTO", "REPARTIDOR", "CLIENTE" })
    void otrosRolesNoAcceden(String rol) throws Exception {
        for (var p : URLS)
            mvc.perform(get("/api/v1/" + p).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol))))
                    .andExpect(status().isForbidden());
    }

    @Test
    void anonimoNoAccede() throws Exception {
        for (var p : URLS)
            mvc.perform(get("/api/v1/" + p)).andExpect(status().isUnauthorized());
    }

    @Test
    void rangosInvalidosYTopInvalidosSon400() throws Exception {
        for (var p : List.of("pedidos/reportes/ventas-por-dia", "delivery/reportes/por-estado",
                "abastecimiento/reportes/resumen-compras")) {
            mvc.perform(get("/api/v1/" + p).param("fechaDesde", "2026-10-25").param("fechaHasta", "2026-10-24")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/api/v1/" + p).param("fechaHasta", "9999-12-31")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                    .andExpect(status().isBadRequest());
        }
        for (var p : List.of("clientes", "productos-mas-vendidos"))
            mvc.perform(get("/api/v1/pedidos/reportes/" + p).param("top", "101")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                    .andExpect(status().isBadRequest());
    }
}
