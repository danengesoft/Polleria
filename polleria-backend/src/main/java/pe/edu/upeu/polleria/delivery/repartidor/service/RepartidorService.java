package pe.edu.upeu.polleria.delivery.repartidor.service;
import java.util.List;
import pe.edu.upeu.polleria.delivery.repartidor.dto.RepartidorResponse;
public interface RepartidorService {
    RepartidorResponse miPerfil(String sub);
    RepartidorResponse activar(String sub, String nombre);
    List<RepartidorResponse> listarActivos();
}
