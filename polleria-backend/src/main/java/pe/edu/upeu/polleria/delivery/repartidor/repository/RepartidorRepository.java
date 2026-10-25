package pe.edu.upeu.polleria.delivery.repartidor.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.polleria.delivery.repartidor.entity.Repartidor;
public interface RepartidorRepository extends JpaRepository<Repartidor,Long> {
    Optional<Repartidor> findByKeycloakSub(String sub);
    List<Repartidor> findByActivoTrueOrderByNombreVisibleAsc();
 long countByActivoTrue();
}
