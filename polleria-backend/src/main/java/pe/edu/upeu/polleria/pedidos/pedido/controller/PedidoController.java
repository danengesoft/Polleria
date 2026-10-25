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

@Tag(name = "Pedidos")
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService service;

    @Operation(summary = "Listar pedidos con sus detalles")
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar() { return ResponseEntity.ok(service.listar()); }

    @Operation(summary = "Obtener pedido histórico")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable Long id) { return ResponseEntity.ok(service.obtener(id)); }

    @Operation(summary = "Crear pedido transaccional; precios y total calculados por el servidor")
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + response.getId())).body(response);
    }

    @Operation(summary = "Cambiar estado mediante transiciones permitidas; sin despacho en esta fase")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }
}
