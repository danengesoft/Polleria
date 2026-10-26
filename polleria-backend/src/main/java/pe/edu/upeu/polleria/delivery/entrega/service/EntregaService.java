package pe.edu.upeu.polleria.delivery.entrega.service;

import java.util.List;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;

public interface EntregaService {
    List<EntregaResponse> listar();

    EntregaResponse obtener(Long id);

    EntregaResponse asignar(AsignacionRequest request);

    EntregaResponse reasignar(Long id, ReasignacionRequest request);

    List<EntregaResponse> mias(String sub);

    EntregaResponse mia(String sub, Long id);

    EntregaResponse iniciar(String sub, Long id);

    EntregaResponse entregar(String sub, Long id);

    EntregaResponse noEntregado(String sub, Long id, IncidenciaRequest request);
}
