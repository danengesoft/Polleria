package pe.edu.upeu.polleria.delivery.entrega.dto;
import jakarta.validation.constraints.*;
public record AsignacionRequest(@NotNull @Positive Long pedidoId, @NotNull @Positive Long repartidorId) {}
