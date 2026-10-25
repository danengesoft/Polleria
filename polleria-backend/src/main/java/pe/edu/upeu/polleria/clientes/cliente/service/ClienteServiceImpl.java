package pe.edu.upeu.polleria.clientes.cliente.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.clientes.cliente.dto.*;
import pe.edu.upeu.polleria.clientes.cliente.entity.Cliente;
import pe.edu.upeu.polleria.clientes.cliente.mapper.ClienteMapper;
import pe.edu.upeu.polleria.clientes.cliente.repository.ClienteRepository;
import pe.edu.upeu.polleria.exception.BusinessConflictException;
import pe.edu.upeu.polleria.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream().map(clienteMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return clienteMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String correo = normalizarCorreo(request.getCorreo());
        if (correo != null && clienteRepository.contarPorCorreo(correo) > 0) {
            throw new BusinessConflictException("Ya existe un cliente con ese correo");
        }
        Cliente cliente = clienteMapper.toEntity(request);
        normalizar(cliente, correo);
        return clienteMapper.toResponse(clienteRepository.saveAndFlush(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarOFallar(id);
        String correo = normalizarCorreo(request.getCorreo());
        if (correo != null && clienteRepository.contarPorCorreoExcluyendoId(correo, id) > 0) {
            throw new BusinessConflictException("Ya existe un cliente con ese correo");
        }
        clienteMapper.actualizar(request, cliente);
        normalizar(cliente, correo);
        return clienteMapper.toResponse(clienteRepository.saveAndFlush(cliente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        // Eliminación física en Fase 4; revisar cuando Pedido referencie Cliente.
        clienteRepository.delete(buscarOFallar(id));
        clienteRepository.flush();
    }

    private Cliente buscarOFallar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private String normalizarCorreo(String correo) {
        String valor = opcional(correo);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private void normalizar(Cliente cliente, String correo) {
        cliente.setNombres(cliente.getNombres().strip().replaceAll("\\s+", " "));
        cliente.setApellidos(cliente.getApellidos().strip().replaceAll("\\s+", " "));
        cliente.setCorreo(correo);
        cliente.setDireccionPrincipal(opcional(cliente.getDireccionPrincipal()));
        cliente.setReferencia(opcional(cliente.getReferencia()));
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
