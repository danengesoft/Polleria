package pe.edu.upeu.polleria.pedidos.pedido.dto;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.edu.upeu.polleria.pedidos.pedido.entity.*;
public record MiPedidoRequest(
    @NotNull TipoPedido tipoPedido, @NotNull MetodoPago metodoPago,
    @Size(max=300) String direccionEntrega, @Size(max=300) String referenciaEntrega,
    @NotEmpty @Valid List<@NotNull DetallePedidoRequest> detalles) {
    public PedidoRequest toInterno(Long clienteId) {
        PedidoRequest r = new PedidoRequest(); r.setClienteId(clienteId);
        r.setTipoPedido(tipoPedido); r.setMetodoPago(metodoPago);
        r.setDireccionEntrega(direccionEntrega); r.setReferenciaEntrega(referenciaEntrega); r.setDetalles(detalles);
        return r;
    }
}
