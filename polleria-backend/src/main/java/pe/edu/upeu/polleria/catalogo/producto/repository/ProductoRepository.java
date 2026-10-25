package pe.edu.upeu.polleria.catalogo.producto.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.polleria.catalogo.producto.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Override
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll();
    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByCategoriaId(Long categoriaId);
}
