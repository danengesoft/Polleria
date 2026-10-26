package pe.edu.upeu.polleria.abastecimiento.proveedor.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.polleria.abastecimiento.proveedor.dto.*;
import pe.edu.upeu.polleria.abastecimiento.proveedor.service.ProveedorService;

@RestController
@RequestMapping("/api/v1/abastecimiento/proveedores")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Abastecimiento — Proveedores")
public class ProveedorController {
    private final ProveedorService service;

    @GetMapping
    public List<ProveedorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest r) {
        var response = service.crear(r);
        return ResponseEntity.created(URI.create("/api/v1/abastecimiento/proveedores/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest r) {
        return service.actualizar(id, r);
    }
}
