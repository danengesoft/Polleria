package pe.edu.upeu.polleria.catalogo.producto.api;

import java.math.BigDecimal;

public record ProductoParaPedido(Long id, String nombre, BigDecimal precio, Boolean disponible) { }
