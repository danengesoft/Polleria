package pe.edu.upeu.polleria.abastecimiento.inventario.mapper;
import org.mapstruct.*;
import pe.edu.upeu.polleria.abastecimiento.inventario.dto.MovimientoResponse;
import pe.edu.upeu.polleria.abastecimiento.inventario.entity.MovimientoInventario;
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.ERROR)
public interface MovimientoMapper {
 @Mapping(target="insumoId",source="insumo.id") @Mapping(target="nombreInsumo",source="insumo.nombre") @Mapping(target="unidadMedida",source="insumo.unidadMedida")
 MovimientoResponse toResponse(MovimientoInventario m);
}
