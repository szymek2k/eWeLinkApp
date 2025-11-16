package org.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class WattController {

    private final WattService wattService;

    public WattController(WattService wattService) {
        this.wattService = wattService;
    }

    @GetMapping(value = "/watt/reactive", produces = "text/event-stream")
    public Flux<WattUsage> streamWatt() {
        return wattService.pollWattReactive();
    }
}