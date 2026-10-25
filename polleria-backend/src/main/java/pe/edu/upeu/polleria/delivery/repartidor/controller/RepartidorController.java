package pe.edu.upeu.polleria.delivery.repartidor.controller;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upeu.polleria.delivery.repartidor.dto.RepartidorResponse;
import pe.edu.upeu.polleria.delivery.repartidor.service.RepartidorService;
@RestController @RequestMapping("/api/v1/delivery/repartidores") @RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name="Repartidores")
public class RepartidorController {
    private final RepartidorService service;
    @GetMapping public List<RepartidorResponse> listar() { return service.listarActivos(); }
    @GetMapping("/me") public RepartidorResponse perfil(@AuthenticationPrincipal Jwt jwt) { return service.miPerfil(jwt.getSubject()); }
    @PostMapping("/me") public ResponseEntity<RepartidorResponse> activar(@AuthenticationPrincipal Jwt jwt) {
        String nombre=jwt.getClaimAsString("name");
        if(nombre==null || nombre.isBlank()) nombre=jwt.getClaimAsString("preferred_username");
        return ResponseEntity.created(URI.create("/api/v1/delivery/repartidores/me")).body(service.activar(jwt.getSubject(),nombre));
    }
}
