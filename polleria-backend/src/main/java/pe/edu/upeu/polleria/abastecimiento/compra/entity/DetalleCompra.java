package pe.edu.upeu.polleria.abastecimiento.compra.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;
@Entity @Table(name="DETALLE_COMPRAS",schema="POLLERIA") @Getter @Setter @NoArgsConstructor
public class DetalleCompra {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="ID") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="COMPRA_ID",nullable=false) private Compra compra;
 @Column(name="INSUMO_ID",nullable=false) private Long insumoId;
 @Column(name="NOMBRE_INSUMO",nullable=false,length=100) private String nombreInsumo;
 @Enumerated(EnumType.STRING) @Column(name="UNIDAD_MEDIDA",nullable=false,length=10) private UnidadMedida unidadMedida;
 @Column(name="CANTIDAD",nullable=false,precision=19,scale=3) private BigDecimal cantidad;
 @Column(name="COSTO_UNITARIO",nullable=false,precision=19,scale=4) private BigDecimal costoUnitario;
 @Column(name="SUBTOTAL",nullable=false,precision=19,scale=2) private BigDecimal subtotal;
}
