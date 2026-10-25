package pe.edu.upeu.polleria.abastecimiento.proveedor.service;
import java.util.List;
import pe.edu.upeu.polleria.abastecimiento.proveedor.dto.*;
public interface ProveedorService {
 List<ProveedorResponse> listar();
 ProveedorResponse obtener(Long id);
 ProveedorResponse crear(ProveedorRequest request);
 ProveedorResponse actualizar(Long id,ProveedorRequest request);
}
