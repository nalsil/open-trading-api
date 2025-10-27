package com.kis.sample.service;

import com.kis.sample.model.DailyPriceResponse;
import com.kis.sample.model.StockPriceResponse;
import com.kis.sample.model.TechnicalIndicators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기술적 분석 서비스 (볼린저 밴드 + RSI)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalAnalysisService {

    private final KisDataServiceCached kisDataService;

    /**
     * 기술적 지표 분석
     *
     * @param stockCode 종목 코드
     * @param bbPeriod 볼린저 밴드 기간
     * @param bbMultiplier 볼린저 밴드 표준편차 배수
     * @param rsiPeriod RSI 기간
     * @param rsiOverbought RSI 과매수 기준
     * @param rsiOversold RSI 과매도 기준
     * @return 기술적 지표
     */
    public TechnicalIndicators analyze(
        String stockCode,
        int bbPeriod,
        double bbMultiplier,
        int rsiPeriod,
        double rsiOverbought,
        double rsiOversold
    ) {
        try {
            // 일봉 데이터 조회
            DailyPriceResponse dailyPrices = kisDataService.getDailyPrices(stockCode, "D");
            List<DailyPriceResponse.DailyPrice> priceList = dailyPrices.getOutput2();

            if (priceList == null || priceList.size() < Math.max(bbPeriod, rsiPeriod + 1)) {
                throw new RuntimeException("Not enough data for technical analysis");
            }

            // 현재가 조회
            StockPriceResponse currentPriceResponse = kisDataService.getCurrentPrice(stockCode);
            double currentPrice = Double.parseDouble(currentPriceResponse.getOutput().getCurrentPrice());

            // 볼린저 밴드 계산
            TechnicalIndicators.BollingerBands bb = calculateBollingerBands(
                priceList, currentPrice, bbPeriod, bbMultiplier
            );

            // RSI 계산
            TechnicalIndicators.RSI rsi = calculateRSI(
                priceList, rsiPeriod, rsiOverbought, rsiOversold
            );

            // 복합 시그널 생성
            TechnicalIndicators.CombinedSignal signal = generateCombinedSignal(
                currentPrice, bb, rsi
            );

            return TechnicalIndicators.builder()
                .bollingerBands(bb)
                .rsi(rsi)
                .combinedSignal(signal)
                .build();

        } catch (Exception e) {
            log.error("Error analyzing stock: {}", stockCode, e);
            throw new RuntimeException("Failed to analyze stock", e);
        }
    }

    /**
     * 볼린저 밴드 계산
     */
    private TechnicalIndicators.BollingerBands calculateBollingerBands(
        List<DailyPriceResponse.DailyPrice> priceList,
        double currentPrice,
        int period,
        double multiplier
    ) {
        // 최근 period 일의 종가 추출
        double[] closePrices = priceList.stream()
            .limit(period)
            .mapToDouble(p -> Double.parseDouble(p.getClosePrice()))
            .toArray();

        // 이동평균 계산
        double sum = 0;
        for (double price : closePrices) {
            sum += price;
        }
        double middle = sum / period;

        // 표준편차 계산
        double variance = 0;
        for (double price : closePrices) {
            variance += Math.pow(price - middle, 2);
        }
        double stdDev = Math.sqrt(variance / period);

        // 상단/하단 밴드 계산
        double upper = middle + (multiplier * stdDev);
        double lower = middle - (multiplier * stdDev);

        // 밴드 폭 계산
        double bandWidth = (upper - lower) / middle * 100;

        return TechnicalIndicators.BollingerBands.builder()
            .upper(upper)
            .middle(middle)
            .lower(lower)
            .stdDev(stdDev)
            .bandWidth(bandWidth)
            .period(period)
            .multiplier(multiplier)
            .build();
    }

    /**
     * RSI 계산
     */
    private TechnicalIndicators.RSI calculateRSI(
        List<DailyPriceResponse.DailyPrice> priceList,
        int period,
        double overbought,
        double oversold
    ) {
        // 가격 변화 계산
        double avgGain = 0;
        double avgLoss = 0;

        for (int i = 0; i < period; i++) {
            if (i + 1 >= priceList.size()) break;

            double currentClose = Double.parseDouble(priceList.get(i).getClosePrice());
            double previousClose = Double.parseDouble(priceList.get(i + 1).getClosePrice());
            double change = currentClose - previousClose;

            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }

        avgGain /= period;
        avgLoss /= period;

        // RSI 계산
        double rsi;
        if (avgLoss == 0) {
            rsi = 100;
        } else {
            double rs = avgGain / avgLoss;
            rsi = 100 - (100 / (1 + rs));
        }

        // 상태 판단
        String status;
        if (rsi >= overbought) {
            status = "OVERBOUGHT";
        } else if (rsi <= oversold) {
            status = "OVERSOLD";
        } else {
            status = "NEUTRAL";
        }

        return TechnicalIndicators.RSI.builder()
            .value(rsi)
            .period(period)
            .overboughtLevel(overbought)
            .oversoldLevel(oversold)
            .status(status)
            .build();
    }

    /**
     * 복합 시그널 생성 (볼린저 밴드 + RSI)
     */
    private TechnicalIndicators.CombinedSignal generateCombinedSignal(
        double currentPrice,
        TechnicalIndicators.BollingerBands bb,
        TechnicalIndicators.RSI rsi
    ) {
        boolean bbOversold = currentPrice < bb.getLower();
        boolean bbOverbought = currentPrice > bb.getUpper();
        boolean rsiOversold = "OVERSOLD".equals(rsi.getStatus());
        boolean rsiOverbought = "OVERBOUGHT".equals(rsi.getStatus());

        TechnicalIndicators.CombinedSignal.SignalType signal;
        String reason;
        double confidence;

        // 강력 매수: BB 하단 돌파 + RSI 과매도
        if (bbOversold && rsiOversold) {
            signal = TechnicalIndicators.CombinedSignal.SignalType.STRONG_BUY;
            reason = String.format(
                "강력 매수 시그널: 볼린저 밴드 하단(%.0f) 이탈 + RSI 과매도(%.1f)",
                bb.getLower(), rsi.getValue()
            );
            confidence = 90;
        }
        // 강력 매도: BB 상단 돌파 + RSI 과매수
        else if (bbOverbought && rsiOverbought) {
            signal = TechnicalIndicators.CombinedSignal.SignalType.STRONG_SELL;
            reason = String.format(
                "강력 매도 시그널: 볼린저 밴드 상단(%.0f) 이탈 + RSI 과매수(%.1f)",
                bb.getUpper(), rsi.getValue()
            );
            confidence = 90;
        }
        // 매수: BB 하단 돌파 또는 RSI 과매도
        else if (bbOversold || rsiOversold) {
            signal = TechnicalIndicators.CombinedSignal.SignalType.BUY;
            if (bbOversold) {
                reason = String.format("매수 시그널: 볼린저 밴드 하단(%.0f) 이탈", bb.getLower());
            } else {
                reason = String.format("매수 시그널: RSI 과매도(%.1f)", rsi.getValue());
            }
            confidence = 70;
        }
        // 매도: BB 상단 돌파 또는 RSI 과매수
        else if (bbOverbought || rsiOverbought) {
            signal = TechnicalIndicators.CombinedSignal.SignalType.SELL;
            if (bbOverbought) {
                reason = String.format("매도 시그널: 볼린저 밴드 상단(%.0f) 이탈", bb.getUpper());
            } else {
                reason = String.format("매도 시그널: RSI 과매수(%.1f)", rsi.getValue());
            }
            confidence = 70;
        }
        // 보유
        else {
            signal = TechnicalIndicators.CombinedSignal.SignalType.HOLD;
            reason = String.format(
                "보유: 정상 범위 (RSI: %.1f, 밴드 내 위치)",
                rsi.getValue()
            );
            confidence = 50;
        }

        return TechnicalIndicators.CombinedSignal.builder()
            .signal(signal)
            .reason(reason)
            .confidence(confidence)
            .build();
    }
}
