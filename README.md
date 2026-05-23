# SpringBot — MT5 Automated Trading Bot

A Spring Boot backend integrated with MetaTrader 5 for real-time market data processing, automated signal generation, and trade execution. Built as a hands-on backend engineering and API integration project using a Moving Average crossover strategy on a demo account.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.x |
| Database | PostgreSQL |
| Trading Platform | MetaTrader 5 (MQL5 Expert Advisor) |
| Communication | HTTP REST via MT5 WebRequest |
| Build Tool | Maven |
| ORM | Spring Data JPA / Hibernate |

---

## Architecture

```
MetaTrader 5 (EA)
      │
      │  POST /api/tick  (every second)
      │  {"symbol":"XAUUSDm","bid":2645.50,"ask":2645.70,"time":...}
      ▼
Spring Boot Application
      │
      ├── MarketDataService    → stores rolling price window (200 ticks)
      ├── StrategyService      → MA crossover → BUY / SELL / HOLD signal
      ├── RiskService          → validates signal against risk rules
      └── TradeService         → persists trade to PostgreSQL
      │
      │  JSON response
      │  {"signal":"BUY","sl":2640.50,"tp":2650.50,"lotSize":0.01}
      ▼
MetaTrader 5 (EA)
      │
      └── OrderSend() → executes trade on demo account
```

---

## Project Structure

```
src/main/java/com/tradingbot/springbot/
├── controller/
│   └── TickController.java        # REST endpoints
├── model/
│   ├── TickData.java              # Incoming tick payload
│   ├── SignalResponse.java        # Outgoing signal + SL/TP/lot
│   ├── RiskConfig.java            # Configurable risk parameters
│   └── TradeRecord.java           # JPA entity — trade history
├── repository/
│   └── TradeRepository.java       # Spring Data JPA queries
├── service/
│   ├── MarketDataService.java     # Rolling price window
│   ├── StrategyService.java       # MA crossover logic
│   ├── RiskService.java           # Risk validation + position tracking
│   └── TradeService.java          # Trade persistence
└── SpringbotApplication.java
```

---

## Strategy — Moving Average Crossover

The bot uses a **Simple Moving Average (SMA) crossover** strategy:

| Event | Signal |
|---|---|
| Fast MA (5-period) crosses **above** Slow MA (20-period) | `BUY` |
| Fast MA (5-period) crosses **below** Slow MA (20-period) | `SELL` |
| No crossover | `HOLD` |

The strategy warms up silently for the first 20 ticks before generating any signals.

---

## Risk Management

Every signal passes through three checks before reaching the EA:

| Check | Rule | Config Key |
|---|---|---|
| Bot enabled | Master on/off switch | `bot.risk.enabled` |
| Daily loss limit | Max USD loss per day | `bot.risk.max-daily-loss-usd` |
| Max open trades | No more than N positions | `bot.risk.max-open-trades` |
| Lot size bounds | Min / max lot validation | `bot.risk.min-lot-size` / `bot.risk.max-lot-size` |

Stop-loss and take-profit levels are calculated by Spring Boot and sent with every approved signal. The EA also auto-adjusts SL/TP to respect the broker's minimum stop distance.

---

## Prerequisites

- Java 21+
- Maven 3.8+
- MetaTrader 5 (Windows)
- PostgreSQL 15+

---

## Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/springbot.git
cd springbot
```

### 2. Create the PostgreSQL database

```sql
CREATE DATABASE tradingbot;
```

### 3. Configure `application.properties`

```properties
# Server
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/tradingbot
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

# Risk settings
bot.risk.enabled=true
bot.risk.max-daily-loss-usd=50.0
bot.risk.min-lot-size=0.01
bot.risk.max-lot-size=0.10
bot.risk.default-lot-size=0.01
bot.risk.stop-loss-pips=50
bot.risk.take-profit-pips=100
bot.risk.pip-value=0.10
bot.risk.max-open-trades=3
```

### 4. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

Spring Boot starts on `http://localhost:8080`.
Hibernate auto-creates the `trade_records` table on first run.

### 5. MetaTrader 5 Setup

1. Open MetaEditor (`F4`) and create `SpringBotEA.mq5`
2. In MT5 → **Tools → Options → Expert Advisors**:
   - ✅ Allow automated trading
   - ✅ Allow WebRequest for listed URL: `http://127.0.0.1:8080`
3. Compile the EA (`F7`) and attach to a chart
4. Enable **AutoTrading** (green button in toolbar)

---

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tick` | Receives tick data from MT5 EA, returns signal |
| `POST` | `/api/trade/closed` | EA reports a closed position |
| `GET` | `/api/health` | Bot status summary string |
| `GET` | `/api/dashboard` | Full JSON status — MAs, P&L, open trades |
| `GET` | `/api/trades` | Full trade history from database |
| `POST` | `/api/bot/toggle?enabled=false` | Enable or disable the bot at runtime |

### Example tick request (MT5 → Spring Boot)

```json
POST /api/tick
{
  "symbol": "XAUUSDm",
  "bid": 2645.50,
  "ask": 2645.70,
  "time": 1716300000
}
```

### Example signal response (Spring Boot → MT5)

```json
{
  "signal": "BUY",
  "fastMa": 2644.80,
  "slowMa": 2643.20,
  "reason": "Fast MA crossed above Slow MA | lot=0.01 sl=2640.70 tp=2650.70",
  "sl": 2640.70,
  "tp": 2650.70,
  "lotSize": 0.01
}
```

---

## Dashboard Example

```
GET /api/dashboard

{
  "botEnabled": true,
  "openTrades": [...],
  "totalTrades": 24,
  "todayPnl": 12.50,
  "dailyLoss": 5.00,
  "maxDailyLoss": 50.00,
  "fastMa": 2644.80,
  "slowMa": 2643.20
}
```

---

## Development Phases

| Phase | Description | Status |
|---|---|---|
| 1 | MT5 Expert Advisor with WebRequest | ✅ Complete |
| 2 | Spring Boot project setup | ✅ Complete |
| 3 | Market Data Service + `/api/tick` endpoint | ✅ Complete |
| 4 | MA Crossover Strategy Engine | ✅ Complete |
| 5 | Risk Manager with SL/TP + daily loss limit | ✅ Complete |
| 6 | PostgreSQL persistence + REST dashboard | ✅ Complete |

---

## Key Design Decisions

**HTTP over ZeroMQ** — MT5's built-in `WebRequest()` was chosen over ZeroMQ to eliminate DLL dependency issues. For an MA crossover strategy on M5 candles, the latency difference is irrelevant.

**Spring Boot calculates SL/TP** — stop-loss and take-profit levels are computed server-side using configurable pip values and sent with every signal response. The EA auto-adjusts them against the broker's minimum stop level.

**EA detects closed trades** — the EA monitors `PositionsTotal()` on every tick and POSTs to `/api/trade/closed` when a position disappears, keeping the Spring Boot open trade counter in sync.

---

## Configuration Reference

All risk parameters are tunable without code changes via `application.properties`:

| Property | Default | Description |
|---|---|---|
| `bot.risk.enabled` | `true` | Master on/off switch |
| `bot.risk.max-daily-loss-usd` | `50.0` | Max USD loss per trading day |
| `bot.risk.min-lot-size` | `0.01` | Minimum allowed lot size |
| `bot.risk.max-lot-size` | `0.10` | Maximum allowed lot size |
| `bot.risk.default-lot-size` | `0.01` | Lot size used per trade |
| `bot.risk.stop-loss-pips` | `50` | Stop loss distance in pips |
| `bot.risk.take-profit-pips` | `100` | Take profit distance in pips |
| `bot.risk.pip-value` | `0.10` | Pip value (0.10 for Gold, 0.00010 for EURUSD) |
| `bot.risk.max-open-trades` | `3` | Max simultaneous open positions |

---

## License

This project is for educational and personal development purposes.
