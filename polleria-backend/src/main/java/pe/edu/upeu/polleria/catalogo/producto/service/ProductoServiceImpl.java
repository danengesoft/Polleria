package pe.edu.upeu.polleria.catalogo.producto.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.polleria.catalogo.categoria.repository.CategoriaRepository;
import pe.edu.upeu.polleria.catalogo.producto.dto.*;
import pe.edu.upeu.polleria.catalogo.producto.entity.Producto;
import pe.edu.upeu.polleria.catalogo.producto.mapper.ProductoMapper;
import pe.edu.upeu.polleria.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.polleria.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream().map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorCategoria(Long categoriaId) {
        buscarCategoriaOFallar(categoriaId);
        return productoRepository.findByCategoriaId(categoriaId).stream().map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return productoMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = buscarCategoriaOFallar(request.getCategoriaId());
        Producto producto = productoMapper.toEntity(request, categoria);
        producto.setNombre(request.getNombre().strip());
        return productoMapper.toResponse(productoRepository.saveAndFlush(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarOFallar(id);
        Categoria categoria = buscarCategoriaOFallar(request.getCategoriaId());
        productoMapper.actualizar(request, categoria, producto);
        producto.setNombre(request.getNombre().strip());
        return productoMapper.toResponse(productoRepository.saveAndFlush(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        productoRepository.delete(buscarOFallar(id));
        productoRepository.flush();
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private Categoria buscarCategoriaOFallar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }
}
