package pe.edu.upeu.polleria.delivery.repartidor.service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.delivery.repartidor.dto.RepartidorResponse;
import pe.edu.upeu.polleria.delivery.repartidor.entity.Repartidor;
import pe.edu.upeu.polleria.delivery.repartidor.repository.RepartidorRepository;
import pe.edu.upeu.polleria.exception.*;
@Service @RequiredArgsConstructor
public class RepartidorServiceImpl implements RepartidorService {
    private final RepartidorRepository repository;
    @Override @Transactional(readOnly=true)
    public RepartidorResponse miPerfil(String sub) { return respuesta(repository.findByKeycloakSub(sub).orElseThrow(()->new ResourceNotFoundException("Activa tu perfil de reparto"))); }
    @Override @Transactional
    public RepartidorResponse activar(String sub, String nombre) {
        if (sub==null || sub.isBlank() || sub.length()>255) throw new InvalidRequestException("Identidad inválida");
        if (repository.findByKeycloakSub(sub).isPresent()) throw new BusinessConflictException("El perfil de reparto ya existe");
        Repartidor r=new Repartidor(); r.setKeycloakSub(sub);
        String visible=nombre==null||nombre.isBlank()?"Repartidor":nombre.strip();
        r.setNombreVisible(visible.substring(0,Math.min(visible.length(),200)));
        return respuesta(repository.saveAndFlush(r));
    }
    @Override @Transactional(readOnly=true)
    public List<RepartidorResponse> listarActivos() { return repository.findByActivoTrueOrderByNombreVisibleAsc().stream().map(this::respuesta).toList(); }
    private RepartidorResponse respuesta(Repartidor r) { return new RepartidorResponse(r.getId(),r.getNombreVisible(),r.getActivo()); }
}
