package com.kis.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기술적 지표 (볼린저 밴드 + RSI)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicators {

    // 볼린저 밴드
    private BollingerBands bollingerBands;

    // RSI
    private RSI rsi;

    // 복합 시그널
    private CombinedSignal combinedSignal;

    /**
     * 볼린저 밴드
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BollingerBands {
        private double upper;        // 상단 밴드
        private double middle;       // 중간 밴드 (이동평균)
        private double lower;        // 하단 밴드
        private double stdDev;       // 표준편차
        private double bandWidth;    // 밴드 폭
        private int period;          // 계산 기간
        private double multiplier;   // 표준편차 배수
    }

    /**
     * RSI (Relative Strength Index)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RSI {
        private double value;              // RSI 값 (0-100)
        private int period;                // 계산 기간
        private double overboughtLevel;    // 과매수 기준선 (기본 70)
        private double oversoldLevel;      // 과매도 기준선 (기본 30)
        private String status;             // OVERBOUGHT, OVERSOLD, NEUTRAL
    }

    /**
     * 복합 시그널
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedSignal {

        public enum SignalType {
            STRONG_BUY("강력 매수"),      // BB 하단 돌파 + RSI 과매도
            BUY("매수"),                   // BB 하단 돌파 또는 RSI 과매도
            STRONG_SELL("강력 매도"),     // BB 상단 돌파 + RSI 과매수
            SELL("매도"),                  // BB 상단 돌파 또는 RSI 과매수
            HOLD("보유");                  // 정상 범위

            private final String description;

            SignalType(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }

        private SignalType signal;
        private String reason;
        private double confidence;  // 신뢰도 (0-100)
    }
}
