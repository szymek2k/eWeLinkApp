package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class EwelinkClient {
    private static final Logger logger = LoggerFactory.getLogger(EwelinkClient.class);


    private final WebClient webClient;
    private final String token;


    public EwelinkClient(WebClient.Builder builder,
                         @Value("${ewelink.baseurl}") String baseUrl,
                         @Value("${ewelink.token:}") String token) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.token = token;
    }


    public Mono<Double> queryCurrentWatt(String deviceSerial) {
// Note: adjust path and parsing to actual eWeLink CUBE V2 API response shape.
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/devices/{serial}/query-state").build(deviceSerial))
                .header("Authorization", token == null || token.isBlank() ? "" : "Bearer " + token)
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
// Example parsing: look for data.power.value - change per real API
                    JsonNode n = json.at("/data/power/value");
                    if (n.isMissingNode() || n.isNull()) {
// fallback: try another path
                        JsonNode alt = json.at("/data/power");
                        if (alt.isNumber()) return alt.asDouble();
                        return 0.0;
                    }
                    return n.asDouble(0.0);
                })
                .doOnError(WebClientResponseException.class, ex -> {
                    logger.warn("EweLink API returned error: {}", ex.getMessage());
                })
                .onErrorReturn(0.0);
    }
}
