package pe.edu.upeu.polleria.pedidos.pedido.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upeu.polleria.pedidos.pedido.entity.EstadoPedido;
import pe.edu.upeu.polleria.pedidos.pedido.entity.TipoPedido;

@Getter @Setter
public class PedidoFiltro {
    private EstadoPedido estado;
    private TipoPedido tipoPedido;
    @Positive
    private Long clienteId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Primer día de creación incluido, hora de Perú", example = "2026-10-01")
    private LocalDate fechaDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Último día de creación incluido, hora de Perú", example = "2026-10-31")
    private LocalDate fechaHasta;
}
