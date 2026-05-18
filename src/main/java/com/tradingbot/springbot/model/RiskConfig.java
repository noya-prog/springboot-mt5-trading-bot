package com.tradingbot.springbot.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot.risk")
public class RiskConfig {
    // Master switch — set to false to stop all trading instantly
    private boolean enabled = true;

    // Maximum total loss allowed per day in USD
    private double maxDailyLossUsd = 50.0;

    // Lot size boundaries
    private double minLotSize = 0.01;
    private double maxLotSize = 0.10;
    private double defaultLotSize = 0.01;

    // Stop loss in pips
    private int stopLossPips = 20;

    // Take profit in pips
    private int takeProfitPips = 40;

    // Pip value for 5-digit brokers (EURUSD)
    private double pipValue = 0.00010;
}
