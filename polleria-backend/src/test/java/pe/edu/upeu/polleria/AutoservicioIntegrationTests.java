package pe.edu.upeu.polleria;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.catalogo.categoria.dto.CategoriaRequest;
import pe.edu.upeu.polleria.catalogo.categoria.service.CategoriaService;
import pe.edu.upeu.polleria.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.polleria.catalogo.producto.service.ProductoService;
import pe.edu.upeu.polleria.clientes.cliente.dto.*;
import pe.edu.upeu.polleria.clientes.cliente.service.ClienteServiceImpl;
import pe.edu.upeu.polleria.pedidos.pedido.dto.*;
import pe.edu.upeu.polleria.pedidos.pedido.service.PedidoServiceImpl;
import pe.edu.upeu.polleria.pedidos.pedido.entity.EstadoPedido;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev") @Transactional
class AutoservicioIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClienteServiceImpl clientes;
    @Autowired PedidoServiceImpl pedidos;
    @Autowired CategoriaService categorias;
    @Autowired ProductoService productos;
    String sub;Long producto;Long cliente;
    PerfilRequest perfil(String correo) { return new PerfilRequest("Cliente", "Público", "912345678", correo, "Dirección principal", "Referencia"); }
    RequestPostProcessor usuario(String subject) { return jwt().jwt(j->j.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")); }
    String request() { return "{\"tipoPedido\":\"RECOJO\",\"metodoPago\":\"EFECTIVO\",\"detalles\":[{\"productoId\":"+producto+",\"cantidad\":1}]}"; }
    Long crearPedido() throws Exception {
        String body=mvc.perform(post("/api/v1/pedidos/mios").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON).content(request()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new tools.jackson.databind.ObjectMapper().readTree(body).get("id").asLong();
    }
    @BeforeEach void preparar() {
        sub="f12-"+UUID.randomUUID();cliente=clientes.crearPerfil(sub,perfil(null)).getId();
        CategoriaRequest c=new CategoriaRequest();c.setNombre("F12 "+UUID.randomUUID());c.setActivo(true);
        Long categoria=categorias.crear(c).getId();
        ProductoRequest p=new ProductoRequest();p.setNombre("Pollo F12");p.setPrecio(new BigDecimal("20.00"));p.setDisponible(true);p.setCategoriaId(categoria);
        producto=productos.crear(p).getId();
    }
    @Test void creaPerfilPorSubSinAceptarIdentidadDelBody() throws Exception {
        String nuevo="otro-"+UUID.randomUUID();
        mvc.perform(post("/api/v1/clientes/me").with(usuario(nuevo)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"telefono\":\"912345678\",\"keycloakSub\":\"ajeno\",\"clienteId\":172,\"rol\":\"ADMINISTRADOR\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.keycloakSub").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM POLLERIA.CLIENTES WHERE KEYCLOAK_SUB=?",Integer.class,nuevo)).isEqualTo(1);
    }
    @Test void obtieneYActualizaSoloMiPerfil() throws Exception {
        mvc.perform(get("/api/v1/clientes/me").with(usuario(sub))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(cliente));
        mvc.perform(put("/api/v1/clientes/me").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombres\":\"Nuevo\",\"apellidos\":\"Nombre\",\"telefono\":\"999000123\",\"direccionPrincipal\":\"Nueva dirección\",\"keycloakSub\":\"ajeno\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.telefono").value("999000123"));
        assertThat(clientes.consultarPorSub(sub).id()).isEqualTo(cliente);
    }
    @Test void perfilInexistente404() throws Exception {mvc.perform(get("/api/v1/clientes/me").with(usuario("sin-perfil"))).andExpect(status().isNotFound());}
    @Test void noConsultaPerfilAjenoPorId() throws Exception {mvc.perform(get("/api/v1/clientes/{id}",cliente).with(usuario(sub))).andExpect(status().isForbidden());}
    @Test void duplicarPerfil409() throws Exception {mvc.perform(post("/api/v1/clientes/me").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"telefono\":\"912345678\"}")).andExpect(status().isConflict());}
    @Test void correoExistenteNoVinculaPorEmail() {
        String correo="f12-"+UUID.randomUUID()+"@example.com";ClienteRequest r=perfil(correo).toInterno();Long id=clientes.crear(r).getId();
        assertThatThrownBy(()->clientes.crearPerfil("sin-vinculo",perfil(correo))).isInstanceOf(pe.edu.upeu.polleria.exception.BusinessConflictException.class);
        assertThat(jdbc.queryForObject("SELECT KEYCLOAK_SUB FROM POLLERIA.CLIENTES WHERE ID=?",String.class,id)).isNull();
    }
    @Test void uniqueOracleReal() {
        assertThatThrownBy(()->jdbc.update("UPDATE POLLERIA.CLIENTES SET KEYCLOAK_SUB=? WHERE ID=?",sub,clientes.crear(perfil(null).toInterno()).getId()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void validaPerfil() throws Exception {mvc.perform(put("/api/v1/clientes/me").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"\",\"telefono\":\"x\"}")).andExpect(status().isBadRequest());}
    @Test void creaCalculandoIdentidadPrecioYTotalSinAutoridadDelBody() throws Exception {
        mvc.perform(post("/api/v1/pedidos/mios").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON)
            .content(request().replace("{\"tipoPedido\"","{\"clienteId\":172,\"total\":0.01,\"precio\":0.01,\"estado\":\"ENTREGADO\",\"tipoPedido\"")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.cliente.id").value(cliente)).andExpect(jsonPath("$.total").value(20))
            .andExpect(jsonPath("$.estado").value("PENDIENTE")).andExpect(jsonPath("$.estadoPago").value("PENDIENTE"));
    }
    @Test void listaSoloMiosYConsultaPropio() throws Exception {Long id=crearPedido();mvc.perform(get("/api/v1/pedidos/mios").with(usuario(sub))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));mvc.perform(get("/api/v1/pedidos/mios/{id}",id).with(usuario(sub))).andExpect(status().isOk());}
    @Test void pedidoAjeno404EnLecturaYCancelacion() throws Exception {
        Long id=crearPedido();String otro="otro-"+UUID.randomUUID();clientes.crearPerfil(otro,perfil(null));
        mvc.perform(get("/api/v1/pedidos/mios/{id}",id).with(usuario(otro))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/pedidos/mios/{id}/cancelar",id).with(usuario(otro))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pedidos/mios").with(usuario(otro))).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void cancelaPendientePropio() throws Exception {Long id=crearPedido();mvc.perform(patch("/api/v1/pedidos/mios/{id}/cancelar",id).with(usuario(sub))).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADO"));}
    @Test void noCancelaConfirmadoPropio() throws Exception {Long id=crearPedido();EstadoPedidoRequest r=new EstadoPedidoRequest();r.setEstado(EstadoPedido.CONFIRMADO);pedidos.cambiarEstado(id,r);mvc.perform(patch("/api/v1/pedidos/mios/{id}/cancelar",id).with(usuario(sub))).andExpect(status().isConflict());}
    @Test void deliveryRequiereDireccionYConservaHistorico() throws Exception {
        mvc.perform(post("/api/v1/pedidos/mios").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON).content(request().replace("RECOJO","DELIVERY"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/pedidos/mios").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON).content(request().replace("RECOJO","DELIVERY").replace("{\"tipoPedido\"","{\"direccionEntrega\":\"Dirección efectiva\",\"tipoPedido\"")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.direccionEntrega").value("Dirección efectiva"));
        assertThat(clientes.miPerfil(sub).getDireccionPrincipal()).isEqualTo("Dirección principal");
    }
    @Test void productoDeshabilitado409() throws Exception {jdbc.update("UPDATE POLLERIA.PRODUCTOS SET DISPONIBLE=0 WHERE ID=?",producto);em.clear();mvc.perform(post("/api/v1/pedidos/mios").with(usuario(sub)).contentType(MediaType.APPLICATION_JSON).content(request())).andExpect(status().isConflict());}
    @Test void sinPerfilNoCreaYClienteNoAccedePedidosInternos() throws Exception {mvc.perform(post("/api/v1/pedidos/mios").with(usuario("sin-perfil")).contentType(MediaType.APPLICATION_JSON).content(request())).andExpect(status().isNotFound());mvc.perform(get("/api/v1/pedidos").with(usuario(sub))).andExpect(status().isForbidden());}
    @Test void autoservicioExigeAutenticacionYRolCliente() throws Exception {mvc.perform(get("/api/v1/clientes/me")).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/clientes/me").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAJERO")))) .andExpect(status().isForbidden());}
}
