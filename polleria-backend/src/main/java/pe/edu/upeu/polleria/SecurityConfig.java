package pe.edu.upeu.polleria;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {
    private static final Set<String> ROLES = Set.of("ADMINISTRADOR", "CAJERO", "REPARTIDOR", "ABASTECIMIENTO", "CLIENTE");

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // The default principal name is the validated JWT subject (String).
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> access = jwt.getClaimAsMap("realm_access");
            Object roles = access == null ? null : access.get("roles");
            if (!(roles instanceof Collection<?> values)) return List.<GrantedAuthority>of();
            return values.stream().filter(String.class::isInstance).map(String.class::cast)
                    .filter(ROLES::contains).distinct()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter,
                                           SecurityErrorHandler errors) throws Exception {
        http.cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias", "/api/v1/categorias/*",
                                "/api/v1/productos", "/api/v1/productos/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/categorias", "/api/v1/productos").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categorias/*", "/api/v1/productos/*").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categorias/*", "/api/v1/productos/*").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/clientes/me", "/api/v1/pedidos/mios", "/api/v1/pedidos/mios/*").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/clientes/me", "/api/v1/pedidos/mios").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clientes/me").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/mios/*/cancelar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/clientes", "/api/v1/clientes/*").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/clientes").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clientes/*").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clientes/*").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/v1/delivery/reportes/**", "/api/v1/abastecimiento/reportes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/delivery/repartidores/me", "/api/v1/delivery/entregas/mias", "/api/v1/delivery/entregas/mias/*").hasRole("REPARTIDOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/delivery/repartidores/me").hasRole("REPARTIDOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/delivery/entregas/mias/*/iniciar", "/api/v1/delivery/entregas/mias/*/entregar", "/api/v1/delivery/entregas/mias/*/no-entregado").hasRole("REPARTIDOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/delivery/repartidores", "/api/v1/delivery/entregas", "/api/v1/delivery/entregas/*").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/delivery/entregas").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/delivery/entregas/*/reasignar", "/api/v1/pedidos/*/confirmar-pago").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers("/api/v1/abastecimiento/**").hasAnyRole("ADMINISTRADOR", "ABASTECIMIENTO")
                        // Reports must precede general order matchers.
                        .requestMatchers("/api/v1/pedidos/reportes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos", "/api/v1/pedidos/*").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pedidos/*/estado").hasAnyRole("ADMINISTRADOR", "CAJERO")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(errors).accessDeniedHandler(errors)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
