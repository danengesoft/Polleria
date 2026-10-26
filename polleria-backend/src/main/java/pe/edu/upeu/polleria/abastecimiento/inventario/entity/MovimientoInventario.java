package pe.edu.upeu.polleria.abastecimiento.inventario.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;

@Entity
@Table(name = "MOVIMIENTOS_INVENTARIO", schema = "POLLERIA")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "INSUMO_ID", nullable = false)
    private Insumo insumo;
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 10)
    private TipoMovimiento tipo;
    @Column(name = "CANTIDAD", nullable = false, precision = 19, scale = 3)
    private BigDecimal cantidad;
    @Column(name = "STOCK_ANTERIOR", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockAnterior;
    @Column(name = "STOCK_POSTERIOR", nullable = false, precision = 19, scale = 3)
    private BigDecimal stockPosterior;
    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;
    @Column(name = "MOTIVO", nullable = false, length = 500)
    private String motivo;
    @Column(name = "COMPRA_ID")
    private Long compraId;
}
