package pe.edu.upeu.polleria.pedidos.pedido.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.pedidos.pedido.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Override @EntityGraph(attributePaths = "detalles")
    List<Pedido> findAll();
    @Override @EntityGraph(attributePaths = "detalles")
    Optional<Pedido> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> buscarParaCambioEstado(@Param("id") Long id);
}
