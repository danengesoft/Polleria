package pe.edu.upeu.polleria.delivery.entrega.repository;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.delivery.entrega.entity.Entrega;
public interface EntregaRepository extends JpaRepository<Entrega,Long> {
    boolean existsByPedidoId(Long pedidoId);
    @Override @EntityGraph(attributePaths="repartidor") List<Entrega> findAll();
    @Override @EntityGraph(attributePaths="repartidor") Optional<Entrega> findById(Long id);
    @EntityGraph(attributePaths="repartidor") List<Entrega> findByRepartidorKeycloakSubOrderByFechaAsignacionDesc(String sub);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from Entrega e where e.id=:id")
    Optional<Entrega> bloquear(@Param("id") Long id);
 @Query("select new pe.edu.upeu.polleria.delivery.entrega.dto.EntregaEstadoReporte(e.estado,count(e)) from Entrega e where (:desde is null or e.fechaAsignacion>=:desde) and (:hasta is null or e.fechaAsignacion<:hasta) group by e.estado")
 List<pe.edu.upeu.polleria.delivery.entrega.dto.EntregaEstadoReporte> porEstado(@Param("desde") java.time.LocalDateTime desde,@Param("hasta") java.time.LocalDateTime hasta);

}
