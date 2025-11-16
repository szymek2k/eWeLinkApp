package org.example;


import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class DeviceService {

    private final EwelinkDeviceClient client;

    public DeviceService(EwelinkDeviceClient client) {
        this.client = client;
    }

    /**
     * Reactive poll – co 10 sekund pobiera listę urządzeń
     */
//    public Flux<String> pollDevicesReactive() {
//        return Flux.interval(Duration.ofSeconds(10))
//                .flatMap(i -> client.getDevices());
//    }
}