package pe.edu.upeu.polleria.abastecimiento.insumo.dto;

import java.math.BigDecimal;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;

public record InsumoResponse(Long id, String nombre, String descripcion, UnidadMedida unidadMedida,
        BigDecimal stockActual, Boolean activo) {
}
