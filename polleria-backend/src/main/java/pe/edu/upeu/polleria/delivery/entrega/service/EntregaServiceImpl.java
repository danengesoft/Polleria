package pe.edu.upeu.polleria.delivery.entrega.service;

import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.delivery.entrega.dto.*;
import pe.edu.upeu.polleria.delivery.entrega.entity.*;
import pe.edu.upeu.polleria.delivery.entrega.repository.EntregaRepository;
import pe.edu.upeu.polleria.delivery.repartidor.entity.Repartidor;
import pe.edu.upeu.polleria.delivery.repartidor.dto.RepartidorResponse;
import pe.edu.upeu.polleria.delivery.repartidor.repository.RepartidorRepository;
import pe.edu.upeu.polleria.pedidos.pedido.api.PedidoDeliveryService;
import pe.edu.upeu.polleria.exception.*;

@Service
@RequiredArgsConstructor
public class EntregaServiceImpl implements EntregaService {
    private final EntregaRepository repository;
    private final RepartidorRepository repartidores;
    private final PedidoDeliveryService pedidos;

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> listar() {
        return repository.findAll().stream().map(this::respuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntregaResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    @Override
    @Transactional
    public EntregaResponse asignar(AsignacionRequest r) {
        pedidos.validarAsignacion(r.pedidoId()); // Locks Pedido through its owning module.
        if (repository.existsByPedidoId(r.pedidoId()))
            throw new BusinessConflictException("El pedido ya tiene una entrega; utiliza reasignar si corresponde");
        Entrega e = new Entrega();
        e.setPedidoId(r.pedidoId());
        e.setRepartidor(activo(r.repartidorId()));
        e.setEstado(EstadoEntrega.ASIGNADO);
        e.setFechaAsignacion(ahora());
        return respuesta(repository.saveAndFlush(e));
    }

    @Override
    @Transactional
    public EntregaResponse reasignar(Long id, ReasignacionRequest r) {
        Entrega e = bloquear(id);
        exigir(e, EstadoEntrega.NO_ENTREGADO);
        pedidos.validarReasignacion(e.getPedidoId());
        e.setRepartidor(activo(r.repartidorId()));
        e.setEstado(EstadoEntrega.ASIGNADO);
        e.setFechaAsignacion(ahora());
        e.setFechaSalida(null);
        e.setFechaEntrega(null); // Preserve previous observations.
        return respuesta(repository.saveAndFlush(e));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> mias(String sub) {
        perfilActivo(sub);
        return repository.findByRepartidorKeycloakSubOrderByFechaAsignacionDesc(sub).stream().map(this::respuesta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntregaResponse mia(String sub, Long id) {
        Entrega e = buscar(id);
        propietario(e, sub);
        return respuesta(e);
    }

    @Override
    @Transactional
    public EntregaResponse iniciar(String sub, Long id) {
        Entrega e = bloquear(id);
        propietario(e, sub);
        exigir(e, EstadoEntrega.ASIGNADO);
        e.setEstado(EstadoEntrega.EN_CAMINO);
        e.setFechaSalida(ahora());
        repository.saveAndFlush(e);
        pedidos.despachar(e.getPedidoId()); // Same transaction; failure rolls back the flushed Entrega.
        return respuesta(e);
    }

    @Override
    @Transactional
    public EntregaResponse entregar(String sub, Long id) {
        Entrega e = bloquear(id);
        propietario(e, sub);
        exigir(e, EstadoEntrega.EN_CAMINO);
        e.setEstado(EstadoEntrega.ENTREGADO);
        e.setFechaEntrega(ahora());
        repository.saveAndFlush(e);
        pedidos.entregar(e.getPedidoId());
        return respuesta(e);
    }

    @Override
    @Transactional
    public EntregaResponse noEntregado(String sub, Long id, IncidenciaRequest r) {
        Entrega e = bloquear(id);
        propietario(e, sub);
        exigir(e, EstadoEntrega.EN_CAMINO);
        if (r.observacion() == null || r.observacion().isBlank() || r.observacion().length() > 500)
            throw new InvalidRequestException("Escribe una observación de hasta 500 caracteres");
        String anterior = e.getObservacion();
        String nueva = "[" + ahora() + "] " + r.observacion().strip();
        if (anterior != null)
            nueva = anterior + "\n" + nueva;
        if (nueva.length() > 2000)
            throw new BusinessConflictException("Se alcanzó el límite de observaciones; requiere revisión interna");
        e.setObservacion(nueva);
        e.setEstado(EstadoEntrega.NO_ENTREGADO);
        return respuesta(repository.saveAndFlush(e)); // Pedido remains DESPACHADO, payment unchanged.
    }

    private Repartidor activo(Long id) {
        Repartidor r = repartidores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));
        if (!Boolean.TRUE.equals(r.getActivo()))
            throw new BusinessConflictException("Repartidor inactivo");
        return r;
    }

    private void perfilActivo(String sub) {
        Repartidor r = repartidores.findByKeycloakSub(sub)
                .orElseThrow(() -> new ResourceNotFoundException("Activa tu perfil de reparto"));
        if (!Boolean.TRUE.equals(r.getActivo()))
            throw new BusinessConflictException("Perfil de reparto inactivo");
    }

    private void propietario(Entrega e, String sub) {
        if (!e.getRepartidor().getKeycloakSub().equals(sub))
            throw new ResourceNotFoundException("Entrega no encontrada");
        perfilActivo(sub);
    }

    private Entrega buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entrega no encontrada"));
    }

    private Entrega bloquear(Long id) {
        return repository.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Entrega no encontrada"));
    }

    private void exigir(Entrega e, EstadoEntrega estado) {
        if (e.getEstado() != estado)
            throw new BusinessConflictException("La entrega no permite esta operación desde " + e.getEstado());
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZoneId.of("America/Lima"));
    }

    private EntregaResponse respuesta(Entrega e) {
        Repartidor r = e.getRepartidor();
        return new EntregaResponse(e.getId(), e.getPedidoId(),
                new RepartidorResponse(r.getId(), r.getNombreVisible(), r.getActivo()),
                e.getFechaAsignacion(), e.getFechaSalida(), e.getFechaEntrega(), e.getEstado(), e.getObservacion(),
                pedidos.consultar(e.getPedidoId()));
    }
}
