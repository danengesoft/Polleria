package pe.edu.upeu.polleria.catalogo.categoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.polleria.catalogo.categoria.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
