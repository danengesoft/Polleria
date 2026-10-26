package pe.edu.upeu.polleria.delivery.entrega.dto;

import java.time.LocalDateTime;
import pe.edu.upeu.polleria.delivery.entrega.entity.EstadoEntrega;
import pe.edu.upeu.polleria.delivery.repartidor.dto.RepartidorResponse;
import pe.edu.upeu.polleria.pedidos.pedido.api.PedidoDeliveryView;

public record EntregaResponse(Long id, Long pedidoId, RepartidorResponse repartidor,
        LocalDateTime fechaAsignacion, LocalDateTime fechaSalida, LocalDateTime fechaEntrega,
        EstadoEntrega estado, String observacion, PedidoDeliveryView pedido) {
}
