package com.kis.sample.service;

import com.kis.sample.model.DailyPriceResponse;
import com.kis.sample.model.MeanReversionSignal;
import com.kis.sample.model.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 평균 회귀 (Mean Reversion) 전략 서비스
 * 볼린저 밴드를 활용한 매매 시그널 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeanReversionService {

    private final KisDataServiceCached kisDataService;

    // 볼린저 밴드 설정
    private static final int BB_PERIOD = 20;  // 이동평균 기간
    private static final double BB_STD_DEV = 2.0;  // 표준편차 배수

    /**
     * 평균 회귀 전략 시그널 생성
     *
     * @param stockCode 종목 코드
     * @param stockName 종목 이름
     * @return 매매 시그널
     */
    public MeanReversionSignal generateSignal(String stockCode, String stockName) {
        try {
            // 1. 일봉 데이터 조회 (과거 데이터로 볼린저 밴드 계산)
            DailyPriceResponse dailyPrices = kisDataService.getDailyPrices(stockCode, "D");
            List<DailyPriceResponse.DailyPrice> priceList = dailyPrices.getOutput2();

            if (priceList == null || priceList.size() < BB_PERIOD) {
                throw new RuntimeException("Not enough data to calculate Bollinger Bands");
            }

            // 2. 현재가 조회
            StockPriceResponse currentPriceResponse = kisDataService.getCurrentPrice(stockCode);
            double currentPrice = Double.parseDouble(currentPriceResponse.getOutput().getCurrentPrice());

            // 3. 볼린저 밴드 계산
            BollingerBands bands = calculateBollingerBands(priceList);

            // 4. 매매 시그널 생성
            MeanReversionSignal signal = MeanReversionSignal.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .currentPrice(currentPrice)
                .upperBand(bands.upper)
                .middleBand(bands.middle)
                .lowerBand(bands.lower)
                .stdDev(bands.stdDev)
                .bandWidth(calculateBandWidth(bands))
                .build();

            // 5. 매매 시그널 결정
            determineSignal(signal);

            log.info("Generated signal for {}: {}", stockName, signal.getSignal());
            return signal;

        } catch (Exception e) {
            log.error("Error generating signal for stock: {}", stockCode, e);
            throw new RuntimeException("Failed to generate signal", e);
        }
    }

    /**
     * 볼린저 밴드 계산
     */
    private BollingerBands calculateBollingerBands(List<DailyPriceResponse.DailyPrice> priceList) {
        // 최근 BB_PERIOD 일의 종가 추출
        double[] closePrices = priceList.stream()
            .limit(BB_PERIOD)
            .mapToDouble(p -> Double.parseDouble(p.getClosePrice()))
            .toArray();

        // 이동평균 계산
        double sum = 0;
        for (double price : closePrices) {
            sum += price;
        }
        double middle = sum / BB_PERIOD;

        // 표준편차 계산
        double variance = 0;
        for (double price : closePrices) {
            variance += Math.pow(price - middle, 2);
        }
        double stdDev = Math.sqrt(variance / BB_PERIOD);

        // 상단/하단 밴드 계산
        double upper = middle + (BB_STD_DEV * stdDev);
        double lower = middle - (BB_STD_DEV * stdDev);

        return new BollingerBands(upper, middle, lower, stdDev);
    }

    /**
     * 밴드 폭 계산 (변동성 지표)
     */
    private double calculateBandWidth(BollingerBands bands) {
        return (bands.upper - bands.lower) / bands.middle * 100;
    }

    /**
     * 매매 시그널 결정
     * - 현재가가 하단 밴드 아래: 매수 (과매도)
     * - 현재가가 상단 밴드 위: 매도 (과매수)
     * - 밴드 내부: 보유
     */
    private void determineSignal(MeanReversionSignal signal) {
        double currentPrice = signal.getCurrentPrice();
        double upperBand = signal.getUpperBand();
        double lowerBand = signal.getLowerBand();
        double middleBand = signal.getMiddleBand();

        // 하단 밴드 이탈 (과매도 상태)
        if (currentPrice < lowerBand) {
            signal.setSignal(MeanReversionSignal.SignalType.BUY);
            double deviation = ((lowerBand - currentPrice) / lowerBand) * 100;
            signal.setSignalReason(String.format(
                "과매도 상태: 현재가(%.0f)가 하단밴드(%.0f)보다 %.2f%% 낮음",
                currentPrice, lowerBand, deviation
            ));
        }
        // 상단 밴드 이탈 (과매수 상태)
        else if (currentPrice > upperBand) {
            signal.setSignal(MeanReversionSignal.SignalType.SELL);
            double deviation = ((currentPrice - upperBand) / upperBand) * 100;
            signal.setSignalReason(String.format(
                "과매수 상태: 현재가(%.0f)가 상단밴드(%.0f)보다 %.2f%% 높음",
                currentPrice, upperBand, deviation
            ));
        }
        // 밴드 내부
        else {
            signal.setSignal(MeanReversionSignal.SignalType.HOLD);
            double positionInBand = ((currentPrice - lowerBand) / (upperBand - lowerBand)) * 100;
            signal.setSignalReason(String.format(
                "정상 범위: 밴드 내 %.1f%% 위치 (중간밴드: %.0f)",
                positionInBand, middleBand
            ));
        }
    }

    /**
     * 볼린저 밴드 내부 클래스
     */
    private static class BollingerBands {
        final double upper;
        final double middle;
        final double lower;
        final double stdDev;

        BollingerBands(double upper, double middle, double lower, double stdDev) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
            this.stdDev = stdDev;
        }
    }
}
