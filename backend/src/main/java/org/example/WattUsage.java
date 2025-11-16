package org.example;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class WattUsage {
    private double voltage;
    private double current;
    private double power;

    @JsonProperty("ts")
    private long timestamp;
}