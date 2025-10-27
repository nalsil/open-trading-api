package com.kis.sample.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 주식 현재가 시세 조회 응답
 */
@Data
public class StockPriceResponse {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output")
    private Output output;

    @Data
    public static class Output {
        @JsonProperty("stck_prpr")  // 주식 현재가
        private String currentPrice;

        @JsonProperty("stck_oprc")  // 시가
        private String openPrice;

        @JsonProperty("stck_hgpr")  // 고가
        private String highPrice;

        @JsonProperty("stck_lwpr")  // 저가
        private String lowPrice;

        @JsonProperty("acml_vol")   // 누적 거래량
        private String volume;

        @JsonProperty("prdy_vrss")  // 전일 대비
        private String priceChange;

        @JsonProperty("prdy_ctrt")  // 전일 대비율
        private String changeRate;
    }
}
