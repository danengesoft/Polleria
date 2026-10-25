package pe.edu.upeu.polleria.clientes.cliente.api;

public interface ClienteConsultaService {
    ClienteResumen consultarCliente(Long id);
    ClienteContacto consultarContacto(Long id);
    ClienteResumen consultarPorSub(String sub);
}
