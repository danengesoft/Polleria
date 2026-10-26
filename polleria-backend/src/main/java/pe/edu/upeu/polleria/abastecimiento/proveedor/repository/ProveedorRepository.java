package pe.edu.upeu.polleria.abastecimiento.proveedor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.polleria.abastecimiento.proveedor.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    boolean existsByRucAndIdNot(String ruc, Long id);

    boolean existsByRuc(String ruc);
}
