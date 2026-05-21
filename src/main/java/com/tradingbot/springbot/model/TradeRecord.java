package com.tradingbot.springbot.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Trade details
    private String        symbol;
    private String        side;         // BUY or SELL
    private double        entryPrice;
    private double        sl;
    private double        tp;
    private double        lotSize;

    // MA values at time of signal
    private double        fastMa;
    private double        slowMa;

    // Timestamps
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    // Outcome
    @Enumerated(EnumType.STRING)
    private TradeStatus   status;       // OPEN, CLOSED
    private double        pnlUsd;       // filled when closed
    private String        closeReason;  // SL_HIT, TP_HIT, MANUAL

    public enum TradeStatus { OPEN, CLOSED }
}