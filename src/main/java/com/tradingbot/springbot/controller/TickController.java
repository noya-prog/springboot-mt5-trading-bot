package com.tradingbot.springbot.controller;

import com.tradingbot.springbot.model.SignalResponse;
import com.tradingbot.springbot.model.TickData;
import com.tradingbot.springbot.service.MarketDataService;
import com.tradingbot.springbot.service.StrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TickController {

    private final MarketDataService marketDataService;
    private final StrategyService strategyService;
    /**
     * MT5 EA calls this every second with live tick data.
     * Returns a signal: BUY / SELL / HOLD
     */
    @PostMapping("/tick")
    public ResponseEntity<SignalResponse> receiveTick(
            @RequestBody TickData tick) {

        log.info("Tick in → {} bid={} ask={} mid={}",
                tick.getSymbol(),
                tick.getBid(),
                tick.getAsk(),
                tick.getMidPrice());

        // Store tick in rolling window
        marketDataService.onTick(tick);

        SignalResponse signal = strategyService.evaluate();

        log.info("Signal → {} | reason: {}",
                signal.getSignal(), signal.getReason());

        return ResponseEntity.ok(signal);
    }

    /**
     * Health check — useful to verify the server is reachable.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("SpringBot is running | window="
                + marketDataService.getWindowSize()
                + " | FastMA=" + strategyService.getCurrentFastMa()
                + " | SlowMA=" + strategyService.getCurrentSlowMa()
                );
    }
}