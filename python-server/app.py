import logging
from flask import Flask, request, jsonify
import yfinance as yf

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger(__name__)

app = Flask(__name__)

# Yahoo Finance exchange code → 표준 거래소 이름
_EXCHANGE_MAP = {
    "KSC": "KOSPI",    # KOSPI  → ticker suffix .KS
    "KOE": "KOSDAQ",   # KOSDAQ → ticker suffix .KQ
    "NMS": "NASDAQ",
    "NGM": "NASDAQ",
    "NIM": "NASDAQ",
    "NYQ": "NYSE",
    "PCX": "NYSE",     # NYSE Arca
    "ASE": "NYSE",     # AMEX
}


# ── 종목 검색 ──────────────────────────────────────────────────────────────────

def _normalize(quote: dict) -> dict | None:
    symbol: str = quote.get("symbol", "")
    name: str = quote.get("shortname") or quote.get("longname") or ""
    exchange_code: str = quote.get("exchange", "")
    quote_type: str = quote.get("quoteType", "")

    if quote_type not in ("EQUITY", "ETF"):
        return None
    if not symbol or not name:
        return None

    exchange = _EXCHANGE_MAP.get(exchange_code, exchange_code)

    if symbol.endswith(".KS") or symbol.endswith(".KQ"):
        stock_code = symbol.rsplit(".", 1)[0]
        currency = "KRW"
    else:
        stock_code = symbol
        currency = "USD"

    return {
        "ticker": symbol,
        "stockCode": stock_code,
        "stockName": name,
        "exchange": exchange,
        "currency": currency,
    }


@app.route("/search")
def search():
    query = request.args.get("q", "").strip()
    if not query:
        return jsonify([])

    try:
        results = yf.Search(query, news_count=0, max_results=15)
        quotes = getattr(results, "quotes", []) or []
        output = [r for q in quotes if (r := _normalize(q)) is not None]
        return jsonify(output)
    except Exception as e:
        log.error("yfinance search failed [query=%s]: %s", query, e)
        return jsonify([])


# ── 전일 종가 조회 (Spring 스케줄러 전용 내부 API) ─────────────────────────────

@app.route("/internal/price")
def get_price():
    ticker = request.args.get("ticker", "").strip().upper()
    if not ticker:
        return jsonify({"error": "ticker is required"}), 400

    try:
        fast_info = yf.Ticker(ticker).fast_info
        price = fast_info.last_price
        if price is None:
            log.warning("종가 데이터 없음 [ticker=%s]", ticker)
            return jsonify({"error": "price not available"}), 404
        return jsonify({"ticker": ticker, "price": float(price)})
    except Exception as e:
        log.error("종가 조회 실패 [ticker=%s]: %s", ticker, e)
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
