package pe.edu.upeu.polleria.pedidos.pedido.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.catalogo.producto.api.ProductoConsultaService;
import pe.edu.upeu.polleria.catalogo.producto.api.ProductoParaPedido;
import pe.edu.upeu.polleria.clientes.cliente.api.ClienteConsultaService;
import pe.edu.upeu.polleria.clientes.cliente.api.ClienteResumen;
import pe.edu.upeu.polleria.exception.*;
import pe.edu.upeu.polleria.pedidos.pedido.dto.*;
import pe.edu.upeu.polleria.pedidos.pedido.entity.*;
import pe.edu.upeu.polleria.pedidos.pedido.mapper.PedidoMapper;
import pe.edu.upeu.polleria.pedidos.pedido.repository.PedidoRepository;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {
    private final PedidoRepository pedidoRepository;
    private final ProductoConsultaService productoConsultaService;
    private final ClienteConsultaService clienteConsultaService;
    private final PedidoMapper pedidoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return buscar(new PedidoFiltro(), "fecha", "DESC");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> buscar(PedidoFiltro filtro, String ordenarPor, String direccion) {
        validarFiltro(filtro);
        if (!List.of("fecha", "total").contains(ordenarPor)) {
            throw new InvalidRequestException("Orden permitido: fecha o total");
        }
        Sort.Direction sentido;
        try {
            sentido = Sort.Direction.valueOf(direccion.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Dirección de orden permitida: ASC o DESC");
        }
        Sort sort = Sort.by(sentido, ordenarPor).and(Sort.by(sentido, "id"));
        return pedidoRepository.buscar(filtro.getEstado(), filtro.getTipoPedido(), filtro.getClienteId(),
                desde(filtro), hasta(filtro), sort).stream().map(this::respuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResumenReporte resumen(PedidoFiltro filtro) {
        validarFiltro(filtro);
        PedidoAgregado agregado = pedidoRepository.agregado(filtro.getEstado(), filtro.getTipoPedido(),
                filtro.getClienteId(), desde(filtro), hasta(filtro));
        BigDecimal total = agregado.totalMonetario().setScale(2, RoundingMode.HALF_UP);
        BigDecimal promedio = agregado.cantidadPedidos() == 0 ? new BigDecimal("0.00")
                : total.divide(BigDecimal.valueOf(agregado.cantidadPedidos()), 2, RoundingMode.HALF_UP);
        return new PedidoResumenReporte(agregado.cantidadPedidos(), total, promedio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoVendidoReporte> productosMasVendidos(PedidoFiltro filtro) {
        validarFiltro(filtro);
        return pedidoRepository.productosVendidos(filtro.getEstado(), filtro.getTipoPedido(),
                filtro.getClienteId(), desde(filtro), hasta(filtro));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoPedidoReporte> porEstado(PedidoFiltro filtro) {
        validarFiltro(filtro);
        EnumMap<EstadoPedido, Long> cantidades = new EnumMap<>(EstadoPedido.class);
        pedidoRepository.porEstado(filtro.getEstado(), filtro.getTipoPedido(), filtro.getClienteId(),
                desde(filtro), hasta(filtro)).forEach(fila -> cantidades.put(fila.estado(), fila.cantidadPedidos()));
        return Arrays.stream(EstadoPedido.values())
                .map(estado -> new EstadoPedidoReporte(estado, cantidades.getOrDefault(estado, 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoPedidoReporte> porTipo(PedidoFiltro filtro) {
        validarFiltro(filtro);
        EnumMap<TipoPedido, Long> cantidades = new EnumMap<>(TipoPedido.class);
        pedidoRepository.porTipo(filtro.getEstado(), filtro.getTipoPedido(), filtro.getClienteId(),
                desde(filtro), hasta(filtro)).forEach(fila -> cantidades.put(fila.tipoPedido(), fila.cantidadPedidos()));
        return Arrays.stream(TipoPedido.values())
                .map(tipo -> new TipoPedidoReporte(tipo, cantidades.getOrDefault(tipo, 0L))).toList();
    }

    private void validarFiltro(PedidoFiltro filtro) {
        if (filtro.getClienteId() != null && filtro.getClienteId() <= 0) {
            throw new InvalidRequestException("clienteId debe ser positivo");
        }
        if (filtro.getFechaDesde() != null && filtro.getFechaHasta() != null
                && filtro.getFechaDesde().isAfter(filtro.getFechaHasta())) {
            throw new InvalidRequestException("fechaDesde no puede ser posterior a fechaHasta");
        }
        // Oracle y el contrato ISO usan años de cuatro dígitos; además evita desbordar plusDays.
        if ((filtro.getFechaDesde() != null && (filtro.getFechaDesde().getYear() < 1 || filtro.getFechaDesde().getYear() > 9999))
                || (filtro.getFechaHasta() != null && (filtro.getFechaHasta().getYear() < 1 || filtro.getFechaHasta().getYear() >= 9999))) {
            throw new InvalidRequestException("Rango de fechas fuera del intervalo admitido");
        }
    }

    private LocalDateTime desde(PedidoFiltro filtro) {
        return filtro.getFechaDesde() == null ? null : filtro.getFechaDesde().atStartOfDay();
    }

    private LocalDateTime hasta(PedidoFiltro filtro) {
        return filtro.getFechaHasta() == null ? null : filtro.getFechaHasta().plusDays(1).atStartOfDay();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return respuesta(pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id)));
    }

    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        ClienteResumen cliente = clienteConsultaService.consultarCliente(request.getClienteId());
        String direccion = opcional(request.getDireccionEntrega());
        if (request.getTipoPedido() == TipoPedido.DELIVERY && direccion == null) {
            throw new InvalidRequestException("Un pedido delivery requiere dirección de entrega");
        }
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.id());
        pedido.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
        pedido.setTipoPedido(request.getTipoPedido());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setMetodoPago(request.getMetodoPago());
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        pedido.setDireccionEntrega(direccion);
        pedido.setReferenciaEntrega(opcional(request.getReferenciaEntrega()));

        BigDecimal total = new BigDecimal("0.00");
        for (DetallePedidoRequest linea : request.getDetalles()) {
            ProductoParaPedido producto = productoConsultaService.consultarParaPedido(linea.getProductoId());
            if (!Boolean.TRUE.equals(producto.disponible())) {
                throw new BusinessConflictException("Producto no disponible: " + producto.id());
            }
            if (producto.precio() == null || producto.precio().signum() <= 0) {
                throw new BusinessConflictException("Producto con precio inválido: " + producto.id());
            }
            BigDecimal precio = producto.precio().setScale(2, RoundingMode.UNNECESSARY);
            BigDecimal subtotal = importe(precio.multiply(BigDecimal.valueOf(linea.getCantidad())));
            DetallePedido detalle = new DetallePedido();
            detalle.setProductoId(producto.id());
            detalle.setNombreProducto(producto.nombre());
            detalle.setPrecioUnitario(precio);
            detalle.setCantidad(linea.getCantidad());
            detalle.setSubtotal(subtotal);
            detalle.setPedido(pedido);
            pedido.getDetalles().add(detalle);
            total = importe(total.add(subtotal));
        }
        pedido.setTotal(total);
        Pedido guardado = pedidoRepository.saveAndFlush(pedido);
        return pedidoMapper.toResponse(guardado, cliente);
    }

    @Override
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request) {
        // Serializa cambios sobre la cabecera; no permite que dos solicitudes validen el mismo estado previo.
        Pedido pedido = pedidoRepository.buscarParaCambioEstado(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
        EstadoPedido origen = pedido.getEstado();
        EstadoPedido destino = request.getEstado();
        boolean permitido;
        if (destino == EstadoPedido.CANCELADO) {
            permitido = (origen == EstadoPedido.PENDIENTE || origen == EstadoPedido.CONFIRMADO)
                    && pedido.getEstadoPago() != EstadoPago.PAGADO;
        } else {
            permitido = switch (origen) {
                case PENDIENTE -> destino == EstadoPedido.CONFIRMADO;
                case CONFIRMADO -> destino == EstadoPedido.EN_PREPARACION;
                case EN_PREPARACION -> destino == EstadoPedido.LISTO;
                case LISTO -> destino == EstadoPedido.ENTREGADO && pedido.getTipoPedido() != TipoPedido.DELIVERY;
                default -> false;
            };
        }
        if (!permitido) {
            throw new BusinessConflictException("Transición de pedido no permitida: " + origen + " → " + destino);
        }
        pedido.setEstado(destino);
        return respuesta(pedidoRepository.saveAndFlush(pedido));
    }

    private PedidoResponse respuesta(Pedido pedido) {
        return pedidoMapper.toResponse(pedido, clienteConsultaService.consultarCliente(pedido.getClienteId()));
    }

    private BigDecimal importe(BigDecimal valor) {
        BigDecimal resultado = valor.setScale(2, RoundingMode.UNNECESSARY);
        if (resultado.precision() > 19) {
            throw new InvalidRequestException("El importe supera la capacidad monetaria del pedido");
        }
        return resultado;
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
