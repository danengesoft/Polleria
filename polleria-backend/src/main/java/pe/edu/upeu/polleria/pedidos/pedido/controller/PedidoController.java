package pe.edu.upeu.polleria.pedidos.pedido.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.edu.upeu.polleria.pedidos.pedido.dto.*;
import pe.edu.upeu.polleria.pedidos.pedido.service.PedidoService;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Pedidos")
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService service;

    @Operation(summary = "Consultar pedidos con filtros opcionales y orden seguro")
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> buscar(
            @Valid @ModelAttribute @ParameterObject PedidoFiltro filtro,
            @RequestParam(defaultValue = "fecha") String ordenarPor,
            @RequestParam(defaultValue = "DESC") String direccion) {
        return ResponseEntity.ok(service.buscar(filtro, ordenarPor, direccion));
    }

    @Operation(summary = "Ventas efectivas: pedidos ENTREGADO y PAGADO, según fecha de creación")
    @GetMapping("/reportes/resumen")
    public ResponseEntity<PedidoResumenReporte> resumen(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro) {
        return ResponseEntity.ok(service.resumen(filtro));
    }

    @Operation(summary = "Productos vendidos de ventas efectivas; agrupados por id con nombre histórico representativo")
    @GetMapping("/reportes/productos-mas-vendidos")
    public ResponseEntity<List<ProductoVendidoReporte>> productosMasVendidos(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro,
            @io.swagger.v3.oas.annotations.Parameter(description="Máximo de filas, entre 1 y 100") @RequestParam(defaultValue="20") int top) {
        return ResponseEntity.ok(service.productosMasVendidos(filtro, top));
    }

    @Operation(summary = "Cantidad de pedidos por estado, incluidos cancelados; muestra ceros")
    @GetMapping("/reportes/por-estado")
    public ResponseEntity<List<EstadoPedidoReporte>> porEstado(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro) {
        return ResponseEntity.ok(service.porEstado(filtro));
    }

    @Operation(summary = "Cantidad de pedidos por tipo; muestra ceros")
    @GetMapping("/reportes/por-tipo")
    public ResponseEntity<List<TipoPedidoReporte>> porTipo(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro) {
        return ResponseEntity.ok(service.porTipo(filtro));
    }

    @Operation(summary = "Obtener pedido histórico")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable Long id) { return ResponseEntity.ok(service.obtener(id)); }

    @Operation(summary = "Crear pedido transaccional; precios y total calculados por el servidor")
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + response.getId())).body(response);
    }

    @Operation(summary = "Confirmar explícitamente pago pendiente YAPE/PLIN/TARJETA")
    @PatchMapping("/{id}/confirmar-pago")
    public ResponseEntity<PedidoResponse> confirmarPago(@PathVariable Long id) { return ResponseEntity.ok(service.confirmarPago(id)); }

    @Operation(summary = "Cambiar estado mediante transiciones permitidas; sin despacho en esta fase")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }
    @Operation(summary="Ventas efectivas por día de creación en Perú, orden ascendente") @GetMapping("/reportes/ventas-por-dia")
    public List<VentaDiaReporte> ventasPorDia(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro) {
        return service.ventasPorDia(filtro);
    }

    @Operation(summary="Ventas efectivas agrupadas por método de pago") @GetMapping("/reportes/metodos-pago")
    public List<MetodoPagoReporte> metodosPago(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro) {
        return service.metodosPago(filtro);
    }

    @Operation(summary="Clientes por compras efectivas; identificadores sin datos personales") @GetMapping("/reportes/clientes")
    public List<ClienteCompradorReporte> clientesCompradores(@Valid @ModelAttribute @ParameterObject PedidoFiltro filtro, @io.swagger.v3.oas.annotations.Parameter(description="Máximo de filas entre 1 y 100") @RequestParam(defaultValue="20") int top) {
        return service.clientesCompradores(filtro, top);
    }

}
