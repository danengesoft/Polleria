package pe.edu.upeu.polleria.abastecimiento.insumo.repository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;
public interface InsumoRepository extends JpaRepository<Insumo,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Insumo i where i.id=:id")
 Optional<Insumo> bloquear(@Param("id") Long id);
}
