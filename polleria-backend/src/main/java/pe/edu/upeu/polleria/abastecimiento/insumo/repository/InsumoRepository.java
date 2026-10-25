package pe.edu.upeu.polleria.abastecimiento.insumo.repository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;
public interface InsumoRepository extends JpaRepository<Insumo,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Insumo i where i.id=:id")
 Optional<Insumo> bloquear(@Param("id") Long id);
 @Query("select new pe.edu.upeu.polleria.abastecimiento.insumo.dto.StockActualReporte(i.id,i.nombre,i.unidadMedida,i.stockActual,i.activo) from Insumo i order by i.nombre,i.id")
 java.util.List<pe.edu.upeu.polleria.abastecimiento.insumo.dto.StockActualReporte> stockActual();

}
