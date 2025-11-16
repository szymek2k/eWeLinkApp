package org.example;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
//@RequestMapping("/api")
public class EventController {
    private final WattPollingService polling;

    public EventController(WattPollingService polling) {
        this.polling = polling;
    }

    @GetMapping(value = "/events/{device}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<WattEvent>> stream(@PathVariable("device") String device) {
        return polling.streamDeviceWatt(device)
                .map(ev -> ServerSentEvent.builder(ev).event("watt-update").id(ev.getTimestamp().toString()).build());
    }

    @GetMapping("/history/{device}")
    public Flux<WattEvent> lastTwoDays(@PathVariable String device) {
        return polling.lastTwoDays(device);
    }

    @GetMapping("/initMessage")
    public String initMessage() {
        return "Witaj świecie frontendowy z backendu!";
    }
}
