package pe.edu.upeu.polleria.abastecimiento.insumo.service;

import java.util.List;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.*;

public interface InsumoService {
    List<InsumoResponse> listar();

    InsumoResponse obtener(Long id);

    InsumoResponse crear(InsumoRequest request);

    InsumoResponse actualizar(Long id, InsumoRequest request);
}
