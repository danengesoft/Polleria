package pe.edu.upeu.polleria;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {
    private final ApplicationModules modules = ApplicationModules.of(PolleriaBackendApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void recognizesCatalogoAndClientes() {
        org.assertj.core.api.Assertions.assertThat(modules.getModuleByName("catalogo")).isPresent();
        org.assertj.core.api.Assertions.assertThat(modules.getModuleByName("clientes")).isPresent();
        org.assertj.core.api.Assertions.assertThat(modules.getModuleByName("pedidos")).isPresent();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
