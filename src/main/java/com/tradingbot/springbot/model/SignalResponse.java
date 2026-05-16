package com.tradingbot.springbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalResponse {

    // "BUY", "SELL", or "HOLD"
    private String signal;

    // Fast MA value at time of signal
    private double fastMa;

    // Slow MA value at time of signal
    private double slowMa;

    // Reason
    private String reason;


    public static SignalResponse hold(String reason) {
        return new SignalResponse("HOLD", 0, 0, reason);
    }

    public static SignalResponse buy(double fastMa, double slowMa) {
        return new SignalResponse("BUY", fastMa, slowMa,
                "Fast MA crossed above Slow MA");
    }

    public static SignalResponse sell(double fastMa, double slowMa) {
        return new SignalResponse("SELL", fastMa, slowMa,
                "Fast MA crossed below Slow MA");
    }
}