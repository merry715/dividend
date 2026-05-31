package com.example.dividend.client;

import com.example.dividend.dto.response.StockSearchResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 외부 API 클라이언트
 *
 * - 종목 검색 : DART corpCode.xml (전종목 코드/이름 매핑) 로컬 캐시 기반
 * - 배당 정보 : DART alotMatter.json (주당 배당금)
 * - 주가 조회 : 네이버 금융 모바일 API (인증 불필요, 한국 주식 전용)
 */
@Slf4j
@Component
public class PythonServerClient {

    /* ── DART API ─────────────────────────────────────────────── */
    private static final String DART_BASE          = "https://opendart.fss.or.kr/api";
    private static final String DART_CORP_CODE_URL = DART_BASE + "/corpCode.xml?crtfc_key=";
    private static final String DART_ALOT_URL      = DART_BASE + "/alotMatter.json";

    /* ── 네이버 금융 모바일 API (주가) ───────────────────────── */
    private static final String NAVER_STOCK_URL =
            "https://m.stock.naver.com/api/stock/%s/basic";

    @Value("${dart.api-key}")
    private String dartApiKey;

    private final RestClient restClient;

    /** 전체 상장 기업 코드 캐시 (앱 시작 시 1회 로드) */
    private final List<DartCorp> corpCache = new CopyOnWriteArrayList<>();

    public PythonServerClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Accept", "application/json, application/octet-stream")
                .build();
    }

    /** 앱 시작 후 비동기로 DART 기업 코드 전체 다운로드 */
    @PostConstruct
    public void loadCorpCodesAsync() {
        Thread t = new Thread(() -> {
            try {
                loadCorpCodes();
            } catch (Exception e) {
                log.warn("DART 기업코드 초기 로드 실패 — 첫 검색 시 재시도합니다: {}", e.getMessage());
            }
        }, "dart-corp-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── 종목 검색 ──────────────────────────────────────────────────────────────

    public List<StockSearchResult> searchStocks(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        if (corpCache.isEmpty()) {
            try {
                loadCorpCodes();
            } catch (Exception e) {
                log.warn("검색 중 기업코드 로드 실패: {}", e.getMessage());
                return Collections.emptyList();
            }
        }

        String q = query.trim().toLowerCase();
        return corpCache.stream()
                .filter(c -> c.getCorpName().toLowerCase().contains(q))
                .limit(10)
                .map(c -> {
                    StockSearchResult r = new StockSearchResult();
                    r.setCorpCode(c.getCorpCode());
                    r.setStockCode(c.getStockCode());
                    r.setStockName(c.getCorpName());
                    r.setTicker(c.getStockCode());
                    r.setCurrency("KRW");
                    r.setExchange("");
                    return r;
                })
                .toList();
    }

    /** stockCode(6자리) → DART corp_code 역조회 */
    public String findCorpCode(String stockCode) {
        if (corpCache.isEmpty()) {
            try { loadCorpCodes(); } catch (Exception e) { return null; }
        }
        return corpCache.stream()
                .filter(c -> stockCode.equals(c.getStockCode()))
                .map(DartCorp::getCorpCode)
                .findFirst()
                .orElse(null);
    }

    // ── 배당 정보 ──────────────────────────────────────────────────────────────

    public DartDividendInfo fetchDividendInfo(String corpCode, int year) {
        if (corpCode == null || corpCode.isBlank()) return null;
        try {
            String url = DART_ALOT_URL
                    + "?crtfc_key=" + dartApiKey
                    + "&corp_code=" + corpCode
                    + "&bsns_year=" + year
                    + "&reprt_code=11011";

            AlotMatterResponse resp = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(AlotMatterResponse.class);

            if (resp == null || !"000".equals(resp.getStatus()) || resp.getList() == null) {
                log.warn("DART 배당 조회 응답 없음 [corp={}, year={}, status={}]",
                        corpCode, year, resp != null ? resp.getStatus() : "null");
                return null;
            }

            for (AlotMatterItem item : resp.getList()) {
                boolean isPerShare = "주당 현금배당금(원)".equals(item.getSe())
                        || "주당현금배당금(원)".equals(item.getSe());
                boolean isCommon   = item.getStockKnd() == null
                        || item.getStockKnd().contains("보통");
                if (!isPerShare || !isCommon) continue;

                String raw = item.getThstrm();
                if (raw == null || raw.isBlank() || "-".equals(raw.trim())) continue;

                String cleaned = raw.replaceAll("[^0-9]", "");
                if (cleaned.isEmpty()) continue;

                long perShare = Long.parseLong(cleaned);
                log.info("DART 배당 조회 성공 [corp={}, year={}, 주당배당금={}원]",
                        corpCode, year, perShare);
                return new DartDividendInfo(perShare, year);
            }

            log.info("DART 사업보고서에 배당 항목 없음 [corp={}, year={}]", corpCode, year);
        } catch (Exception e) {
            log.warn("DART 배당 정보 조회 실패 [corp={}, year={}]: {}", corpCode, year, e.getMessage());
        }
        return null;
    }

    // ── 주가 조회 (네이버 금융) ───────────────────────────────────────────────

    /**
     * 네이버 금융 모바일 API로 전일 종가 조회.
     * ticker 파라미터는 6자리 종목코드 (예: 005930.KS → 005930 으로 변환).
     * 실패 시 null 반환.
     */
    public BigDecimal fetchPrice(String ticker) {
        // .KS / .KQ 접미사 제거 → 6자리 종목코드만 추출
        String stockCode = ticker.replaceAll("\\.(KS|KQ)$", "");
        try {
            String url = String.format(NAVER_STOCK_URL, stockCode);
            NaverStockBasic resp = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(NaverStockBasic.class);

            if (resp == null || resp.getClosePrice() == null || resp.getClosePrice().isBlank()) {
                log.warn("네이버 금융 응답 없음 [stockCode={}]", stockCode);
                return null;
            }

            // closePrice 는 "87,400" 형식 → 쉼표 제거 후 파싱
            String cleaned = resp.getClosePrice().replaceAll("[^0-9]", "");
            if (cleaned.isEmpty()) return null;

            BigDecimal price = new BigDecimal(cleaned);
            if (price.compareTo(BigDecimal.ZERO) <= 0) return null;

            log.info("네이버 금융 주가 조회 성공 [stockCode={}, price={}]", stockCode, price);
            return price;

        } catch (Exception e) {
            log.warn("네이버 금융 주가 조회 실패 [stockCode={}]: {}", stockCode, e.getMessage());
            return null;
        }
    }

    // ── DART corpCode.xml 다운로드 + 파싱 ─────────────────────────────────────

    private synchronized void loadCorpCodes() throws Exception {
        if (!corpCache.isEmpty()) return;

        log.info("DART 기업코드 다운로드 시작 ...");
        byte[] zipBytes = restClient.get()
                .uri(DART_CORP_CODE_URL + dartApiKey)
                .retrieve()
                .body(byte[].class);

        if (zipBytes == null || zipBytes.length == 0)
            throw new RuntimeException("DART corpCode.xml ZIP 응답이 비어 있습니다");

        byte[] xmlBytes = extractFirstXmlFromZip(zipBytes);

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlBytes));

        NodeList nodes = doc.getElementsByTagName("list");
        List<DartCorp> list = new ArrayList<>(nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el       = (Element) nodes.item(i);
            String corpCode  = getText(el, "corp_code");
            String corpName  = getText(el, "corp_name");
            String stockCode = getText(el, "stock_code");

            if (stockCode != null && stockCode.matches("\\d{6}")) {
                list.add(new DartCorp(corpCode, corpName, stockCode));
            }
        }

        corpCache.addAll(list);
        log.info("DART 기업코드 로드 완료: {}개 상장법인", corpCache.size());
    }

    private byte[] extractFirstXmlFromZip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".xml")) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new RuntimeException("ZIP 파일 내 XML을 찾을 수 없습니다");
    }

    private String getText(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return (nl.getLength() > 0) ? nl.item(0).getTextContent().trim() : null;
    }

    // ── 공개 DTO ──────────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class DartDividendInfo {
        private final long dividendPerShare;
        private final int year;
    }

    // ── 내부 모델 ─────────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    private static class DartCorp {
        private final String corpCode;
        private final String corpName;
        private final String stockCode;
    }

    // ── DART alotMatter 응답 DTO ──────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AlotMatterResponse {
        private String status;
        private String message;
        private List<AlotMatterItem> list;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AlotMatterItem {
        @JsonProperty("se")        private String se;
        @JsonProperty("stock_knd") private String stockKnd;
        @JsonProperty("thstrm")    private String thstrm;
        @JsonProperty("frmtrm")    private String frmtrm;
        @JsonProperty("lwfr")      private String lwfr;
    }

    // ── 네이버 금융 응답 DTO ──────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NaverStockBasic {
        @JsonProperty("closePrice") private String closePrice;
    }
}
