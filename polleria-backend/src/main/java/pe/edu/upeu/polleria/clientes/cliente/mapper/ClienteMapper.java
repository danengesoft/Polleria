package pe.edu.upeu.polleria.clientes.cliente.mapper;

import org.mapstruct.*;
import pe.edu.upeu.polleria.clientes.cliente.dto.*;
import pe.edu.upeu.polleria.clientes.cliente.entity.Cliente;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClienteMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakSub", ignore = true)
    Cliente toEntity(ClienteRequest request);
    ClienteResponse toResponse(Cliente cliente);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakSub", ignore = true)
    void actualizar(ClienteRequest request, @MappingTarget Cliente cliente);
}
