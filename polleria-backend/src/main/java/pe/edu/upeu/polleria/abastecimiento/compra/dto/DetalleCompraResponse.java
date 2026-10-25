package pe.edu.upeu.polleria.abastecimiento.compra.dto;
import java.math.BigDecimal;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;
public record DetalleCompraResponse(Long id,Long insumoId,String nombreInsumo,UnidadMedida unidadMedida,BigDecimal cantidad,BigDecimal costoUnitario,BigDecimal subtotal) {}
