package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

//@EnableWebFluxSecurity
public class SecurityConfig {
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http
//                .authorizeExchange(ex -> ex
//                        .pathMatchers("/", "/login**").permitAll()
//                        .anyExchange().authenticated()
//                )
//                .oauth2Login();
//        return http.build();
//    }
//
//    @Bean
//    public WebClient webClient(ReactiveClientRegistrationRepository clients,
//                               ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2) {
//        return WebClient.builder()
//                .filter(oauth2)  // wstrzykuje token OAuth2
//                .build();
//    }
}
