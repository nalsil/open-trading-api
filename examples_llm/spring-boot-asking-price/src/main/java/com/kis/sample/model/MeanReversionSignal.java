package com.kis.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 평균 회귀 전략 시그널
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeanReversionSignal {

    /**
     * 시그널 타입
     */
    public enum SignalType {
        BUY("매수"),
        SELL("매도"),
        HOLD("보유");

        private final String description;

        SignalType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private String stockCode;      // 종목 코드
    private String stockName;      // 종목 이름
    private double currentPrice;   // 현재가
    private double upperBand;      // 볼린저 밴드 상단
    private double middleBand;     // 볼린저 밴드 중간(이동평균)
    private double lowerBand;      // 볼린저 밴드 하단
    private double stdDev;         // 표준편차
    private SignalType signal;     // 매매 시그널
    private String signalReason;   // 시그널 발생 이유
    private double bandWidth;      // 밴드 폭 (변동성 지표)
}
