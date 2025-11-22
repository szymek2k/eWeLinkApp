package org.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class AuthCallbackController {

    private static final Logger log = LoggerFactory.getLogger(AuthCallbackController.class);
    private final EwelinkAuthClient authService;

    public AuthCallbackController(EwelinkAuthClient authService) {
        this.authService = authService;
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
        return authService.authenticateOrRefresh(code);
    }

}