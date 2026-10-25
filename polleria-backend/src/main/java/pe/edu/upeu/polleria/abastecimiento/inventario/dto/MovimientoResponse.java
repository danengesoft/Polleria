package pe.edu.upeu.polleria.abastecimiento.inventario.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.TipoMovimiento;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;
public record MovimientoResponse(Long id,Long insumoId,String nombreInsumo,UnidadMedida unidadMedida,TipoMovimiento tipo,BigDecimal cantidad,BigDecimal stockAnterior,BigDecimal stockPosterior,LocalDateTime fecha,String motivo,Long compraId) {}
