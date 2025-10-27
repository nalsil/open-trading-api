package com.kis.sample.controller;

import com.kis.sample.model.StockPriceResponse;
import com.kis.sample.model.TechnicalIndicators;
import com.kis.sample.service.CandlestickChartService;
import com.kis.sample.service.KisDataServiceCached;
import com.kis.sample.service.TechnicalAnalysisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 기술적 분석 컨트롤러 (볼린저 밴드 + RSI)
 */
@Slf4j
@RestController
@RequestMapping("/api/technical-analysis")
@RequiredArgsConstructor
public class TechnicalAnalysisController {

    private final TechnicalAnalysisService technicalAnalysisService;
    private final CandlestickChartService candlestickChartService;
    private final KisDataServiceCached kisDataService;

    /**
     * 기술적 지표 분석
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(@RequestBody AnalysisRequest request) {
        try {
            log.info("Analyzing stock: {} with parameters: {}", request.getStockCode(), request);

            // 현재가 조회
            StockPriceResponse priceResponse = kisDataService.getCurrentPrice(request.getStockCode());
            double currentPrice = Double.parseDouble(priceResponse.getOutput().getCurrentPrice());

            // 기술적 지표 분석
            TechnicalIndicators indicators = technicalAnalysisService.analyze(
                request.getStockCode(),
                request.getBbPeriod(),
                request.getBbMultiplier(),
                request.getRsiPeriod(),
                request.getRsiOverbought(),
                request.getRsiOversold()
            );

            AnalysisResult result = AnalysisResult.builder()
                .stockCode(request.getStockCode())
                .stockName(request.getStockName())
                .currentPrice(currentPrice)
                .indicators(indicators)
                .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error analyzing stock", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 캔들스틱 차트 생성
     */
    @PostMapping("/chart")
    public ResponseEntity<Resource> generateChart(@RequestBody ChartRequest request) {
        try {
            log.info("Generating chart for stock: {}", request.getStockCode());

            // 현재가 조회
            StockPriceResponse priceResponse = kisDataService.getCurrentPrice(request.getStockCode());
            double currentPrice = Double.parseDouble(priceResponse.getOutput().getCurrentPrice());

            // 기술적 지표 분석
            TechnicalIndicators indicators = technicalAnalysisService.analyze(
                request.getStockCode(),
                request.getBbPeriod(),
                request.getBbMultiplier(),
                request.getRsiPeriod(),
                request.getRsiOverbought(),
                request.getRsiOversold()
            );

            // 차트 파일 경로
            Path chartDir = Paths.get(System.getProperty("user.home"), "kis-charts");
            Files.createDirectories(chartDir);

            String fileName = String.format("technical_%s_%s.png",
                request.getStockCode(),
                System.currentTimeMillis());
            String filePath = chartDir.resolve(fileName).toString();

            // 차트 생성
            candlestickChartService.createTechnicalChart(
                request.getStockCode(),
                request.getStockName(),
                indicators,
                currentPrice,
                filePath,
                request.getDisplayDays()
            );

            // 파일 리소스 반환
            File chartFile = new File(filePath);
            Resource resource = new FileSystemResource(chartFile);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);

        } catch (Exception e) {
            log.error("Error generating chart", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 분석 요청
     */
    @Data
    public static class AnalysisRequest {
        private String stockCode;
        private String stockName = "종목";

        // 볼린저 밴드 파라미터
        private int bbPeriod = 20;
        private double bbMultiplier = 2.0;

        // RSI 파라미터
        private int rsiPeriod = 14;
        private double rsiOverbought = 70.0;
        private double rsiOversold = 30.0;
    }

    /**
     * 차트 생성 요청
     */
    @Data
    public static class ChartRequest {
        private String stockCode;
        private String stockName = "종목";
        private int displayDays = 60;

        // 볼린저 밴드 파라미터
        private int bbPeriod = 20;
        private double bbMultiplier = 2.0;

        // RSI 파라미터
        private int rsiPeriod = 14;
        private double rsiOverbought = 70.0;
        private double rsiOversold = 30.0;
    }

    /**
     * 분석 결과
     */
    @Data
    @lombok.Builder
    public static class AnalysisResult {
        private String stockCode;
        private String stockName;
        private double currentPrice;
        private TechnicalIndicators indicators;
    }
}
