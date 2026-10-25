package pe.edu.upeu.polleria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("dev")
@WithMockUser(roles="ADMINISTRADOR")
class HttpContractIntegrationTests {
    @Autowired MockMvc mvc;
    @Test void tipoDeContenidoNoSoportadoEs415YNo500() throws Exception {
        mvc.perform(post("/api/v1/categorias").contentType(MediaType.TEXT_PLAIN)
            .content("{\"nombre\":\"F17 formato\",\"activo\":true}"))
            .andExpect(status().isUnsupportedMediaType());
    }
    @Test void representacionNoSoportadaEs406YNo500() throws Exception {
        mvc.perform(get("/api/v1/productos").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isNotAcceptable());
    }
}
