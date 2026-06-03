package com.example.dividend.client;

import com.example.dividend.dto.response.StockSearchResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DART / Naver API 클라이언트.
 * - 종목 검색 : DART corpCode.xml 기반
 * - 배당 정보 : DART alotMatter.json (주당 현금배당금)
 * - 주가 조회 : 네이버 금융 모바일 API
 */
@Slf4j
@Component
public class DartNaverClient {

    private static final String DART_BASE          = "https://opendart.fss.or.kr/api";
    private static final String DART_CORP_CODE_URL = DART_BASE + "/corpCode.xml?crtfc_key=";
    private static final String DART_ALOT_URL      = DART_BASE + "/alotMatter.json";

    private static final String NAVER_STOCK_URL =
            "https://m.stock.naver.com/api/stock/%s/basic";

    private static final String KRX_LISTED_URL =
            "https://apis.data.go.kr/1160100/service/GetKrxListedInfoService/getItemInfo";

    @Value("${dart.api-key}")
    private String dartApiKey;

    @Value("${krx.api-key:}")
    private String krxApiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 전체 상장법인 코드 캐시 (DART corpCode.xml, 앱 시작 후 1회 로드) */
    private final List<DartCorp> corpCache = new CopyOnWriteArrayList<>();

    /** KRX 현재 거래가능 종목 캐시: 종목코드(6자리) → 현재 종목명 */
    private final Map<String, String> activeKrxStocks = new ConcurrentHashMap<>();

    public DartNaverClient() {
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

    /** 앱 시작 시 비동기로 DART 기업 코드 + KRX 상장종목 로드 */
    @PostConstruct
    public void loadCorpCodesAsync() {
        Thread t = new Thread(() -> {
            try {
                loadCorpCodes();
            } catch (Exception e) {
                log.warn("DART 기업코드 초기 로드 실패 — 요청 시 재시도합니다: {}", e.getMessage());
            }
            loadKrxStocksWithRetry();
        }, "dart-corp-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── 종목 검색 ─────────────────────────────────────────────────────────────

    public List<StockSearchResult> searchStocks(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        if (corpCache.isEmpty()) {
            try {
                loadCorpCodes();
            } catch (Exception e) {
                log.warn("검색용 기업코드 로드 실패: {}", e.getMessage());
                return Collections.emptyList();
            }
        }

        String q = query.trim().toLowerCase();
        // KRX 캐시 준비 여부에 따라 교차 필터 적용 여부 결정
        boolean krxReady = !activeKrxStocks.isEmpty();
        if (!krxReady) {
            log.debug("KRX 상장종목 캐시 미준비 — DART 기반 fallback 검색");
        }

        return corpCache.stream()
                .filter(c -> c.getStockCode() != null && c.getStockCode().matches("\\d{6}"))
                // KRX 교차 필터: 현재 거래가능 종목만 (캐시 미준비 시 건너뜀)
                .filter(c -> !krxReady || activeKrxStocks.containsKey(c.getStockCode()))
                // 이름 검색: KRX 현재 종목명 기준 (이름 변경 반영), fallback은 DART 이름
                .filter(c -> {
                    String name = krxReady ? activeKrxStocks.get(c.getStockCode()) : c.getCorpName();
                    return name != null && name.toLowerCase().contains(q);
                })
                .limit(10)
                .map(c -> {
                    // 표시명: KRX 현재 종목명 우선
                    String displayName = krxReady
                            ? activeKrxStocks.getOrDefault(c.getStockCode(), c.getCorpName())
                            : c.getCorpName();
                    StockSearchResult r = new StockSearchResult();
                    r.setCorpCode(c.getCorpCode());
                    r.setStockCode(c.getStockCode());
                    r.setStockName(displayName);
                    r.setTicker(c.getStockCode());
                    r.setCurrency("KRW");
                    r.setExchange("");
                    return r;
                })
                .toList();
    }

    /** stockCode(6자리) → DART corp_code 반환 */
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

    /**
     * stockCode(6자리) → DART 공식 회사명 반환.
     * 공공데이터포털 API의 stckIssuCmpyNm 과 일치하는 공식명을 사용하기 위해 활용.
     * 예) "신한지주" → "신한금융지주회사"
     */
    public String findCorpName(String stockCode) {
        if (corpCache.isEmpty()) {
            try { loadCorpCodes(); } catch (Exception e) { return null; }
        }
        return corpCache.stream()
                .filter(c -> stockCode.equals(c.getStockCode()))
                .map(DartCorp::getCorpName)
                .findFirst()
                .orElse(null);
    }

    // ── 배당 정보 ─────────────────────────────────────────────────────────────

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
                log.info("DART 배당 항목 없음 [corp={}, year={}, status={}, msg={}]",
                        corpCode, year,
                        resp != null ? resp.getStatus() : "null",
                        resp != null ? resp.getMessage() : "null");
                return null;
            }

            for (AlotMatterItem item : resp.getList()) {
                String se = item.getSe() != null ? item.getSe() : "";
                boolean isPerShare = se.contains("주당") && se.contains("배당금");
                boolean isCommon   = item.getStockKnd() == null
                        || item.getStockKnd().contains("보통") || item.getStockKnd().contains("common");
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

    /**
     * 배당 주기 감지 (다중 전략, year 및 year-1 재시도).
     *
     * 전략 1 — 사업보고서(11011) se 항목 키워드 분석:
     *   "분기"+"배당" 항목 존재 → QUARTERLY
     *   "중간/반기"+"배당" 항목 존재 → SEMI_ANNUAL
     *   "주당배당금"만 존재 → ANNUAL
     *
     * 전략 2 — 각 분기/반기 보고서 존재 여부 체크:
     *   1분기(11013) or 3분기(11014) 배당 있으면 → QUARTERLY
     *   반기(11012)만 있으면 → SEMI_ANNUAL
     *   사업보고서(11011)만 있으면 → ANNUAL
     */
    public String detectDividendCycle(String stockCode, int year) {
        String corpCode = findCorpCode(stockCode);
        if (corpCode == null) return null;

        // year, year-1 순으로 두 번 시도 (당해년도 보고서 미제출 대응)
        for (int y = year; y >= year - 1; y--) {
            try {
                // 전략 1: 사업보고서 se 항목 키워드로 분기/중간배당 감지
                String cycleFromReport = detectCycleFromAnnualReport(corpCode, y);
                if (cycleFromReport != null) {
                    log.info("DART 사업보고서 항목으로 배당주기 감지 [stockCode={}, year={}, cycle={}]",
                            stockCode, y, cycleFromReport);
                    return cycleFromReport;
                }

                // 전략 2: 각 분기/반기 보고서 존재 여부
                boolean q1 = hasDividend(corpCode, y, "11013"); // 1분기보고서
                boolean h1 = hasDividend(corpCode, y, "11012"); // 반기보고서
                boolean q3 = hasDividend(corpCode, y, "11014"); // 3분기보고서
                boolean an = hasDividend(corpCode, y, "11011"); // 사업보고서

                if (q1 || q3) return "QUARTERLY";
                if (h1)       return "SEMI_ANNUAL";
                if (an)       return "ANNUAL";

            } catch (Exception e) {
                log.warn("DART 배당 주기 감지 실패 [stockCode={}, year={}]: {}", stockCode, y, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 사업보고서(11011) alotMatter se 항목 분석으로 배당주기 추론.
     *
     * DART 사업보고서에는 다음과 같은 se 항목이 기재됨:
     *   "분기(1분기)배당금(원)" / "분기(3분기)배당금(원)" → QUARTERLY
     *   "중간배당금(원)" / "반기배당금(원)"               → SEMI_ANNUAL
     *   "주당 현금배당금(원)"                              → ANNUAL
     */
    private String detectCycleFromAnnualReport(String corpCode, int year) {
        try {
            String url = DART_ALOT_URL
                    + "?crtfc_key=" + dartApiKey
                    + "&corp_code=" + corpCode
                    + "&bsns_year=" + year
                    + "&reprt_code=11011";
            AlotMatterResponse resp = restClient.get().uri(url).retrieve()
                    .body(AlotMatterResponse.class);
            if (resp == null || !"000".equals(resp.getStatus()) || resp.getList() == null)
                return null;

            boolean hasQuarterly   = false;
            boolean hasInterim     = false;
            boolean hasDividendRow = false;

            for (AlotMatterItem item : resp.getList()) {
                String se  = item.getSe()     != null ? item.getSe()     : "";
                String raw = item.getThstrm() != null
                        ? item.getThstrm().replaceAll("[^0-9]", "") : "";
                boolean hasValue = !raw.isEmpty() && Long.parseLong(raw) > 0;

                if (se.contains("분기") && se.contains("배당") && hasValue) hasQuarterly = true;
                if ((se.contains("중간") || se.contains("반기")) && se.contains("배당") && hasValue)
                    hasInterim = true;
                if (se.contains("주당") && se.contains("배당금") && hasValue)
                    hasDividendRow = true;
            }

            if (hasQuarterly)   return "QUARTERLY";
            if (hasInterim)     return "SEMI_ANNUAL";
            if (hasDividendRow) return "ANNUAL";
        } catch (Exception ignored) {}
        return null;
    }

    /** 특정 보고서 코드에 주당배당금 항목이 있는지 확인 */
    private boolean hasDividend(String corpCode, int year, String reprtCode) {
        try {
            String url = DART_ALOT_URL
                    + "?crtfc_key=" + dartApiKey
                    + "&corp_code=" + corpCode
                    + "&bsns_year=" + year
                    + "&reprt_code=" + reprtCode;
            AlotMatterResponse resp = restClient.get().uri(url).retrieve()
                    .body(AlotMatterResponse.class);
            if (resp == null || !"000".equals(resp.getStatus()) || resp.getList() == null)
                return false;
            for (AlotMatterItem item : resp.getList()) {
                String se = item.getSe() != null ? item.getSe() : "";
                boolean isPerShare = se.contains("주당") && se.contains("배당금");
                if (!isPerShare) continue;
                String raw = item.getThstrm();
                if (raw == null || raw.isBlank() || "-".equals(raw.trim())) continue;
                String cleaned = raw.replaceAll("[^0-9]", "");
                if (!cleaned.isEmpty() && Long.parseLong(cleaned) > 0) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ── 주가 조회 (네이버 금융) ───────────────────────────────────────────────

    /**
     * 네이버 금융 모바일 API로 현재가 조회.
     * ticker 파라미터는 6자리 종목코드 (예: 005930.KS → 005930 으로 변환).
     * 실패 시 null 반환.
     */
    public BigDecimal fetchPrice(String ticker) {
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

    // ── KRX 상장종목 로드 ──────────────────────────────────────────────────────

    /**
     * 최근 영업일(최대 7일 소급)로 KRX 상장종목 전체를 로드해 activeKrxStocks에 캐시.
     * API 키 미설정 또는 실패 시 경고 로그만 남기고 종료 (DART fallback 사용).
     */
    private void loadKrxStocksWithRetry() {
        if (!activeKrxStocks.isEmpty()) return;
        if (krxApiKey == null || krxApiKey.isBlank()) {
            log.warn("KRX_API_KEY 미설정 — KRX 교차 필터 비활성화 (DART fallback 사용)");
            return;
        }
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = today.minusDays(i);
            DayOfWeek dow = candidate.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
            String basDt = candidate.format(DateTimeFormatter.BASIC_ISO_DATE);
            if (tryLoadKrxStocksForDate(basDt)) return;
        }
        log.warn("KRX 상장종목: 최근 7일 내 유효 데이터 없음 — DART fallback 사용");
    }

    /** 특정 기준일자(yyyyMMdd)로 KRX 상장종목을 조회해 캐시에 저장. 성공 시 true. */
    private boolean tryLoadKrxStocksForDate(String basDt) {
        try {
            String url = KRX_LISTED_URL
                    + "?serviceKey=" + krxApiKey.trim()
                    + "&resultType=json"
                    + "&numOfRows=5000"
                    + "&pageNo=1"
                    + "&basDt=" + basDt;

            String json = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(json);

            String resultCode = root.path("response").path("header").path("resultCode").asText("");
            if (!"00".equals(resultCode)) {
                log.warn("KRX API 오류 [basDt={}, code={}]", basDt, resultCode);
                return false;
            }

            int totalCount = root.path("response").path("body").path("totalCount").asInt(0);
            if (totalCount == 0) {
                log.info("KRX 상장종목 [basDt={}] 데이터 없음 → 이전 날 재시도", basDt);
                return false;
            }

            JsonNode items = root.path("response").path("body").path("items").path("item");
            Map<String, String> temp = new HashMap<>();
            if (items.isArray()) {
                for (JsonNode item : items) {
                    // srtnCd = "A005930" → 숫자만 추출 → 6자리 종목코드
                    String srtnCd = item.path("srtnCd").asText("");
                    String itmsNm = item.path("itmsNm").asText("").trim();
                    String code   = srtnCd.replaceAll("[^0-9]", "");
                    if (code.length() == 6 && !itmsNm.isEmpty()) {
                        temp.put(code, itmsNm);
                    }
                }
            }

            if (temp.isEmpty()) return false;
            activeKrxStocks.putAll(temp);
            log.info("KRX 상장종목 캐시 로드 완료: {}개 [basDt={}]", temp.size(), basDt);
            return true;

        } catch (Exception e) {
            log.warn("KRX 상장종목 로드 실패 [basDt={}]: {}", basDt, e.getMessage());
            return false;
        }
    }

    // ── DART corpCode.xml 로드 + 파싱 ────────────────────────────────────────

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
            String corpCls   = getText(el, "corp_cls");  // Y/K/N/E

            // 6자리 종목코드 없는 항목 제외
            if (stockCode == null || !stockCode.matches("\\d{6}")) continue;

            // corp_cls 값이 있을 때 E(비상장/상장폐지) 제외
            if ("E".equals(corpCls)) continue;

            list.add(new DartCorp(corpCode, corpName, stockCode, corpCls));
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

    // ── 공통 DTO ──────────────────────────────────────────────────────────────

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
        /** Y=KOSPI, K=KOSDAQ, N=KONEX, E=비상장/상장폐지 */
        private final String corpCls;
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
