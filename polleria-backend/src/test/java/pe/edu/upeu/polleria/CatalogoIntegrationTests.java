package pe.edu.upeu.polleria;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.catalogo.categoria.dto.CategoriaRequest;
import pe.edu.upeu.polleria.catalogo.categoria.service.CategoriaService;
import pe.edu.upeu.polleria.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.polleria.catalogo.producto.service.ProductoService;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.security.test.context.support.WithMockUser(username = "regression", roles = "ADMINISTRADOR")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class CatalogoIntegrationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private CategoriaService categorias;
    @Autowired
    private ProductoService productos;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long categoria(String nombre) {
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre(nombre);
        request.setActivo(true);
        return categorias.crear(request).getId();
    }

    private Long categoria() {
        return categoria("Prueba " + UUID.randomUUID());
    }

    private String productoJson(Long categoriaId, String precio) {
        return """
                {"nombre":"1/4 Pollo","descripcion":"Con papas","precio":%s,
                "disponible":true,"categoriaId":%d,"imagenUrl":"https://example.com/pollo.jpg"}
                """.formatted(precio, categoriaId);
    }

    @Test
    void creaCategoriaValidaYDevuelveLocation() throws Exception {
        mvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Categoría " + UUID.randomUUID() + "\",\"activo\":true}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.activo").value(true)).andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void rechazaCategoriaSinNombreYConservaTraceId() throws Exception {
        mvc.perform(post("/api/v1/categorias").header("X-Trace-ID", "catalogo-test")
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \",\"activo\":true}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").value("catalogo-test"))
                .andExpect(header().string("X-Trace-ID", "catalogo-test"));
    }

    @Test
    void obtieneCategoriaExistente() throws Exception {
        Long id = categoria();
        mvc.perform(get("/api/v1/categorias/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void categoriaInexistenteEs404() throws Exception {
        mvc.perform(get("/api/v1/categorias/9223372036854775807"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rechazaNombreDuplicadoNormalizandoMayusculasYEspacios() throws Exception {
        String nombre = "Pollos " + UUID.randomUUID();
        categoria(nombre);
        mvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"  " + nombre.toUpperCase().replace(" ", "   ") + "  \",\"activo\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizaCategoriaYPermiteDesactivarla() throws Exception {
        Long id = categoria();
        mvc.perform(put("/api/v1/categorias/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Actualizada " + UUID.randomUUID() + "\",\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void creaProductoConRelacionCorrecta() throws Exception {
        Long id = categoria();
        mvc.perform(
                post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(productoJson(id, "18.50")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.categoria.id").value(id))
                .andExpect(jsonPath("$.nombre").value("1/4 Pollo"))
                .andExpect(jsonPath("$.precio").value(18.50)).andExpect(jsonPath("$.stock").doesNotExist());
    }

    @Test
    void rechazaProductoConCategoriaInexistente() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON)
                .content(productoJson(Long.MAX_VALUE, "18")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Categoría no encontrada: " + Long.MAX_VALUE));
    }

    @Test
    void rechazaPrecioCeroNegativoOConMasDeDosDecimales() throws Exception {
        Long id = categoria();
        for (String precio : new String[] { "0", "-1", "18.555", "100000000.00" }) {
            mvc.perform(
                    post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(productoJson(id, precio)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void actualizaProductoConsultaYFiltraRelacion() throws Exception {
        Long anterior = categoria();
        Long nueva = categoria();
        ProductoRequest request = new ProductoRequest();
        request.setNombre("Pollo");
        request.setPrecio(new BigDecimal("18.00"));
        request.setDisponible(true);
        request.setCategoriaId(anterior);
        Long productoId = productos.crear(request).getId();
        mvc.perform(put("/api/v1/productos/{id}", productoId).contentType(MediaType.APPLICATION_JSON)
                .content(productoJson(nueva, "20.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(productoId))
                .andExpect(jsonPath("$.categoria.id").value(nueva));
        mvc.perform(get("/api/v1/productos/{id}", productoId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(20.00));
        mvc.perform(get("/api/v1/productos").param("categoriaId", nueva.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(productoId));
        assertThat(productos.listarPorCategoria(anterior)).isEmpty();
    }

    @Test
    void impideEliminarCategoriaUtilizadaSinExponerSql() throws Exception {
        Long id = categoria();
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(productoJson(id, "18")))
                .andExpect(status().isCreated());
        // Simula el nuevo contexto de persistencia de una petición HTTP independiente.
        entityManager.clear();
        mvc.perform(delete("/api/v1/categorias/{id}", id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No se puede eliminar una categoría utilizada; puede desactivarla"));
    }

    @Test
    void eliminaProductoYDespuesCategoriaLibre() throws Exception {
        Long id = categoria();
        ProductoRequest request = new ProductoRequest();
        request.setNombre("Producto eliminable");
        request.setPrecio(new BigDecimal("5.00"));
        request.setDisponible(false);
        request.setCategoriaId(id);
        Long productoId = productos.crear(request).getId();
        mvc.perform(delete("/api/v1/productos/{id}", productoId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/productos/{id}", productoId)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/categorias/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void rechazaCategoriaNulaYUrlNoHttp() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Pollo\",\"precio\":18,\"disponible\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON)
                .content(productoJson(categoria(), "18").replace("https://example.com/pollo.jpg",
                        "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaJsonMalformadoYParametroInvalido() throws Exception {
        mvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/productos/no-es-id")).andExpect(status().isBadRequest());
    }
}
