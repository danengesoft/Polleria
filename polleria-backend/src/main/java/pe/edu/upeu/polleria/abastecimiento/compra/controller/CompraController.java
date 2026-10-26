package pe.edu.upeu.polleria.abastecimiento.compra.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
import pe.edu.upeu.polleria.abastecimiento.compra.service.CompraService;

@RestController
@RequestMapping("/api/v1/abastecimiento/compras")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Abastecimiento — Compras")
public class CompraController {
    private final CompraService service;

    @GetMapping
    public List<CompraResponse> buscar(@Valid @ModelAttribute @ParameterObject CompraFiltro f) {
        return service.buscar(f);
    }

    @GetMapping("/{id}")
    public CompraResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CompraResponse> crear(@Valid @RequestBody CompraRequest r) {
        var c = service.crear(r);
        return ResponseEntity.created(URI.create("/api/v1/abastecimiento/compras/" + c.id())).body(c);
    }
}
