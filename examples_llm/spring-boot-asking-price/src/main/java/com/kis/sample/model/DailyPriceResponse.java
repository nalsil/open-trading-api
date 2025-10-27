package com.kis.sample.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 주식 일봉 조회 응답
 */
@Data
public class DailyPriceResponse {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output2")
    private List<DailyPrice> output2;

    @Data
    public static class DailyPrice {
        @JsonProperty("stck_bsop_date")  // 주식 영업 일자
        private String date;

        @JsonProperty("stck_clpr")  // 주식 종가
        private String closePrice;

        @JsonProperty("stck_oprc")  // 주식 시가
        private String openPrice;

        @JsonProperty("stck_hgpr")  // 주식 최고가
        private String highPrice;

        @JsonProperty("stck_lwpr")  // 주식 최저가
        private String lowPrice;

        @JsonProperty("acml_vol")   // 누적 거래량
        private String volume;

        @JsonProperty("acml_tr_pbmn")  // 누적 거래 대금
        private String tradeValue;

        @JsonProperty("prdy_vrss")  // 전일 대비
        private String priceChange;

        @JsonProperty("prdy_ctrt")  // 전일 대비율
        private String changeRate;
    }
}
