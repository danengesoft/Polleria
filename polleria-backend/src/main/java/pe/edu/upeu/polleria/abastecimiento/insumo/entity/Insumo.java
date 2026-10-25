package pe.edu.upeu.polleria.abastecimiento.insumo.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="INSUMOS",schema="POLLERIA") @Getter @Setter @NoArgsConstructor
public class Insumo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="ID") private Long id;
    @Column(name="NOMBRE",nullable=false,length=100) private String nombre;
    @Column(name="DESCRIPCION",length=500) private String descripcion;
    @Enumerated(EnumType.STRING) @Column(name="UNIDAD_MEDIDA",nullable=false,length=10) private UnidadMedida unidadMedida;
    @Column(name="STOCK_ACTUAL",nullable=false,precision=19,scale=3) private java.math.BigDecimal stockActual=java.math.BigDecimal.ZERO.setScale(3);
    @Convert(converter=org.hibernate.type.NumericBooleanConverter.class)
    @Column(name="ACTIVO",nullable=false,precision=1) private Boolean activo=true;
}
