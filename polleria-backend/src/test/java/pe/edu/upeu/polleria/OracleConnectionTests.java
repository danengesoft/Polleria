package pe.edu.upeu.polleria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class OracleConnectionTests {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void connectsToDedicatedApplicationSchema() {
        assertThat(jdbc.queryForObject("SELECT USER FROM dual", String.class)).isEqualTo("POLLERIA");
        assertThat(jdbc.queryForObject("SELECT SYS_CONTEXT('USERENV', 'CON_NAME') FROM dual", String.class))
                .isEqualTo("FREEPDB1");
    }
}
