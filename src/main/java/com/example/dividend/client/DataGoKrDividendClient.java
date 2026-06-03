package com.example.dividend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 공공데이터포털 금융위원회_주식배당정보 V2 API 클라이언트.
 *
 * 엔드포인트: GET /1160100/GetStocDiviInfoService_V2/getDiviInfo_V2
 * 응답을 String 으로 수신 후 Jackson JsonNode 로 직접 파싱합니다.
 */
@Slf4j
@Component
public class DataGoKrDividendClient {

    private static final String BASE_URL =
            "https://apis.data.go.kr/1160100/GetStocDiviInfoService_V2/getDiviInfo_V2";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${data-go-kr.api-key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataGoKrDividendClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ── 공개 API ──────────────────────────────────────────────────────────────

    /**
     * 회사명 기준으로 특정 연도의 배당 레코드를 조회합니다.
     * 전체 이력 조회 후 dvdnBasDt 연도로 Java 레벨 필터링합니다.
     * baseYear 없으면 baseYear-1 자동 재시도합니다.
     */
    public List<DataGoKrDividendInfo> fetchAllDividendRecords(
            String stockCode, String companyName, int baseYear) {

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DATA_GO_KR_API_KEY 미설정 [stockCode={}]", stockCode);
            return List.of();
        }
        if (companyName == null || companyName.isBlank()) {
            log.warn("회사명 없음 [stockCode={}]", stockCode);
            return List.of();
        }

        try {
            String json = callRaw(stockCode, companyName.trim(), 100);
            if (json == null) return List.of();

            List<JsonNode> allItems = parseItems(json, stockCode, companyName);
            if (allItems.isEmpty()) return List.of();

            // baseYear → baseYear-1 순으로 연도 필터링
            for (int year = baseYear; year >= baseYear - 1; year--) {
                List<DataGoKrDividendInfo> filtered = filterByYear(allItems, year, stockCode, companyName);
                if (!filtered.isEmpty()) return filtered;
                log.info("공공데이터포털 {}년 데이터 없음 → {}년 재시도 [stockCode={}]",
                        year, year - 1, stockCode);
            }

            return List.of();

        } catch (Exception e) {
            log.warn("공공데이터포털 배당 조회 실패 [stockCode={}, companyName={}]: {}",
                    stockCode, companyName, e.getMessage());
            return List.of();
        }
    }

    /** @deprecated 3인자 메서드 사용 권장 */
    @Deprecated
    public List<DataGoKrDividendInfo> fetchAllDividendRecords(String stockCode, int baseYear) {
        log.warn("deprecated 2인자 호출 [stockCode={}]", stockCode);
        return List.of();
    }

    public Optional<LocalDate> fetchCashDividendPayDate(
            String stockCode, String companyName, int baseYear) {
        return fetchAllDividendRecords(stockCode, companyName, baseYear).stream()
                .filter(r -> r.getPayDate() != null)
                .map(DataGoKrDividendInfo::getPayDate)
                .findFirst();
    }

    // ── 내부 HTTP 호출 ────────────────────────────────────────────────────────

    private String callRaw(String stockCode, String companyName, int numOfRows) {
        // ISIN 코드로 조회 (회사명 불일치 문제 해결)
        // 회사명은 로그용으로만 사용
        String isinCd = calcKoreanIsin(stockCode);
        String param  = (isinCd != null)
                ? "&isinCd=" + isinCd
                : "&stckIssuCmpyNm=" + encode(companyName);

        String uri = BASE_URL
                + "?serviceKey=" + apiKey
                + "&resultType=json"
                + "&numOfRows=" + numOfRows
                + "&pageNo=1"
                + param;

        log.info("공공데이터포털 배당 요청 [companyName={}, isinCd={}]", companyName, isinCd);

        // URI.create() 로 RestClient의 자동 재인코딩 방지
        return restClient.get().uri(java.net.URI.create(uri)).retrieve().body(String.class);
    }

    /**
     * 6자리 종목코드로 한국 주식 ISIN 계산.
     *
     * 형식: KR{type} + code(6) + 00 + check(1) = 12자리
     *   - type=7: 보통주 (종목코드 끝자리 0~4, 6~9)
     *   - type=8: 우선주 (종목코드 끝자리 5)
     *
     * 체크 디지트: ISO 6166 Luhn 알고리즘 (홀수 위치 2배)
     *
     * 배당 종목 커버리지:
     *   - KOSPI/KOSDAQ 보통주 → KR7 (∞ 대부분)
     *   - KOSPI/KOSDAQ 우선주 → KR8
     *   - ETF·SPAC 등 비배당 종목 → 이 시스템 등록 대상 외
     */
    private String calcKoreanIsin(String stockCode) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) return null;
        try {
            // 끝자리 5 = 우선주(KR8), 그 외 = 보통주(KR7)
            String typeCode = stockCode.endsWith("5") ? "KR8" : "KR7";
            String base = typeCode + stockCode + "00";  // 11자리 (체크 디지트 제외)

            // 알파벳 → 숫자 변환 (A=10, B=11, ... K=20, R=27 ...)
            StringBuilder digits = new StringBuilder();
            for (char c : base.toCharArray()) {
                if (Character.isLetter(c)) {
                    digits.append(c - 'A' + 10);
                } else {
                    digits.append(c);
                }
            }
            // ISO 6166 Luhn: 오른쪽부터 홀수 위치(1, 3, 5, ...) 값을 2배
            String d = digits.toString();
            int sum = 0;
            for (int i = 0; i < d.length(); i++) {
                int posFromRight = d.length() - i;  // 오른쪽부터 1-based 위치
                int val = d.charAt(i) - '0';
                if (posFromRight % 2 == 1) {  // 홀수 위치 → 2배
                    val *= 2;
                    if (val > 9) val -= 9;   // 두 자리면 각 자리 합산
                }
                sum += val;
            }
            int check = (10 - (sum % 10)) % 10;
            return base + check;
        } catch (Exception e) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ── JSON 파싱 ────────────────────────────────────────────────────────────

    /**
     * JSON 문자열에서 item 배열 추출.
     * 구조: response → body → items → item (배열 or 단일 객체)
     */
    private List<JsonNode> parseItems(String json, String stockCode, String companyName) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // resultCode 확인
            JsonNode resultCode = root.path("response").path("header").path("resultCode");
            if (!resultCode.isMissingNode() && !"00".equals(resultCode.asText())) {
                log.warn("공공데이터포털 오류 [stockCode={}, code={}]", stockCode, resultCode.asText());
                return List.of();
            }

            JsonNode itemNode = root.path("response").path("body").path("items").path("item");

            if (itemNode.isMissingNode() || itemNode.isNull()) {
                log.info("공공데이터포털 item 없음 [stockCode={}]", stockCode);
                return List.of();
            }

            List<JsonNode> items = new ArrayList<>();
            if (itemNode.isArray()) {
                itemNode.forEach(items::add);
            } else if (itemNode.isObject()) {
                items.add(itemNode); // 단일 객체
            }

            log.info("공공데이터포털 전체 이력 {}건 수신 [stockCode={}]", items.size(), stockCode);
            return items;

        } catch (Exception e) {
            log.warn("공공데이터포털 JSON 파싱 실패 [stockCode={}]: {}", stockCode, e.getMessage());
            return List.of();
        }
    }

    /**
     * dvdnBasDt 연도 필터링 + 무배당 제외 + DataGoKrDividendInfo 변환.
     */
    private List<DataGoKrDividendInfo> filterByYear(
            List<JsonNode> items, int year, String stockCode, String companyName) {

        String yearStr = String.valueOf(year);
        List<DataGoKrDividendInfo> result = new ArrayList<>();

        for (JsonNode item : items) {
            String dvdnBasDt = item.path("dvdnBasDt").asText("");
            if (!dvdnBasDt.startsWith(yearStr)) continue;

            // 무배당 제외
            String rcdNm = item.path("stckDvdnRcdNm").asText("");
            if ("무배당".equals(rcdNm)) continue;

            LocalDate payDate  = parseDate(item.path("cashDvdnPayDt").asText(""));
            LocalDate baseDt   = parseDate(item.path("dvdnBasDt").asText(""));
            int amountPerShare = parseAmount(item.path("stckGenrDvdnAmt").asText("0"));

            result.add(new DataGoKrDividendInfo(payDate, amountPerShare, baseDt));
        }

        if (!result.isEmpty()) {
            log.info("공공데이터포털 배당 조회 성공 [stockCode={}, companyName={}, year={}, {}건]",
                    stockCode, companyName, year, result.size());
        }
        return result;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) return null;
        try {
            return LocalDate.parse(raw.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseAmount(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) return 0;
        try {
            return Integer.parseInt(raw.trim().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
