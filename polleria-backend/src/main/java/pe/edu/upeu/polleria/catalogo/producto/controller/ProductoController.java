package pe.edu.upeu.polleria.catalogo.producto.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.edu.upeu.polleria.catalogo.producto.dto.*;
import pe.edu.upeu.polleria.catalogo.producto.service.ProductoService;

@Tag(name = "Productos")
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService service;

    @Operation(summary = "Listar productos")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(@RequestParam(required = false) Long categoriaId) {
        return ResponseEntity.ok(categoriaId == null ? service.listar() : service.listarPorCategoria(categoriaId));
    }

    @Operation(summary = "Obtener producto por id")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Crear producto")
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + response.getId())).body(response);
    }

    @Operation(summary = "Actualizar producto completa")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @Operation(summary = "Eliminar producto")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
