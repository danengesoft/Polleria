package pe.edu.upeu.polleria.delivery.entrega.controller;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;
import pe.edu.upeu.polleria.delivery.entrega.service.EntregaService;
@RestController @RequestMapping("/api/v1/delivery/entregas") @RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name="Delivery")
public class EntregaController {
    private final EntregaService service;
    @GetMapping public List<EntregaResponse> listar(){return service.listar();}
    @GetMapping("/{id}") public EntregaResponse obtener(@PathVariable Long id){return service.obtener(id);}
    @PostMapping public ResponseEntity<EntregaResponse> asignar(@Valid @RequestBody AsignacionRequest r){
        EntregaResponse e=service.asignar(r);return ResponseEntity.created(URI.create("/api/v1/delivery/entregas/"+e.id())).body(e);
    }
    @PatchMapping("/{id}/reasignar") public EntregaResponse reasignar(@PathVariable Long id,@Valid @RequestBody ReasignacionRequest r){return service.reasignar(id,r);}
    @GetMapping("/mias") public List<EntregaResponse> mias(@AuthenticationPrincipal Jwt jwt){return service.mias(jwt.getSubject());}
    @GetMapping("/mias/{id}") public EntregaResponse mia(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return service.mia(jwt.getSubject(),id);}
    @PatchMapping("/mias/{id}/iniciar") public EntregaResponse iniciar(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return service.iniciar(jwt.getSubject(),id);}
    @PatchMapping("/mias/{id}/entregar") public EntregaResponse entregar(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return service.entregar(jwt.getSubject(),id);}
    @PatchMapping("/mias/{id}/no-entregado") public EntregaResponse noEntregado(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody IncidenciaRequest r){return service.noEntregado(jwt.getSubject(),id,r);}
}
