package pe.edu.upeu.polleria.delivery.repartidor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "REPARTIDORES", schema = "POLLERIA")
@Getter
@Setter
@NoArgsConstructor
public class Repartidor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "KEYCLOAK_SUB", nullable = false, unique = true, length = 255)
    private String keycloakSub;
    @Column(name = "NOMBRE_VISIBLE", nullable = false, length = 200)
    private String nombreVisible;
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    @Column(name = "ACTIVO", nullable = false, precision = 1)
    private Boolean activo = true;
}
