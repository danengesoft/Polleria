package pe.edu.upeu.polleria.abastecimiento.compra.repository;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.polleria.abastecimiento.compra.entity.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    @Override
    @EntityGraph(attributePaths = { "detalles", "proveedor" })
    Optional<Compra> findById(Long id);

    @EntityGraph(attributePaths = { "detalles", "proveedor" })
    @Query("select c from Compra c where (:proveedor is null or c.proveedor.id=:proveedor) and (:desde is null or c.fecha>=:desde) and (:hasta is null or c.fecha<:hasta) order by c.fecha desc,c.id desc")
    List<Compra> buscar(@Param("proveedor") Long proveedor, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("select new pe.edu.upeu.polleria.abastecimiento.compra.dto.CompraAgregado(count(c),coalesce(sum(c.total),0BD)) from Compra c where (:desde is null or c.fecha>=:desde) and (:hasta is null or c.fecha<:hasta)")
    pe.edu.upeu.polleria.abastecimiento.compra.dto.CompraAgregado agregado(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("select new pe.edu.upeu.polleria.abastecimiento.compra.dto.ProveedorCompraReporte(c.proveedor.id,c.proveedor.razonSocial,count(c),sum(c.total)) from Compra c where (:desde is null or c.fecha>=:desde) and (:hasta is null or c.fecha<:hasta) group by c.proveedor.id,c.proveedor.razonSocial order by sum(c.total) desc,c.proveedor.id")
    java.util.List<pe.edu.upeu.polleria.abastecimiento.compra.dto.ProveedorCompraReporte> porProveedor(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select new pe.edu.upeu.polleria.abastecimiento.compra.dto.InsumoCompradoReporte(d.insumoId,min(d.nombreInsumo),d.unidadMedida,sum(d.cantidad),sum(d.subtotal)) from DetalleCompra d join d.compra c where (:desde is null or c.fecha>=:desde) and (:hasta is null or c.fecha<:hasta) group by d.insumoId,d.unidadMedida order by sum(d.subtotal) desc,d.insumoId")
    java.util.List<pe.edu.upeu.polleria.abastecimiento.compra.dto.InsumoCompradoReporte> insumosComprados(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
