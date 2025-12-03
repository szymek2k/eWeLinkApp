package org.example.controllers;

import org.example.EwelinkAuthClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class AuthCallbackController {

    private static final Logger log = LoggerFactory.getLogger(AuthCallbackController.class);

    //@Value("#{systemEnvironment['app_id']}")
    private String APP_ID = "";
    // @Value("#{systemEnvironment['app_secret']}")
    private String APP_SECRET = "";
    private final String baseUrl = "https://eu-apia.coolkit.cc";
    private final String redirectUri = "http://localhost:8080/auth/return";

    public AuthCallbackController() {
    }

    /**
     * Endpoint, który musi być ustawiony jako 'redirect_uri' w konfiguracji Twojej aplikacji eWeLink.
     * Przechwytuje kod autoryzacyjny i rozpoczyna wymianę na tokeny.
     */
    @GetMapping("/auth/return")
    public Mono<String> handleEwelinkCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state
    ) {

        EwelinkAuthClient client = new EwelinkAuthClient();
        return client.exchangeCodeForTokensAndConnect(code)
                .doOnError(err -> System.err.println("ERROR: " + err.getMessage()))
                .thenReturn("<html><body><h1>Autoryzacja udana!</h1><p>Tokeny pobrano i rozpoczęto połączenie WebSocket.</p></body></html>")
                .onErrorResume(e -> {
                    System.err.println("Błąd w trakcie autoryzacji: " + e.getMessage());
                    return Mono.just("<html><body><h1>Błąd autoryzacji</h1><p>Wystąpił błąd: " + e.getMessage() + "</p></body></html>");
                });
    }
}
