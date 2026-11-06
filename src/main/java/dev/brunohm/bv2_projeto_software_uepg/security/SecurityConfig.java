package dev.brunohm.bv2_projeto_software_uepg.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Healthcheck do container e smoke test do deploy. Sem detalhes (show-details=never).
            "/actuator/health",
            "/actuator/health/**"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final UsuarioDetailsService usuarioDetailsService;
    private final ProblemDetailErroHandler problemDetailErroHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // API stateless com JWT: nao ha sessao nem formulario, logo nao ha CSRF a proteger.
                .csrf(csrf -> csrf.disable())
                // O front fica em outro dominio (app.bv2.tech): sem isso o navegador barra o
                // preflight OPTIONS antes mesmo do JWT ser avaliado.
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        // Recuperacao de senha: publicas por definicao — quem esqueceu
                        // a senha nao tem como se autenticar para pedir a troca.
                        .requestMatchers(HttpMethod.POST, "/auth/senha/esqueci").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/senha/redefinir").permitAll()
                        // Confirmacao de e-mail e de troca de senha: o token e a credencial, e o link costuma
                        // ser aberto em outro navegador, sem sessao.
                        .requestMatchers(HttpMethod.POST, "/auth/email/confirmar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/senha/confirmar").permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemDetailErroHandler)
                        .accessDeniedHandler(problemDetailErroHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String origensPermitidas) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // X-Conta-Id: conta em que o MASTER opera (ContaAtual). Sem ele na lista, o preflight
        // de toda chamada do MASTER em outra conta e barrado pelo navegador.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", ContaAtual.HEADER));
        // Autenticacao e JWT no header Authorization, nao cookie: credentials desligado.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Compativel com o hash $2b$12$ semeado na migration V9.
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
