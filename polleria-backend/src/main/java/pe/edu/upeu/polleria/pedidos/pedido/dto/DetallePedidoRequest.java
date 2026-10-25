package pe.edu.upeu.polleria.pedidos.pedido.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DetallePedidoRequest {
    @NotNull @Positive
    private Long productoId;
    @NotNull @Positive
    private Integer cantidad;
}
