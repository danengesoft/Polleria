package pe.edu.upeu.polleria;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.catalogo.categoria.dto.CategoriaRequest;
import pe.edu.upeu.polleria.catalogo.categoria.service.CategoriaService;
import pe.edu.upeu.polleria.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.polleria.catalogo.producto.service.ProductoService;
import pe.edu.upeu.polleria.clientes.cliente.dto.ClienteRequest;
import pe.edu.upeu.polleria.clientes.cliente.service.ClienteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class SecurityIntegrationTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    JwtAuthenticationConverter converter;
    @Autowired
    CategoriaService categorias;
    @Autowired
    ProductoService productos;
    @Autowired
    ClienteService clientes;

    private RequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.subject("user-" + role)
                .claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private long categoria() {
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre("Security " + UUID.randomUUID());
        request.setActivo(true);
        return categorias.crear(request).getId();
    }

    @Test
    void publicCatalogReadsWorkWithoutToken() throws Exception {
        long id = categoria();
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/categorias/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void anonymousCatalogWriteIs401WithTraceIdAndNoSession() throws Exception {
        mvc.perform(post("/api/v1/categorias").header("X-Trace-ID", "security-401")
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Prohibido\",\"activo\":true}"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().string("X-Trace-ID", "security-401"))
                .andExpect(jsonPath("$.traceId").value("security-401"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }

    @Test
    void invalidBearerIs401Not500EvenOnPublicRead() throws Exception {
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void administratorCanCreateUpdateAndDeleteCatalog() throws Exception {
        long id = categoria();
        mvc.perform(put("/api/v1/categorias/" + id).with(role("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Edit " + UUID.randomUUID() + "\",\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(delete("/api/v1/categorias/" + id).with(role("ADMINISTRADOR"))).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = { "CAJERO", "CLIENTE", "REPARTIDOR", "ABASTECIMIENTO" })
    void nonAdministratorCannotWriteCatalog(String role) throws Exception {
        mvc.perform(post("/api/v1/categorias").with(role(role)).header("X-Trace-ID", "security-403")
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Prohibido\",\"activo\":true}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.traceId").value("security-403"))
                .andExpect(header().string("X-Trace-ID", "security-403"));
        mvc.perform(delete("/api/v1/productos/999999999").with(role(role))).andExpect(status().isForbidden());
    }

    @Test
    void ordersAndReportsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pedidos/reportes/resumen")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "CLIENTE", "REPARTIDOR", "ABASTECIMIENTO" })
    void nonOperationalRolesHaveNoGlobalCustomerOrOrderAccess(String role) throws Exception {
        mvc.perform(get("/api/v1/clientes").with(role(role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos").with(role(role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos/175").with(role(role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos/reportes/resumen").with(role(role))).andExpect(status().isForbidden());
    }

    @Test
    void cashierCanCreateOrderButNotDeleteCustomersOrReadReports() throws Exception {
        ClienteRequest c = new ClienteRequest();
        c.setNombres("Security");
        c.setApellidos("Prueba");
        c.setTelefono("999000010");
        long cliente = clientes.crear(c).getId();
        ProductoRequest p = new ProductoRequest();
        p.setNombre("Pollo seguridad");
        p.setPrecio(new java.math.BigDecimal("20.00"));
        p.setDisponible(true);
        p.setCategoriaId(categoria());
        long producto = productos.crear(p).getId();
        mvc.perform(post("/api/v1/pedidos").with(role("CAJERO")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":" + cliente
                        + ",\"tipoPedido\":\"RECOJO\",\"metodoPago\":\"EFECTIVO\",\"detalles\":[{\"productoId\":"
                        + producto + ",\"cantidad\":2}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(40))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mvc.perform(delete("/api/v1/clientes/" + cliente).with(role("CAJERO"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pedidos/reportes/resumen").with(role("CAJERO"))).andExpect(status().isForbidden());
    }

    @Test
    void cashierCanCreateReadAndUpdateCustomer() throws Exception {
        long id = clientes.crear(customerRequest()).getId();
        mvc.perform(get("/api/v1/clientes/" + id).with(role("CAJERO"))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/clientes/" + id).with(role("CAJERO")).contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"nombres\":\"Cajero\",\"apellidos\":\"Operativo\",\"telefono\":\"999000010\",\"activo\":true}"))
                .andExpect(status().isOk());
    }

    private ClienteRequest customerRequest() {
        ClienteRequest c = new ClienteRequest();
        c.setNombres("Security");
        c.setApellidos("Prueba");
        c.setTelefono("999000010");
        return c;
    }

    @Test
    void roleConverterUsesSubjectAndOnlyApprovedRealmRoles() {
        Jwt token = Jwt.withTokenValue("test").header("alg", "RS256").subject("8cda5dab-4be0-4977-b67d-13025b527953")
                .claim("realm_access", Map.of("roles", List.of("CAJERO", "offline_access", "ADMIN", "CAJERO"))).build();
        var authentication = converter.convert(token);
        assertThat(authentication.getName()).isEqualTo(token.getSubject());
        assertThat(authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).toList()).containsExactly("ROLE_CAJERO");
        var withoutRoles = converter.convert(Jwt.withTokenValue("test").header("alg", "RS256").subject("user").build());
        assertThat(withoutRoles.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_"))
                .toList()).isEmpty();
    }

    @Test
    void healthIsMinimalAndOtherActuatorRequiresAdmin() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/info").with(role("CAJERO"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/info").with(role("ADMINISTRADOR"))).andExpect(status().isOk());
    }

    @Test
    void openApiDeclaresBearerButPublicCatalogDoesNotRequireIt() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/productos'].get.security").isEmpty());
    }

    @Test
    void corsPreflightAllowsOnlyConfiguredOriginAndBearerHeader() throws Exception {
        mvc.perform(options("/api/v1/pedidos").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type,x-trace-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(options("/api/v1/pedidos").header("Origin", "http://example.invalid")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
