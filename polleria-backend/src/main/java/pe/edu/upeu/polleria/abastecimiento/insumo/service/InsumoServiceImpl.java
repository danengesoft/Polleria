package pe.edu.upeu.polleria.abastecimiento.insumo.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;
import pe.edu.upeu.polleria.abastecimiento.insumo.mapper.InsumoMapper;
import pe.edu.upeu.polleria.abastecimiento.insumo.repository.InsumoRepository;
import pe.edu.upeu.polleria.abastecimiento.inventario.repository.MovimientoInventarioRepository;
import pe.edu.upeu.polleria.exception.*;

@Service
@RequiredArgsConstructor
public class InsumoServiceImpl implements InsumoService {
    private final InsumoRepository repository;
    private final InsumoMapper mapper;
    private final MovimientoInventarioRepository movimientos;

    @Override
    @Transactional(readOnly = true)
    public List<InsumoResponse> listar() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InsumoResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public InsumoResponse crear(InsumoRequest r) {
        Insumo i = mapper.toEntity(r);
        normalizar(i);
        return mapper.toResponse(repository.saveAndFlush(i));
    }

    @Override
    @Transactional
    public InsumoResponse actualizar(Long id, InsumoRequest r) {
        Insumo i = repository.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado"));
        if (i.getUnidadMedida() != r.unidadMedida()
                && (i.getStockActual().signum() != 0 || movimientos.existsByInsumoId(id)))
            throw new BusinessConflictException(
                    "No se puede cambiar la unidad de un insumo con inventario o historial");
        mapper.actualizar(r, i);
        normalizar(i);
        return mapper.toResponse(repository.saveAndFlush(i));
    }

    private Insumo buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado"));
    }

    private void normalizar(Insumo i) {
        i.setNombre(i.getNombre().strip().replaceAll("\\s+", " "));
        i.setDescripcion(
                i.getDescripcion() == null || i.getDescripcion().isBlank() ? null : i.getDescripcion().strip());
    }
}
