package pe.edu.upeu.polleria.abastecimiento.compra.mapper;
import org.mapstruct.*;
import pe.edu.upeu.polleria.abastecimiento.compra.entity.*;
import pe.edu.upeu.polleria.abastecimiento.compra.dto.*;
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.ERROR)
public interface CompraMapper { CompraResponse toResponse(Compra c); DetalleCompraResponse toResponse(DetalleCompra d); }
