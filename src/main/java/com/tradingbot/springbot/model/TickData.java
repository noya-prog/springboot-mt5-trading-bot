package com.tradingbot.springbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickData {
    @JsonProperty("symbol")
    private String symbol;
    @JsonProperty("bid")
    private double bid;

    // Ask price from MT5
    @JsonProperty("ask")
    private double ask;

    // Unix timestamp sent by EA
    @JsonProperty("time")
    private long time;

    // Mid price
    public double getMidPrice() {
        return (bid + ask) / 2.0;
    }

    // Convert unix time to Instant for logging
    public Instant getTimestamp() {
        return Instant.ofEpochSecond(time);
    }
}
