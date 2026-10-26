package pe.edu.upeu.polleria.abastecimiento.inventario.service;

import java.math.*;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;
import pe.edu.upeu.polleria.abastecimiento.insumo.repository.InsumoRepository;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.mapper.MovimientoMapper;
import pe.edu.upeu.polleria.abastecimiento.inventario.repository.MovimientoInventarioRepository;
import pe.edu.upeu.polleria.exception.*;

@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {
    private final InsumoRepository insumos;
    private final MovimientoInventarioRepository repository;
    private final MovimientoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponse> buscar(MovimientoFiltro f) {
        validarFechas(f.getFechaDesde(), f.getFechaHasta());
        return repository.buscar(f.getInsumoId(), f.getTipo(), desde(f.getFechaDesde()), hasta(f.getFechaHasta()))
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public MovimientoResponse salida(SalidaRequest r) {
        Insumo i = bloquearActivo(r.insumoId());
        BigDecimal cantidad = cantidad(r.cantidad());
        if (cantidad.signum() <= 0)
            throw new InvalidRequestException("La salida requiere cantidad positiva");
        return registrar(i, TipoMovimiento.SALIDA, cantidad, i.getStockActual().subtract(cantidad), r.motivo(), null);
    }

    @Override
    @Transactional
    public MovimientoResponse ajuste(AjusteRequest r) {
        Insumo i = bloquearActivo(r.insumoId());
        BigDecimal nuevo = cantidad(r.nuevoStock());
        if (nuevo.signum() < 0)
            throw new InvalidRequestException("El conteo físico no puede ser negativo");
        BigDecimal diferencia = nuevo.subtract(i.getStockActual());
        if (diferencia.signum() == 0)
            throw new BusinessConflictException("El conteo coincide con el stock actual; no requiere ajuste");
        return registrar(i, TipoMovimiento.AJUSTE, diferencia, nuevo, r.motivo(), null);
    }

    // Internal only: Compra already holds all Insumo locks in ascending ID order.
    @Transactional
    public void entradaCompra(Insumo i, BigDecimal cantidad, Long compraId) {
        registrar(i, TipoMovimiento.ENTRADA, cantidad, i.getStockActual().add(cantidad), "Compra #" + compraId,
                compraId);
    }

    private Insumo bloquearActivo(Long id) {
        Insumo i = insumos.bloquear(id).orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado"));
        if (!Boolean.TRUE.equals(i.getActivo()))
            throw new BusinessConflictException("Insumo inactivo");
        return i;
    }

    private MovimientoResponse registrar(Insumo i, TipoMovimiento tipo, BigDecimal cantidad, BigDecimal posterior,
            String motivo, Long compraId) {
        if (motivo == null || motivo.isBlank() || motivo.length() > 500)
            throw new InvalidRequestException("Escribe un motivo de hasta 500 caracteres");
        posterior = cantidad(posterior);
        if (posterior.signum() < 0)
            throw new BusinessConflictException("Stock insuficiente para registrar la salida");
        MovimientoInventario m = new MovimientoInventario();
        m.setInsumo(i);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockAnterior(i.getStockActual());
        m.setStockPosterior(posterior);
        m.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
        m.setMotivo(motivo.strip());
        m.setCompraId(compraId);
        i.setStockActual(posterior);
        insumos.saveAndFlush(i);
        return mapper.toResponse(repository.saveAndFlush(m));
    }

    private BigDecimal cantidad(BigDecimal n) {
        try {
            BigDecimal v = n.setScale(3, RoundingMode.UNNECESSARY);
            if (v.precision() > 19)
                throw new ArithmeticException();
            return v;
        } catch (ArithmeticException ex) {
            throw new InvalidRequestException("Cantidad fuera de precisión: máximo 16 enteros y 3 decimales");
        }
    }

    public static void validarFechas(LocalDate desde, LocalDate hasta) {
        if ((desde != null && (desde.getYear() < 1 || desde.getYear() > 9999))
                || (hasta != null && (hasta.getYear() < 1 || hasta.getYear() >= 9999))
                || (desde != null && hasta != null && desde.isAfter(hasta)))
            throw new InvalidRequestException("Rango de fechas inválido");
    }

    public static LocalDateTime desde(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    public static LocalDateTime hasta(LocalDate d) {
        return d == null ? null : d.plusDays(1).atStartOfDay();
    }
}
