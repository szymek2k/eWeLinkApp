package org.example;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

@Service
public class WattPollingService {
    private final EwelinkClient ewelink;
    private final WattEventRepository repo;

    public WattPollingService(EwelinkClient ewelink, WattEventRepository repo) {
        this.ewelink = ewelink;
        this.repo = repo;
    }

    public Flux<WattEvent> streamDeviceWatt(String deviceSerial) {
        // poll co 3s (lub mniejsze) — tu co 3s
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
                .flatMap(tick -> ewelink.queryCurrentWatt(deviceSerial)
                        .map(watt -> {
                            WattEvent ev = WattEvent.builder().deviceId(deviceSerial).timestamp(Instant.now()).watt(watt).build();
                            return ev;
                        })
                        .flatMap(repo::save)
                );
    }

    // pomocnicza metoda do pobrania danych z ostatnich 2 dni
    public Flux<WattEvent> lastTwoDays(String deviceSerial) {
        Instant end = Instant.now();
        Instant start = end.minus(2, ChronoUnit.DAYS);
        return repo.findAllByTimestampBetween(start, end)
                .filter(ev -> ev.getDeviceId().equals(deviceSerial))
                .sort(Comparator.comparing(WattEvent::getTimestamp));
    }
}
