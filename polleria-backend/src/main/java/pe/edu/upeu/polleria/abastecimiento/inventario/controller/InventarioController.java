package pe.edu.upeu.polleria.abastecimiento.inventario.controller;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.service.InventarioService;
@RestController @RequestMapping("/api/v1/abastecimiento/movimientos") @RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name="Abastecimiento — Inventario")
public class InventarioController {
 private final InventarioService service;
 @GetMapping public List<MovimientoResponse> buscar(@Valid @ModelAttribute @ParameterObject MovimientoFiltro f){return service.buscar(f);}
 @PostMapping("/salidas") public ResponseEntity<MovimientoResponse> salida(@Valid @RequestBody SalidaRequest r){return ResponseEntity.status(201).body(service.salida(r));}
 @PostMapping("/ajustes") public ResponseEntity<MovimientoResponse> ajuste(@Valid @RequestBody AjusteRequest r){return ResponseEntity.status(201).body(service.ajuste(r));}
}
