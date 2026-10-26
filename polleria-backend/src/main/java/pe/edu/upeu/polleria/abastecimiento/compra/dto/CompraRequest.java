package pe.edu.upeu.polleria.abastecimiento.compra.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CompraRequest(@NotNull @Positive Long proveedorId,
        @NotEmpty @Valid List<@NotNull DetalleCompraRequest> detalles) {
}
