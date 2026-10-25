package pe.edu.upeu.polleria.pedidos.pedido.service;

import java.util.List;
import pe.edu.upeu.polleria.pedidos.pedido.dto.*;

public interface PedidoService {
    List<PedidoResponse> listar();
    PedidoResponse obtener(Long id);
    PedidoResponse crear(PedidoRequest request);
    PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request);
}
