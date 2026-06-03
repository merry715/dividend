package com.example.dividend.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공공데이터포털 금융위원회_주식배당정보 V2 응답 DTO.
 * endpoint: GET /1160100/GetStocDiviInfoService_V2/getDiviInfo_V2
 *
 * 실제 응답 구조:
 * {
 *   "response": {
 *     "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE." },
 *     "body": {
 *       "items": { "item": [ {...}, ... ] },
 *       "totalCount": 67
 *     }
 *   }
 * }
 *
 * @JsonAutoDetect(fieldVisibility = ANY) 로 private 필드를 Jackson이 직접 접근하여
 * setter 없이도 역직렬화가 가능하도록 설정합니다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DataGoKrDividendResponse {

    @JsonProperty("response")
    private ResponseWrapper response;

    // 기존 호출부 호환 편의 메서드
    public Header getHeader() { return response != null ? response.header : null; }
    public Body   getBody()   { return response != null ? response.body   : null; }

    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class ResponseWrapper {
        private Header header;
        private Body   body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Body {
        private Items  items;
        private Object totalCount;

        public String getTotalCount() {
            return totalCount != null ? String.valueOf(totalCount) : null;
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Items {
        @JsonDeserialize(using = ItemListDeserializer.class)
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Item {
        private String basDt;
        /** 배당기준일 ← 연도 필터 기준 */
        private String dvdnBasDt;
        /** 현금배당지급일자 ← 실제 지급일 */
        private String cashDvdnPayDt;
        /** 주당배당금액 */
        private String stckGenrDvdnAmt;
        /** 배당 종류: 현금배당 / 무배당 등 */
        private String stckDvdnRcdNm;
        private String stckIssuCmpyNm;
        private String isinCd;
    }
}
