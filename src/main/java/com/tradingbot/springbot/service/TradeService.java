package com.tradingbot.springbot.service;

import com.tradingbot.springbot.model.SignalResponse;
import com.tradingbot.springbot.model.TickData;
import com.tradingbot.springbot.model.TradeRecord;
import com.tradingbot.springbot.model.TradeRecord.TradeStatus;
import com.tradingbot.springbot.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final RiskService     riskService;

    /**
     * Called when a BUY or SELL signal is approved.
     * Persists the trade as OPEN.
     */
    public TradeRecord recordOpenTrade(SignalResponse signal, TickData tick) {
        TradeRecord trade = TradeRecord.builder()
                .symbol    (tick.getSymbol())
                .side      (signal.getSignal())
                .entryPrice(tick.getMidPrice())
                .sl        (signal.getSl())
                .tp        (signal.getTp())
                .lotSize   (signal.getLotSize())
                .fastMa    (signal.getFastMa())
                .slowMa    (signal.getSlowMa())
                .openedAt  (LocalDateTime.now())
                .status    (TradeStatus.OPEN)
                .build();

        tradeRepository.save(trade);
        log.info("Trade saved → {} {} at {} sl={} tp={}",
                trade.getSide(), trade.getSymbol(),
                trade.getEntryPrice(), trade.getSl(), trade.getTp());
        return trade;
    }

    /**
     * Called when EA reports a trade closed.
     * Marks the most recent open trade as CLOSED.
     */
    public void recordClosedTrade(TickData latestTick) {
        Optional<TradeRecord> openTrade = tradeRepository
                .findFirstByStatusOrderByOpenedAtDesc(TradeStatus.OPEN);

        if (openTrade.isEmpty()) {
            log.warn("Close reported but no open trade found in DB");
            return;
        }

        TradeRecord trade = openTrade.get();
        double exitPrice  = (latestTick != null)
                ? latestTick.getMidPrice() : trade.getEntryPrice();

        // Estimate P&L: (exit - entry) * lot * 100 for Gold
        double priceDiff = trade.getSide().equals("BUY")
                ? exitPrice - trade.getEntryPrice()
                : trade.getEntryPrice() - exitPrice;
        double pnl = priceDiff * trade.getLotSize() * 100;

        trade.setStatus(TradeStatus.CLOSED);
        trade.setClosedAt(LocalDateTime.now());
        trade.setPnlUsd(pnl);
        trade.setCloseReason(pnl >= 0 ? "TP_HIT" : "SL_HIT");

        tradeRepository.save(trade);
        riskService.recordTradePnl(pnl);

        log.info("Trade closed → {} pnl=${} reason={}",
                trade.getId(), pnl, trade.getCloseReason());
    }

    // ── Dashboard queries ────────────────────────────────────────────

    public List<TradeRecord> getOpenTrades() {
        return tradeRepository.findByStatus(TradeStatus.OPEN);
    }

    public List<TradeRecord> getAllTrades() {
        return tradeRepository.findAll();
    }

    public double getTodayPnl() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return tradeRepository.sumPnlSince(startOfDay);
    }

    public long getTotalTrades()  { return tradeRepository.count(); }
    public long getOpenTradeCount() {
        return tradeRepository.findByStatus(TradeStatus.OPEN).size();
    }
}