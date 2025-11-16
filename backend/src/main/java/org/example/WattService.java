package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class WattService {

    private final EwelinkDeviceClient deviceClient;

    private final EwelinkAuthClient authClient;

//    @Value("${ewelink_email}")
//    private String email;
//
//    @Value("${ewelink_password}")
//    private String password;
//
//    @Value("${ewelink_region}")
//    private String region;
//
//    @Value("${ewelink_device_id}")
//    private String deviceId;

    @Value("#{systemEnvironment['ewelink_email']}")
    private String email;

    @Value("#{systemEnvironment['ewelink_password']}")
    private String password;

    @Value("#{systemEnvironment['ewelink_region']}")
    private String region;

    // Musisz zmienić klucz w IntelliJ na np. "EWELINK_DEVICE_ID" (bez myślnika)
// i odwoływać się do niego:
    @Value("#{systemEnvironment['ewelink_device_id']}")
    private String deviceId;
    private volatile String token; // aktualny token w pamięci

    public WattService(EwelinkAuthClient authClient, EwelinkDeviceClient deviceClient) {
        this.authClient = authClient;
        this.deviceClient = deviceClient;
    }

    /**
     * Reaktywne cykliczne pobieranie danych watt co 5 sekund
     */
    public Flux<WattUsage> pollWattReactive() {
        System.out.println(email + "; " + password + "; " + region + "; " + deviceId);
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(i -> getToken()
                        .flatMapMany(t -> deviceClient.getWattUsage(deviceId, t).flux())
                        .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2))
                                .filter(ex -> ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized)
                                .doBeforeRetry(signal -> invalidateToken()))
                );
    }

    private Mono<String> getToken() {
        if (token != null) {
            return Mono.just(token);
        }
        return authClient.login(email, password, region)
                .doOnNext(t -> this.token = t);
    }

    private void invalidateToken() {
        this.token = null;
    }
}
