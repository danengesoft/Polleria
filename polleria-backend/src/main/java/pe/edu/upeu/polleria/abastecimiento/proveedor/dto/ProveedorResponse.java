package pe.edu.upeu.polleria.abastecimiento.proveedor.dto;

public record ProveedorResponse(Long id, String razonSocial, String ruc, String telefono, String correo,
        String direccion, Boolean activo) {
}
