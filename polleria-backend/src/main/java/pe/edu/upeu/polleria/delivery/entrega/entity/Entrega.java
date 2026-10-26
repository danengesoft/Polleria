package pe.edu.upeu.polleria.delivery.entrega.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import pe.edu.upeu.polleria.delivery.repartidor.entity.Repartidor;

@Entity
@Table(name = "ENTREGAS", schema = "POLLERIA")
@Getter
@Setter
@NoArgsConstructor
public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "PEDIDO_ID", nullable = false, unique = true)
    private Long pedidoId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REPARTIDOR_ID", nullable = false)
    private Repartidor repartidor;
    @Column(name = "FECHA_ASIGNACION", nullable = false)
    private LocalDateTime fechaAsignacion;
    @Column(name = "FECHA_SALIDA")
    private LocalDateTime fechaSalida;
    @Column(name = "FECHA_ENTREGA")
    private LocalDateTime fechaEntrega;
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoEntrega estado;
    @Column(name = "OBSERVACION", length = 2000)
    private String observacion;
}
