package pe.edu.upeu.polleria.abastecimiento.proveedor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PROVEEDORES", schema = "POLLERIA")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "RAZON_SOCIAL", nullable = false, length = 150)
    private String razonSocial;
    @Column(name = "RUC", unique = true, length = 11)
    private String ruc;
    @Column(name = "TELEFONO", length = 12)
    private String telefono;
    @Column(name = "CORREO", length = 254)
    private String correo;
    @Column(name = "DIRECCION", length = 300)
    private String direccion;
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    @Column(name = "ACTIVO", nullable = false, precision = 1)
    private Boolean activo = true;
}
