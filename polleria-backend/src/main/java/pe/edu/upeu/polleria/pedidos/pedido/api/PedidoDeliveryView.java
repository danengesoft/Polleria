package pe.edu.upeu.polleria.pedidos.pedido.api;

import java.math.BigDecimal;

public record PedidoDeliveryView(Long id, String estadoPedido, String nombreCliente,
        String telefono, String direccionEntrega, String referenciaEntrega,
        BigDecimal total, String metodoPago, String estadoPago) {
}
