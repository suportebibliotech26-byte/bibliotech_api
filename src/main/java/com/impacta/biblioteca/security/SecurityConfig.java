package com.impacta.biblioteca.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // URL do frontend (Vercel em prod, localhost em dev)
    // Configure no Railway: FRONTEND_URL=https://seu-app.vercel.app
    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ─── Rotas públicas (sem autenticação) ──────────
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ─── Funcionários: apenas ADMIN e SUPORTE ───────
                        .requestMatchers("/api/funcionarios/**").hasAnyRole("ADMIN", "SUPORTE")

                        // ─── Tickets: criação livre, ações restritas ────
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/tickets").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/**").hasAnyRole("ADMIN", "SUPORTE")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/relatorio").hasAnyRole("ADMIN", "SUPORTE")

                        // ─── Demais rotas: qualquer autenticado ─────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origens permitidas: Vercel (prod) + localhost (dev)
        config.setAllowedOrigins(List.of(
                frontendUrl,
                "http://localhost:3000",
                "http://localhost:3001"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Wildcard "*" não pode ser usado com allowCredentials=true;
        // os headers devem ser listados explicitamente
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // Expõe o header Authorization para que o frontend consiga lê-lo
        config.setExposedHeaders(List.of("Authorization"));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
