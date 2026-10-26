package pe.edu.upeu.polleria.abastecimiento.proveedor.dto;

import jakarta.validation.constraints.*;

public record ProveedorRequest(@NotBlank @Size(max = 150) String razonSocial,
        @Pattern(regexp = "[0-9]{11}") String ruc,
        @Pattern(regexp = "^(\\+51)?[0-9]{7,9}$") String telefono,
        @Email @Size(max = 254) String correo, @Size(max = 300) String direccion, @NotNull Boolean activo) {
    public ProveedorRequest {
        ruc = opcional(ruc);
        telefono = opcional(telefono);
        correo = opcional(correo);
    }

    private static String opcional(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
