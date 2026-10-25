package pe.edu.upeu.polleria.abastecimiento.compra.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record DetalleCompraRequest(@NotNull @Positive Long insumoId,
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=3) BigDecimal cantidad,
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=4) BigDecimal costoUnitario) {}
