package pe.edu.upeu.polleria.abastecimiento.inventario.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record SalidaRequest(@NotNull @Positive Long insumoId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 3) BigDecimal cantidad,
        @NotBlank @Size(max = 500) String motivo) {
}
