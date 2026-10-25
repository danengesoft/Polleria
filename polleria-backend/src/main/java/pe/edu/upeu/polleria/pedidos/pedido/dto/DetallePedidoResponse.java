package pe.edu.upeu.polleria.pedidos.pedido.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DetallePedidoResponse {
    private Long productoId;
    private String nombreProducto;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
}
