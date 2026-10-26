package pe.edu.upeu.polleria.abastecimiento.compra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;
import pe.edu.upeu.polleria.abastecimiento.proveedor.entity.Proveedor;

@Entity
@Table(name = "COMPRAS", schema = "POLLERIA")
@Getter
@Setter
@NoArgsConstructor
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROVEEDOR_ID", nullable = false)
    private Proveedor proveedor;
    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;
    @Column(name = "TOTAL", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;
    @OneToMany(mappedBy = "compra", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<DetalleCompra> detalles = new ArrayList<>();
}
