package pe.edu.upeu.polleria.clientes.cliente.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.clientes.cliente.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    java.util.Optional<Cliente> findByKeycloakSub(String sub);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE LOWER(TRIM(c.correo)) = :correo")
    long contarPorCorreo(@Param("correo") String correo);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE LOWER(TRIM(c.correo)) = :correo AND c.id <> :id")
    long contarPorCorreoExcluyendoId(@Param("correo") String correo, @Param("id") Long id);
}
