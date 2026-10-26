package pe.edu.upeu.polleria.pedidos.pedido.dto;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductoVendidoReporte(
                Long productoId,
                @Schema(description = "Nombre histórico representativo: mínimo alfabético entre los detalles filtrados; agrupa por productoId") String nombreProducto,
                long cantidadVendida,
                BigDecimal importeGenerado) {
}
