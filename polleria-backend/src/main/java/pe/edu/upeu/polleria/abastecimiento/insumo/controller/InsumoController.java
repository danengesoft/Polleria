package pe.edu.upeu.polleria.abastecimiento.insumo.controller;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.service.InsumoService;
@RestController @RequestMapping("/api/v1/abastecimiento/insumos") @RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name="Abastecimiento — Insumos")
public class InsumoController {
 private final InsumoService service;
 @GetMapping public List<InsumoResponse> listar() {return service.listar();}
 @GetMapping("/{id}") public InsumoResponse obtener(@PathVariable Long id) {return service.obtener(id);}
 @PostMapping public ResponseEntity<InsumoResponse> crear(@Valid @RequestBody InsumoRequest r) {
  var response=service.crear(r);return ResponseEntity.created(URI.create("/api/v1/abastecimiento/insumos/"+response.id())).body(response);
 }
 @PutMapping("/{id}") public InsumoResponse actualizar(@PathVariable Long id,@Valid @RequestBody InsumoRequest r) {return service.actualizar(id,r);}
}
