package com.tradingbot.springbot.controller;

import com.tradingbot.springbot.model.RiskConfig;
import com.tradingbot.springbot.model.SignalResponse;
import com.tradingbot.springbot.model.TickData;
import com.tradingbot.springbot.model.TradeRecord;
import com.tradingbot.springbot.service.MarketDataService;
import com.tradingbot.springbot.service.RiskService;
import com.tradingbot.springbot.service.StrategyService;
import com.tradingbot.springbot.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TickController {

    private final MarketDataService marketDataService;
    private final StrategyService strategyService;
    private final RiskService riskService;
    private final TradeService tradeService;
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
        // Run Strategy
        SignalResponse signal = strategyService.evaluate();
        // Validate through risk manager
        SignalResponse approved = riskService.validate(signal, tick);
        // Track open trades
        if (!approved.getSignal().equals("HOLD")) {
            riskService.incrementOpenTrades();
            tradeService.recordOpenTrade(approved, tick);
        }
        log.info("Final signal → {} sl={} tp={} lot={} | {}",
                approved.getSignal(),
                approved.getSl(),
                approved.getTp(),
                approved.getLotSize(),
                approved.getReason());

        return ResponseEntity.ok(approved);
    }
    /**
     * EA calls this when a trade closes (hit SL or TP).
     * Decrements the open trade counter so new trades are allowed.
     */
    @PostMapping("/trade/closed")
    public ResponseEntity<String> tradeClosed() {
        riskService.decrementOpenTrades();
        tradeService.recordClosedTrade(marketDataService.getLatestTick());
        log.info("Trade closed reported | openTrades={}",
                riskService.getOpenTradeCount());
        return ResponseEntity.ok("acknowledged");
    }

    /**
     * Health check — useful to verify the server is reachable.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok( "SpringBot running"
                        + " | enabled="    + riskService.isBotEnabled()
                        + " | window="     + marketDataService.getWindowSize()
                        + " | FastMA="     + strategyService.getCurrentFastMa()
                        + " | SlowMA="     + strategyService.getCurrentSlowMa()
                        + " | dailyLoss=$" + riskService.getDailyLossUsd()
                        + " / $"           + riskService.getMaxDailyLossUsd()
                );
    }
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "botEnabled",   riskService.isBotEnabled(),
                "openTrades",   tradeService.getOpenTrades(),
                "totalTrades",  tradeService.getTotalTrades(),
                "todayPnl",     tradeService.getTodayPnl(),
                "dailyLoss",    riskService.getDailyLossUsd(),
                "maxDailyLoss", riskService.getMaxDailyLossUsd(),
                "fastMa",       strategyService.getCurrentFastMa(),
                "slowMa",       strategyService.getCurrentSlowMa()
        ));
    }
    @GetMapping("/trades")
    public ResponseEntity<List<TradeRecord>> getAllTrades() {
        return ResponseEntity.ok(tradeService.getAllTrades());
    }

}