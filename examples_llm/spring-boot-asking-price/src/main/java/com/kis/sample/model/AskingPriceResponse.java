package com.kis.sample.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class AskingPriceResponse {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output1")
    private Output1 output1;

    @JsonProperty("output2")
    private Output2 output2;

    @Data
    public static class Output1 {
        // 호가 정보
        @JsonProperty("askp1")
        private String askp1; // 매도호가1

        @JsonProperty("askp2")
        private String askp2; // 매도호가2

        @JsonProperty("askp3")
        private String askp3; // 매도호가3

        @JsonProperty("askp4")
        private String askp4; // 매도호가4

        @JsonProperty("askp5")
        private String askp5; // 매도호가5

        @JsonProperty("askp6")
        private String askp6; // 매도호가6

        @JsonProperty("askp7")
        private String askp7; // 매도호가7

        @JsonProperty("askp8")
        private String askp8; // 매도호가8

        @JsonProperty("askp9")
        private String askp9; // 매도호가9

        @JsonProperty("askp10")
        private String askp10; // 매도호가10

        @JsonProperty("bidp1")
        private String bidp1; // 매수호가1

        @JsonProperty("bidp2")
        private String bidp2; // 매수호가2

        @JsonProperty("bidp3")
        private String bidp3; // 매수호가3

        @JsonProperty("bidp4")
        private String bidp4; // 매수호가4

        @JsonProperty("bidp5")
        private String bidp5; // 매수호가5

        @JsonProperty("bidp6")
        private String bidp6; // 매수호가6

        @JsonProperty("bidp7")
        private String bidp7; // 매수호가7

        @JsonProperty("bidp8")
        private String bidp8; // 매수호가8

        @JsonProperty("bidp9")
        private String bidp9; // 매수호가9

        @JsonProperty("bidp10")
        private String bidp10; // 매수호가10

        @JsonProperty("askp_rsqn1")
        private String askpRsqn1; // 매도호가잔량1

        @JsonProperty("askp_rsqn2")
        private String askpRsqn2; // 매도호가잔량2

        @JsonProperty("askp_rsqn3")
        private String askpRsqn3; // 매도호가잔량3

        @JsonProperty("askp_rsqn4")
        private String askpRsqn4; // 매도호가잔량4

        @JsonProperty("askp_rsqn5")
        private String askpRsqn5; // 매도호가잔량5

        @JsonProperty("askp_rsqn6")
        private String askpRsqn6; // 매도호가잔량6

        @JsonProperty("askp_rsqn7")
        private String askpRsqn7; // 매도호가잔량7

        @JsonProperty("askp_rsqn8")
        private String askpRsqn8; // 매도호가잔량8

        @JsonProperty("askp_rsqn9")
        private String askpRsqn9; // 매도호가잔량9

        @JsonProperty("askp_rsqn10")
        private String askpRsqn10; // 매도호가잔량10

        @JsonProperty("bidp_rsqn1")
        private String bidpRsqn1; // 매수호가잔량1

        @JsonProperty("bidp_rsqn2")
        private String bidpRsqn2; // 매수호가잔량2

        @JsonProperty("bidp_rsqn3")
        private String bidpRsqn3; // 매수호가잔량3

        @JsonProperty("bidp_rsqn4")
        private String bidpRsqn4; // 매수호가잔량4

        @JsonProperty("bidp_rsqn5")
        private String bidpRsqn5; // 매수호가잔량5

        @JsonProperty("bidp_rsqn6")
        private String bidpRsqn6; // 매수호가잔량6

        @JsonProperty("bidp_rsqn7")
        private String bidpRsqn7; // 매수호가잔량7

        @JsonProperty("bidp_rsqn8")
        private String bidpRsqn8; // 매수호가잔량8

        @JsonProperty("bidp_rsqn9")
        private String bidpRsqn9; // 매수호가잔량9

        @JsonProperty("bidp_rsqn10")
        private String bidpRsqn10; // 매수호가잔량10

        @JsonProperty("total_askp_rsqn")
        private String totalAskpRsqn; // 총매도호가잔량

        @JsonProperty("total_bidp_rsqn")
        private String totalBidpRsqn; // 총매수호가잔량
    }

    @Data
    public static class Output2 {
        // 예상체결 정보
        @JsonProperty("stck_prpr")
        private String stckPrpr; // 주식현재가

        @JsonProperty("prdy_vrss")
        private String prdyVrss; // 전일대비

        @JsonProperty("prdy_vrss_sign")
        private String prdyVrssSign; // 전일대비부호

        @JsonProperty("prdy_ctrt")
        private String prdyCtrt; // 전일대비율

        @JsonProperty("acml_vol")
        private String acmlVol; // 누적거래량
    }
}
