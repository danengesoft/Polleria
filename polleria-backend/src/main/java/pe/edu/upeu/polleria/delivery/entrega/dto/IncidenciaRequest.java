package pe.edu.upeu.polleria.delivery.entrega.dto;
import jakarta.validation.constraints.*;
public record IncidenciaRequest(@NotBlank @Size(max=500) String observacion) {}
