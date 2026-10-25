package pe.edu.upeu.polleria.catalogo.categoria.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import pe.edu.upeu.polleria.catalogo.categoria.dto.*;
import pe.edu.upeu.polleria.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.polleria.catalogo.categoria.mapper.CategoriaMapper;
import pe.edu.upeu.polleria.catalogo.categoria.repository.CategoriaRepository;
import pe.edu.upeu.polleria.exception.*;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(categoriaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return categoriaMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = normalizarNombre(request.getNombre());
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessConflictException("Ya existe una categoría con ese nombre");
        }
        Categoria categoria = categoriaMapper.toEntity(request);
        categoria.setNombre(nombre);
        return categoriaMapper.toResponse(categoriaRepository.saveAndFlush(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarOFallar(id);
        String nombre = normalizarNombre(request.getNombre());
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessConflictException("Ya existe una categoría con ese nombre");
        }
        categoriaMapper.actualizar(request, categoria);
        categoria.setNombre(nombre);
        return categoriaMapper.toResponse(categoriaRepository.saveAndFlush(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarOFallar(id);
        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // La FK protege incluso ante una asociación concurrente, sin dependencia inversa de Producto.
            throw new BusinessConflictException("No se puede eliminar una categoría utilizada; puede desactivarla");
        }
    }

    private Categoria buscarOFallar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    private String normalizarNombre(String nombre) {
        return nombre.strip().replaceAll("\\s+", " ");
    }
}
