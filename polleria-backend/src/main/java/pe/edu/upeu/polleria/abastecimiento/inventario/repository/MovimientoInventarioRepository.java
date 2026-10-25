package pe.edu.upeu.polleria.abastecimiento.inventario.repository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.*;
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario,Long> {
 boolean existsByInsumoId(Long id);
 @EntityGraph(attributePaths="insumo")
 @Query("select m from MovimientoInventario m where (:insumo is null or m.insumo.id=:insumo) and (:tipo is null or m.tipo=:tipo) and (:desde is null or m.fecha>=:desde) and (:hasta is null or m.fecha<:hasta) order by m.fecha desc,m.id desc")
 List<MovimientoInventario> buscar(@Param("insumo") Long insumo,@Param("tipo") TipoMovimiento tipo,@Param("desde") LocalDateTime desde,@Param("hasta") LocalDateTime hasta);
}
