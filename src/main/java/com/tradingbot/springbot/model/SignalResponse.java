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

    // MA values at time of signal
    private double fastMa;
    private double slowMa;

    // Human-readable reason
    private String reason;

    // Execution parameters — sent to EA and used in OrderSend()

    private double sl;       // stop loss price

    private double tp;       // take profit price

    private double lotSize;  // validated lot size

    // ── Factory methods ─────────────────────────────────────────────

    public static SignalResponse hold(String reason) {
        SignalResponse r = new SignalResponse();
        r.signal = "HOLD";
        r.reason = reason;
        return r;
    }

    public static SignalResponse buy(double fastMa, double slowMa) {
        SignalResponse r = new SignalResponse();
        r.signal  = "BUY";
        r.fastMa  = fastMa;
        r.slowMa  = slowMa;
        r.reason  = "Fast MA crossed above Slow MA";
        return r;
    }

    public static SignalResponse sell(double fastMa, double slowMa) {
        SignalResponse r = new SignalResponse();
        r.signal  = "SELL";
        r.fastMa  = fastMa;
        r.slowMa  = slowMa;
        r.reason  = "Fast MA crossed below Slow MA";
        return r;
    }
}