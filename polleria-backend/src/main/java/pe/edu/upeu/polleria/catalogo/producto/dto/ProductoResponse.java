package pe.edu.upeu.polleria.catalogo.producto.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private java.math.BigDecimal precio;
    private Boolean disponible;
    private pe.edu.upeu.polleria.catalogo.categoria.dto.CategoriaResumen categoria;
    private String imagenUrl;
}
