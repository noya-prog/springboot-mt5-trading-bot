package com.tradingbot.springbot.service;

import com.tradingbot.springbot.model.SignalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final MarketDataService marketDataService;

    // MA periods — tune these to your strategy
    private static final int FAST_PERIOD = 5;
    private static final int SLOW_PERIOD = 20;

    // Track previous MA state to detect crossover
    private double prevFastMa = 0;
    private double prevSlowMa = 0;

    /**
     * Main strategy entry point.
     * Called on every tick from the controller.
     * Returns BUY, SELL, or HOLD.
     */
    public SignalResponse evaluate() {

        // Need at least SLOW_PERIOD prices before we can calculate
        if (marketDataService.getWindowSize() < SLOW_PERIOD) {
            int remaining = SLOW_PERIOD - marketDataService.getWindowSize();
            log.debug("Warming up... {} more ticks needed", remaining);
            return SignalResponse.hold("Warming up: " + remaining + " more ticks needed");
        }

        // Calculate current fast and slow MAs
        double fastMa = calculateMa(FAST_PERIOD);
        double slowMa = calculateMa(SLOW_PERIOD);

        log.debug("FastMA={} SlowMA={} | PrevFast={} PrevSlow={}",
                fastMa, slowMa, prevFastMa, prevSlowMa);

        SignalResponse signal = detectCrossover(fastMa, slowMa);

        // Store current MAs for next tick comparison
        prevFastMa = fastMa;
        prevSlowMa = slowMa;

        return signal;
    }

    /**
     * Detects if the fast MA has crossed above or below the slow MA.
     * Crossover = PREVIOUS state was one side, CURRENT state is the other.
     */
    private SignalResponse detectCrossover(double fastMa, double slowMa) {

        // Skip first tick — no previous state to compare
        if (prevFastMa == 0 || prevSlowMa == 0) {
            prevFastMa = fastMa;
            prevSlowMa = slowMa;
            return SignalResponse.hold("Initialising MA state");
        }

        boolean wasBelowOrEqual = prevFastMa <= prevSlowMa;
        boolean isNowAbove      = fastMa > slowMa;

        boolean wasAbove        = prevFastMa > prevSlowMa;
        boolean isNowBelow      = fastMa <= slowMa;

        // BUY
        if (wasBelowOrEqual && isNowAbove) {
            log.info("🔵 BUY signal! FastMA({}) crossed ABOVE SlowMA({})", fastMa, slowMa);
            return SignalResponse.buy(fastMa, slowMa);
        }

        // SELL
        if (wasAbove && isNowBelow) {
            log.info("⚪ SELL signal! FastMA({}) crossed BELOW SlowMA({})", fastMa, slowMa);
            return SignalResponse.sell(fastMa, slowMa);
        }

        return SignalResponse.hold(
                String.format("No crossover | FastMA=%.5f SlowMA=%.5f", fastMa, slowMa)
        );
    }

    /**
     * Calculates simple moving average of the last N prices.
     */
    private double calculateMa(int period) {
        List<Double> prices = marketDataService.getPrices(period);
        return prices.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    // Getters for monitoring
    public double getCurrentFastMa() { return prevFastMa; }
    public double getCurrentSlowMa() { return prevSlowMa; }
    public int    getFastPeriod()     { return FAST_PERIOD; }
    public int    getSlowPeriod()     { return SLOW_PERIOD; }
}