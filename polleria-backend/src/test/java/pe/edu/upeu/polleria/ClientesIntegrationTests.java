package pe.edu.upeu.polleria;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.polleria.clientes.cliente.dto.ClienteRequest;
import pe.edu.upeu.polleria.clientes.cliente.service.ClienteService;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ClientesIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private ClienteService clientes;
    @Autowired private JdbcTemplate jdbc;

    private String correoUnico() { return "cliente-" + UUID.randomUUID() + "@example.com"; }

    private String json(String correo) {
        String campoCorreo = correo == null ? "null" : "\"" + correo + "\"";
        return """
                {"nombres":"  Ana   María  ","apellidos":"  Pérez   López  ",
                "telefono":"+51912345678","correo":%s}
                """.formatted(campoCorreo);
    }

    private Long crear(String correo) {
        ClienteRequest request = new ClienteRequest();
        request.setNombres("Ana");request.setApellidos("Pérez");
        request.setTelefono("+51912345678");request.setCorreo(correo);
        return clientes.crear(request).getId();
    }

    @Test
    void creaNormalizaYAplicaActivoInicial() throws Exception {
        String correo = correoUnico();
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("  " + correo.toUpperCase() + "  ")))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nombres").value("Ana María"))
                .andExpect(jsonPath("$.apellidos").value("Pérez López"))
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void obtieneClienteExistenteYLoIncluyeEnListado() throws Exception {
        Long id = crear(correoUnico());
        mvc.perform(get("/api/v1/clientes/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void actualizaConservandoSuCorreoYPermiteDesactivar() throws Exception {
        String correo = correoUnico();Long id = crear(correo);
        mvc.perform(put("/api/v1/clientes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(correo.toUpperCase())
                                .replace("\"correo\":", "\"activo\":false,\"direccionPrincipal\":\" Calle Uno 123 \",\"correo\":")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.direccionPrincipal").value("Calle Uno 123"));
        assertThat(clientes.obtener(id).getActivo()).isFalse();
    }

    @Test
    void eliminaFisicamenteYResponde404Despues() throws Exception {
        Long id = crear(null);
        mvc.perform(delete("/api/v1/clientes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/clientes/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void rechazaNombresVaciosConTrazabilidad() throws Exception {
        mvc.perform(post("/api/v1/clientes").header("X-Trace-ID", "clientes-test")
                        .contentType(MediaType.APPLICATION_JSON).content(json(null).replace("  Ana   María  ", "  ")))
                .andExpect(status().isBadRequest()).andExpect(header().string("X-Trace-ID", "clientes-test"))
                .andExpect(jsonPath("$.traceId").value("clientes-test"));
    }

    @Test
    void rechazaApellidosVacios() throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null).replace("  Pérez   López  ", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaTelefonosInvalidos() throws Exception {
        for (String telefono : new String[]{"123", "telefono", "+52912345678", "912 345 678", "1234567890"}) {
            mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(json(null).replace("+51912345678", telefono)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void aceptaTelefonoCompartidoYConservaCerosYPrefijo() throws Exception {
        for (String telefono : new String[]{"01234567", "01234567", "+5101234567"}) {
            mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(json(null).replace("+51912345678", telefono)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.telefono").value(telefono));
        }
    }

    @Test
    void rechazaCorreoInvalido() throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(json("sin-arroba")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaDuplicadoNormalizadoAlCrearYActualizar() throws Exception {
        String correo = correoUnico();crear(correo);
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("  " + correo.toUpperCase() + "  ")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Ya existe un cliente con ese correo"));
        Long otroId = crear(null);
        mvc.perform(put("/api/v1/clientes/{id}", otroId).contentType(MediaType.APPLICATION_JSON).content(json(correo)))
                .andExpect(status().isConflict());
        assertThat(clientes.obtener(otroId).getCorreo()).isNull();
    }

    @Test
    void clienteInexistenteEs404EnConsultaActualizacionYBorrado() throws Exception {
        String ruta = "/api/v1/clientes/9223372036854775807";
        mvc.perform(get(ruta)).andExpect(status().isNotFound());
        mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON).content(json(null))).andExpect(status().isNotFound());
        mvc.perform(delete(ruta)).andExpect(status().isNotFound());
    }

    @Test
    void permiteVariosClientesSinCorreoNiDireccion() throws Exception {
        for (String correo : new String[]{null, "", "   "}) {
            mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(json(correo)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.correo").isEmpty())
                    .andExpect(jsonPath("$.direccionPrincipal").isEmpty());
        }
    }

    @Test
    void rechazaLongitudesExcesivasYActivoNulo() throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null).replace("  Ana   María  ", "A".repeat(101))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(null).replace("\"correo\":", "\"activo\":null,\"correo\":")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void indiceOracleImpideDuplicadosInclusoFueraDelServicio() {
        String correo = correoUnico();crear(correo);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO POLLERIA.CLIENTES (NOMBRES, APELLIDOS, TELEFONO, CORREO)
                VALUES ('Prueba', 'Índice', '912345678', ?)
                """, "  " + correo.toUpperCase() + "  "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
