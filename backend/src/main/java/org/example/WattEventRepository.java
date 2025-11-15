package org.example;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface WattEventRepository extends ReactiveCrudRepository<WattEvent, Long> {
    Flux<WattEvent> findAllByTimestampBetween(Instant start, Instant end);
}
