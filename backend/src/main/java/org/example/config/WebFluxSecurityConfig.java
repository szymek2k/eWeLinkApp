package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class WebFluxSecurityConfig {

//    @Bean
//    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
//
//        return http
//                // 1. Wyłączenie CSRF, ponieważ używamy bezstanowego REST API (Tokeny/JWT)
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//
//                // 2. Konfiguracja reguł autoryzacji
//                .authorizeExchange(exchanges -> exchanges
//                        // Zezwól na publiczny dostęp do danych pomiarowych
//                        .pathMatchers("/api/watts/**").permitAll()
//                        // Zezwól na dostęp do endpointów autoryzacji (start, callback)
//                        .pathMatchers("/auth/**").permitAll()
//                        // Wszystkie inne ścieżki (np. /admin) wymagają uwierzytelnienia
//                        .anyExchange().authenticated()
//                )
//                // 3. Wyłączenie domyślnych mechanizmów uwierzytelniania opartych na stanie
//                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//
//                .build();
//    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // Wyłączenie CSRF w środowisku deweloperskim
                .authorizeExchange(exchanges -> exchanges
                        // Zezwól na dostęp publiczny do endpointów OAuth
                        .pathMatchers("/auth/**").permitAll()
                        // Wymagaj uwierzytelnienia dla wszystkich innych zapytań
                        .anyExchange().authenticated()
                )
                // Usunięcie domyślnego HTTP Basic Auth, jeśli było wcześniej włączone
                .httpBasic(httpBasic -> httpBasic.disable())
                .build();
    }
}