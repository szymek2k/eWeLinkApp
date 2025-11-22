package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
public class WattService {

    private final EwelinkDeviceClient deviceClient;

    private final EwelinkAuthClient authClient;

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
                .flatMap(i -> {
                            try {
                                return getToken()
                                        .flatMapMany(t -> deviceClient.getWattUsage(deviceId, t).flux())
                                        .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2))
                                                .filter(ex -> ex instanceof WebClientResponseException.Unauthorized)
                                                .doBeforeRetry(signal -> invalidateToken()));
                            } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
    }

    private Mono<String> getToken() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        if (token != null) {
            return Mono.just(token);
        }
//        return authClient.login()
//                .doOnNext(t -> this.token = t);
        return Mono.empty();
    }

    private void invalidateToken() {
        this.token = null;
    }
}
