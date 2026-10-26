package pe.edu.upeu.polleria.abastecimiento.proveedor.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.abastecimiento.proveedor.dto.*;
import pe.edu.upeu.polleria.abastecimiento.proveedor.entity.Proveedor;
import pe.edu.upeu.polleria.abastecimiento.proveedor.mapper.ProveedorMapper;
import pe.edu.upeu.polleria.abastecimiento.proveedor.repository.ProveedorRepository;
import pe.edu.upeu.polleria.exception.*;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {
    private final ProveedorRepository repository;
    private final ProveedorMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorRequest r) {
        if (r.ruc() != null && repository.existsByRuc(r.ruc()))
            throw new BusinessConflictException("Ya existe un proveedor con ese RUC");
        Proveedor p = mapper.toEntity(r);
        normalizar(p);
        return mapper.toResponse(repository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest r) {
        Proveedor p = buscar(id);
        if (r.ruc() != null && repository.existsByRucAndIdNot(r.ruc(), id))
            throw new BusinessConflictException("Ya existe un proveedor con ese RUC");
        mapper.actualizar(r, p);
        normalizar(p);
        return mapper.toResponse(repository.saveAndFlush(p));
    }

    private Proveedor buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
    }

    private String opcional(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private void normalizar(Proveedor p) {
        p.setRazonSocial(p.getRazonSocial().strip().replaceAll("\\s+", " "));
        p.setCorreo(p.getCorreo() == null ? null : p.getCorreo().toLowerCase(Locale.ROOT));
        p.setDireccion(opcional(p.getDireccion()));
    }
}
