package com.tradingbot.springbot.repository;

import com.tradingbot.springbot.model.TradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<TradeRecord, Long> {

    // All open trades
    List<TradeRecord> findByStatus(TradeRecord.TradeStatus status);

    // Most recent open trade — used when a close is reported
    Optional<TradeRecord> findFirstByStatusOrderByOpenedAtDesc(
            TradeRecord.TradeStatus status);

    // All trades for a symbol
    List<TradeRecord> findBySymbolOrderByOpenedAtDesc(String symbol);

    // Total P&L since a given time
    @Query("SELECT COALESCE(SUM(t.pnlUsd), 0) FROM TradeRecord t " +
            "WHERE t.closedAt >= :since AND t.status = 'CLOSED'")
    double sumPnlSince(LocalDateTime since);
}