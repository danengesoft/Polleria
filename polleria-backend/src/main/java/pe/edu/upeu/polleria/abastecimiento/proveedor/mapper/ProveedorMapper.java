package pe.edu.upeu.polleria.abastecimiento.proveedor.mapper;

import org.mapstruct.*;
import pe.edu.upeu.polleria.abastecimiento.proveedor.entity.Proveedor;
import pe.edu.upeu.polleria.abastecimiento.proveedor.dto.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProveedorMapper {
    @Mapping(target = "id", ignore = true)
    Proveedor toEntity(ProveedorRequest r);

    ProveedorResponse toResponse(Proveedor e);

    @Mapping(target = "id", ignore = true)
    void actualizar(ProveedorRequest r, @MappingTarget Proveedor e);
}
