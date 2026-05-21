import logging
import urllib.parse
from flask import Flask, request, jsonify
import yfinance as yf
import requests as http

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger(__name__)

app = Flask(__name__)

_HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}

# Yahoo Finance exchange code → 표준 거래소 이름 (KRX 종목만 지원)
_EXCHANGE_MAP      = {"KSC": "KOSPI", "KOE": "KOSDAQ"}
_KRX_EXCHANGE_CODES = {"KSC", "KOE"}


def _is_korean(text: str) -> bool:
    return any('가' <= c <= '힣' for c in text)


# ── 한글 검색: 네이버 금융 자동완성 API ────────────────────────────────────────

def _search_naver(query: str) -> list[dict]:
    """네이버 금융 자동완성으로 한글 종목명 검색 (국내 주식·ETF만 반환)."""
    url = "https://ac.stock.naver.com/ac?q=" + urllib.parse.quote(query) + "&target=stock,etf,etn"
    try:
        resp = http.get(url, headers=_HEADERS, timeout=5)
        data = resp.json()
        results = []
        for item in data.get("items", []):
            code      = item.get("code", "")
            name      = item.get("name", "")
            type_code = item.get("typeCode", "")   # "KOSPI" | "KOSDAQ"
            nation    = item.get("nationCode", "")  # "KOR" for domestic

            # 국내 KRX 종목 + 6자리 숫자 코드만 허용
            if nation != "KOR" or not code.isdigit() or len(code) != 6:
                continue
            if type_code not in ("KOSPI", "KOSDAQ"):
                continue

            suffix = ".KS" if type_code == "KOSPI" else ".KQ"
            results.append({
                "ticker":    code + suffix,
                "stockCode": code,
                "stockName": name,
                "exchange":  type_code,
                "currency":  "KRW",
            })
        return results[:10]
    except Exception as e:
        log.error("네이버 금융 검색 실패 [query=%s]: %s", query, e)
        return []


# ── 영문 검색: Yahoo Finance ───────────────────────────────────────────────────

def _normalize(quote: dict) -> dict | None:
    symbol        = quote.get("symbol", "")
    name          = quote.get("shortname") or quote.get("longname") or ""
    exchange_code = quote.get("exchange", "")
    quote_type    = quote.get("quoteType", "")

    if quote_type not in ("EQUITY", "ETF") or not symbol or not name:
        return None
    if exchange_code not in _KRX_EXCHANGE_CODES:
        return None

    exchange   = _EXCHANGE_MAP[exchange_code]
    stock_code = symbol.rsplit(".", 1)[0] if symbol.endswith((".KS", ".KQ")) else symbol

    if not stock_code.isdigit() or len(stock_code) != 6:
        return None

    return {
        "ticker":    symbol,
        "stockCode": stock_code,
        "stockName": name,
        "exchange":  exchange,
        "currency":  "KRW",
    }


def _search_yahoo(query: str) -> list[dict]:
    try:
        yf_results = yf.Search(query, news_count=0, max_results=15)
        quotes = getattr(yf_results, "quotes", []) or []
        return [r for q in quotes if (r := _normalize(q)) is not None]
    except Exception as e:
        log.error("yfinance search failed [query=%s]: %s", query, e)
        return []


# ── 종목 검색 엔드포인트 ──────────────────────────────────────────────────────

@app.route("/search")
def search():
    query = request.args.get("q", "").strip()
    if not query:
        return jsonify([])

    if _is_korean(query):
        return jsonify(_search_naver(query))
    else:
        return jsonify(_search_yahoo(query))


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
