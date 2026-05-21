package com.tradingbot.springbot.service;

import com.tradingbot.springbot.model.RiskConfig;
import com.tradingbot.springbot.model.SignalResponse;
import com.tradingbot.springbot.model.TickData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskConfig riskConfig;

    // Tracks daily loss — resets at midnight
    private double    dailyLossUsd   = 0.0;
    private LocalDate lastResetDate  = LocalDate.now();

    /**
     * Main entry point.
     * Takes a raw strategy signal and validates it against all risk rules.
     * Returns the original signal if approved, or HOLD if any check fails.
     */
    public SignalResponse validate(SignalResponse signal, TickData tick) {

        // Pass HOLD straight through — nothing to validate
        if ("HOLD".equals(signal.getSignal())) return signal;

        resetDailyLossIfNewDay();

        // Check 1 — is the bot enabled?
        if (!riskConfig.isEnabled()) {
            log.warn("Risk check FAILED: bot is disabled");
            return SignalResponse.hold("Bot is disabled via config");
        }

        // Check 2 — has daily loss limit been hit?
        if (dailyLossUsd >= riskConfig.getMaxDailyLossUsd()) {
            log.warn("Risk check FAILED: daily loss limit hit (${} / ${})",
                    dailyLossUsd, riskConfig.getMaxDailyLossUsd());
            return SignalResponse.hold(
                    String.format("Daily loss limit reached: $%.2f of $%.2f",
                            dailyLossUsd, riskConfig.getMaxDailyLossUsd())
            );
        }

        // Check 3 — is lot size within bounds?
        double lotSize = riskConfig.getDefaultLotSize();
        if (lotSize < riskConfig.getMinLotSize() || lotSize > riskConfig.getMaxLotSize()) {
            log.warn("Risk check FAILED: lot size {} out of bounds [{} - {}]",
                    lotSize, riskConfig.getMinLotSize(), riskConfig.getMaxLotSize());
            return SignalResponse.hold(
                    String.format("Lot size %.2f out of allowed range", lotSize)
            );
        }
        // Check 4 — max open trades
        int openTrades = countOpenTrades();
        if (openTrades >= riskConfig.getMaxOpenTrades()) {
            log.warn("Risk check FAILED: max open trades reached ({}/{})",
                    openTrades, riskConfig.getMaxOpenTrades());
            return SignalResponse.hold(
                    String.format("Max open trades reached: %d of %d",
                            openTrades, riskConfig.getMaxOpenTrades())
            );
        }

        // All checks passed — enrich signal with SL/TP
        double sl = calculateStopLoss(signal.getSignal(), tick);
        double tp = calculateTakeProfit(signal.getSignal(), tick);
        signal.setSl(sl);
        signal.setTp(tp);
        signal.setLotSize(lotSize);
        log.info("Risk check PASSED | lot={} sl={} tp={} dailyLoss=${}",
                lotSize, sl, tp, dailyLossUsd);

        signal.setReason(signal.getReason()
                + String.format(" | lot=%.2f sl=%.5f tp=%.5f", lotSize, sl, tp));

        return signal;
    }

    /**
     * Called after a trade closes to record the P&L.
     * Pass negative value for a loss, positive for a profit.
     */
    public void recordTradePnl(double pnlUsd) {
        if (pnlUsd < 0) {
            dailyLossUsd += Math.abs(pnlUsd);
            log.info("Daily loss updated: ${} / ${}",
                    dailyLossUsd, riskConfig.getMaxDailyLossUsd());
        }
    }
    // ── SL/TP calculations ──────────────────────────────────────────

    private double calculateStopLoss(String side, TickData tick) {
        double slDistance = riskConfig.getStopLossPips() * riskConfig.getPipValue();
        return "BUY".equals(side)
                ? tick.getAsk() - slDistance
                : tick.getBid() + slDistance;
    }

    private double calculateTakeProfit(String side, TickData tick) {
        double tpDistance = riskConfig.getTakeProfitPips() * riskConfig.getPipValue();
        return "BUY".equals(side)
                ? tick.getAsk() + tpDistance
                : tick.getBid() - tpDistance;
    }

    private void resetDailyLossIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            log.info("New trading day — resetting daily loss counter");
            dailyLossUsd  = 0.0;
            lastResetDate = today;
        }
    }
    /**
     * Spring Boot asks MT5 how many positions are open
     * by tracking signals we have approved and not yet closed.
     * We maintain a simple in-memory counter.
     */
    private int openTradeCount = 0;

    public void incrementOpenTrades()  { openTradeCount++; }
    public void decrementOpenTrades()  {
        if (openTradeCount > 0) openTradeCount--;
    }
    private int countOpenTrades()      { return openTradeCount; }
    public int  getOpenTradeCount()    { return openTradeCount; }

    public double  getDailyLossUsd()       { return dailyLossUsd; }
    public double  getMaxDailyLossUsd()    { return riskConfig.getMaxDailyLossUsd(); }
    public boolean isBotEnabled()          { return riskConfig.isEnabled(); }
    public double  getDefaultLotSize()     { return riskConfig.getDefaultLotSize(); }
}