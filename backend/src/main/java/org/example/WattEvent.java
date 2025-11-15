package org.example;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("watt_event")
@AllArgsConstructor
@Data
@Builder
public class WattEvent {
    @Id
    private Long id;
    private String deviceId;
    private Instant timestamp;
    private Double watt;


//    public WattEvent() {}
//
//
//    public WattEvent(String deviceId, Instant timestamp, Double watt) {
//        this.deviceId = deviceId;
//        this.timestamp = timestamp;
//        this.watt = watt;
//    }


    // getters & setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//    public String getDeviceId() { return deviceId; }
//    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
//    public Instant getTimestamp() { return timestamp; }
//    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
//    public Double getWatt() { return watt; }
//    public void setWatt(Double watt) { this.watt = watt; }
}
