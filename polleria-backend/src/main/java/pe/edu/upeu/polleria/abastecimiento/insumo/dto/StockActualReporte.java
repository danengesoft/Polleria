package pe.edu.upeu.polleria.abastecimiento.insumo.dto;

public record StockActualReporte(Long id, String nombre,
        pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida unidadMedida, java.math.BigDecimal stockActual,
        Boolean activo) {
}
