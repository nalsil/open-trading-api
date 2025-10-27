package com.kis.sample.controller;

import com.kis.sample.model.MeanReversionSignal;
import com.kis.sample.service.ChartService;
import com.kis.sample.service.MeanReversionService;
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
 * 평균 회귀 전략 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/strategy/mean-reversion")
@RequiredArgsConstructor
public class MeanReversionController {

    private final MeanReversionService meanReversionService;
    private final ChartService chartService;

    /**
     * 매매 시그널 조회
     *
     * @param stockCode 종목 코드
     * @param stockName 종목 이름
     * @return 매매 시그널
     */
    @GetMapping("/signal")
    public ResponseEntity<MeanReversionSignal> getSignal(
        @RequestParam String stockCode,
        @RequestParam(defaultValue = "종목") String stockName
    ) {
        log.info("Generating mean reversion signal for stock: {} ({})", stockName, stockCode);
        MeanReversionSignal signal = meanReversionService.generateSignal(stockCode, stockName);
        return ResponseEntity.ok(signal);
    }

    /**
     * 차트 생성 및 다운로드
     *
     * @param stockCode 종목 코드
     * @param stockName 종목 이름
     * @return PNG 차트 파일
     */
    @GetMapping("/chart")
    public ResponseEntity<Resource> getChart(
        @RequestParam String stockCode,
        @RequestParam(defaultValue = "종목") String stockName
    ) {
        try {
            log.info("Generating chart for stock: {} ({})", stockName, stockCode);

            // 시그널 생성
            MeanReversionSignal signal = meanReversionService.generateSignal(stockCode, stockName);

            // 차트 파일 경로
            Path chartDir = Paths.get(System.getProperty("user.home"), "kis-charts");
            Files.createDirectories(chartDir);

            String fileName = String.format("mean_reversion_%s_%s.png",
                stockCode,
                System.currentTimeMillis());
            String filePath = chartDir.resolve(fileName).toString();

            // 차트 생성
            chartService.createMeanReversionChart(signal, filePath);

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
     * 시그널 + 차트 URL 반환
     *
     * @param stockCode 종목 코드
     * @param stockName 종목 이름
     * @return 시그널 정보와 차트 다운로드 URL
     */
    @GetMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(
        @RequestParam String stockCode,
        @RequestParam(defaultValue = "종목") String stockName
    ) {
        try {
            log.info("Analyzing stock: {} ({})", stockName, stockCode);

            // 시그널 생성
            MeanReversionSignal signal = meanReversionService.generateSignal(stockCode, stockName);

            // 차트 URL 생성
            String chartUrl = String.format("/api/strategy/mean-reversion/chart?stockCode=%s&stockName=%s",
                stockCode, stockName);

            AnalysisResult result = new AnalysisResult(signal, chartUrl);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error analyzing stock", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 분석 결과 응답 클래스
     */
    public record AnalysisResult(
        MeanReversionSignal signal,
        String chartUrl
    ) {}
}
