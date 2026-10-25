package pe.edu.upeu.polleria.abastecimiento.insumo.mapper;
import org.mapstruct.*;
import pe.edu.upeu.polleria.abastecimiento.insumo.entity.Insumo;
import pe.edu.upeu.polleria.abastecimiento.insumo.dto.*;
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.ERROR)
public interface InsumoMapper {
 @Mapping(target="id",ignore=true) @Mapping(target="stockActual",ignore=true)
 Insumo toEntity(InsumoRequest r);
 InsumoResponse toResponse(Insumo e);
 @Mapping(target="id",ignore=true) @Mapping(target="stockActual",ignore=true)
 void actualizar(InsumoRequest r,@MappingTarget Insumo e);
}
