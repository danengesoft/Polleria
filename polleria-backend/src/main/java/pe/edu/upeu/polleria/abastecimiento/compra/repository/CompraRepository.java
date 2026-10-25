package pe.edu.upeu.polleria.abastecimiento.compra.repository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.abastecimiento.compra.entity.Compra;
public interface CompraRepository extends JpaRepository<Compra,Long> {
 @Override @EntityGraph(attributePaths={"detalles","proveedor"}) Optional<Compra> findById(Long id);
 @EntityGraph(attributePaths={"detalles","proveedor"})
 @Query("select c from Compra c where (:proveedor is null or c.proveedor.id=:proveedor) and (:desde is null or c.fecha>=:desde) and (:hasta is null or c.fecha<:hasta) order by c.fecha desc,c.id desc")
 List<Compra> buscar(@Param("proveedor") Long proveedor,@Param("desde") LocalDateTime desde,@Param("hasta") LocalDateTime hasta);
}
