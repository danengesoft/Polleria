package pe.edu.upeu.polleria.abastecimiento.inventario.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record AjusteRequest(@NotNull @Positive Long insumoId,
        @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 3) BigDecimal nuevoStock,
        @NotBlank @Size(max = 500) String motivo) {
}
