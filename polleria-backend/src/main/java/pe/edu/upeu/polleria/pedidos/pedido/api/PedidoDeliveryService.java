package pe.edu.upeu.polleria.pedidos.pedido.api;
public interface PedidoDeliveryService {
    PedidoDeliveryView consultar(Long id);
    void validarAsignacion(Long id);
    void validarReasignacion(Long id);
    void despachar(Long id);
    void entregar(Long id);
}
