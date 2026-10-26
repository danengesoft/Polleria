package pe.edu.upeu.polleria.delivery.entrega.dto;

import jakarta.validation.constraints.*;

public record ReasignacionRequest(@NotNull @Positive Long repartidorId) {
}
