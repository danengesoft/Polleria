package pe.edu.upeu.polleria.abastecimiento.compra.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record CompraResponse(Long id,ProveedorResumen proveedor,LocalDateTime fecha,BigDecimal total,List<DetalleCompraResponse> detalles) {
 public record ProveedorResumen(Long id,String razonSocial) {}
}
