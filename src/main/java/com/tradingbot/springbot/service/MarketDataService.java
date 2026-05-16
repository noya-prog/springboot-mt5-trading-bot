package com.tradingbot.springbot.service;

import com.tradingbot.springbot.model.TickData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
public class MarketDataService {

    // Rolling window — keeps the last 200 mid prices
    private static final int MAX_WINDOW = 200;

    private final Deque<Double> priceWindow = new ArrayDeque<>();
    private TickData latestTick;

    /**
     * Called by the controller every time MT5 sends a tick.
     * Stores the mid price in the rolling window.
     */
    public void onTick(TickData tick) {
        latestTick = tick;
        double mid = tick.getMidPrice();
        priceWindow.addLast(mid);

        // Drop oldest price if window is full
        if (priceWindow.size() > MAX_WINDOW) {
            priceWindow.pollFirst();
        }

        log.debug("Tick received | {} | bid={} ask={} mid={} | window size={}",
                tick.getSymbol(),
                tick.getBid(),
                tick.getAsk(),
                mid,
                priceWindow.size());
    }

    /**
     * Returns the last N prices as a list.
     * Used by StrategyService to calculate MAs.
     */
    public List<Double> getPrices(int count) {
        List<Double> all = new ArrayList<>(priceWindow);
        int size = all.size();
        if (size < count) return all;
        return all.subList(size - count, size);
    }

    /**
     * How many prices we have so far.
     * Strategy won't fire until we have enough data.
     */
    public int getWindowSize() {
        return priceWindow.size();
    }

    public TickData getLatestTick() {
        return latestTick;
    }
}