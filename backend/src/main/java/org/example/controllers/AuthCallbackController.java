package org.example.controllers;

import org.example.EwelinkAuthClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
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
                .flatMap(auth -> {
                    // auth zawiera np. access_token, region itd.
                    System.out.println("handleEwelinkCallback " + auth.toString());
                    String token = auth.getAccessToken();

                    return getDevices(token)
                            .map(devicesJson ->
                                    "<html><body>" +
                                            "<h1>Autoryzacja udana!</h1>" +
                                            "<p>Token pobrany, WebSocket uruchomiony.</p>" +
                                            "<h2>Urządzenia użytkownika:</h2>" +
                                            "<pre>" + devicesJson + "</pre>" +
                                            "</body></html>");
                })
                .onErrorResume(e -> {
                    log.error("Błąd autoryzacji: {}", e.getMessage());
                    return Mono.just("<html><body><h1>Błąd autoryzacji</h1><p>" + e.getMessage() + "</p></body></html>");
                });
    }

    public Mono<String> getDevices(String token) {
        WebClient client = WebClient.create(baseUrl);

        return client.get()
                .uri("/v2/device/thing")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class);
    }
}
