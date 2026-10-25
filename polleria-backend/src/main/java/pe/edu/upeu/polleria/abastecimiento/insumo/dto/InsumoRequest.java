package pe.edu.upeu.polleria.abastecimiento.insumo.dto;
import jakarta.validation.constraints.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.UnidadMedida;
public record InsumoRequest(@NotBlank @Size(max=100) String nombre,
 @Size(max=500) String descripcion,@NotNull UnidadMedida unidadMedida,@NotNull Boolean activo) {}
