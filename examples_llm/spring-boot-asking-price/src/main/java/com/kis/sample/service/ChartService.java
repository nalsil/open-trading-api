package com.kis.sample.service;

import com.kis.sample.model.DailyPriceResponse;
import com.kis.sample.model.MeanReversionSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 차트 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private final KisDataServiceCached kisDataService;

    /**
     * 평균 회귀 전략 차트 생성
     *
     * @param signal 매매 시그널
     * @param outputPath 저장 경로
     */
    public void createMeanReversionChart(MeanReversionSignal signal, String outputPath) {
        try {
            // 일봉 데이터 조회
            DailyPriceResponse dailyPrices = kisDataService.getDailyPrices(signal.getStockCode(), "D");
            List<DailyPriceResponse.DailyPrice> priceList = dailyPrices.getOutput2();

            // 데이터셋 생성
            XYSeriesCollection dataset = createDataset(priceList, signal);

            // 차트 생성
            JFreeChart chart = ChartFactory.createXYLineChart(
                signal.getStockName() + " - 평균 회귀 전략 (Bollinger Bands)",
                "거래일 (최근 순)",
                "가격 (원)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            );

            // 차트 스타일 설정
            customizeChart(chart, signal);

            // 차트 저장
            File chartFile = new File(outputPath);
            ChartUtils.saveChartAsPNG(chartFile, chart, 1200, 600);

            log.info("Chart saved to: {}", outputPath);

        } catch (IOException e) {
            log.error("Error creating chart", e);
            throw new RuntimeException("Failed to create chart", e);
        }
    }

    /**
     * 데이터셋 생성
     */
    private XYSeriesCollection createDataset(List<DailyPriceResponse.DailyPrice> priceList,
                                            MeanReversionSignal signal) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // 종가 시리즈
        XYSeries priceSeries = new XYSeries("종가");

        // 볼린저 밴드 시리즈
        XYSeries upperBandSeries = new XYSeries("상단 밴드");
        XYSeries middleBandSeries = new XYSeries("중간 밴드 (20일 이평)");
        XYSeries lowerBandSeries = new XYSeries("하단 밴드");

        // 최근 30일 데이터만 표시
        int displayDays = Math.min(30, priceList.size());

        for (int i = 0; i < displayDays; i++) {
            DailyPriceResponse.DailyPrice price = priceList.get(i);
            double closePrice = Double.parseDouble(price.getClosePrice());

            // X축: 역순으로 표시 (0이 가장 최근)
            int xValue = displayDays - i - 1;

            priceSeries.add(xValue, closePrice);

            // 최근 데이터(인덱스 0)에만 볼린저 밴드 표시
            if (i == 0) {
                upperBandSeries.add(xValue, signal.getUpperBand());
                middleBandSeries.add(xValue, signal.getMiddleBand());
                lowerBandSeries.add(xValue, signal.getLowerBand());
            }
        }

        // 볼린저 밴드를 전체 구간에 표시하기 위해 확장
        upperBandSeries.add(0, signal.getUpperBand());
        upperBandSeries.add(displayDays - 1, signal.getUpperBand());

        middleBandSeries.add(0, signal.getMiddleBand());
        middleBandSeries.add(displayDays - 1, signal.getMiddleBand());

        lowerBandSeries.add(0, signal.getLowerBand());
        lowerBandSeries.add(displayDays - 1, signal.getLowerBand());

        dataset.addSeries(priceSeries);
        dataset.addSeries(upperBandSeries);
        dataset.addSeries(middleBandSeries);
        dataset.addSeries(lowerBandSeries);

        return dataset;
    }

    /**
     * 차트 스타일 커스터마이징
     */
    private void customizeChart(JFreeChart chart, MeanReversionSignal signal) {
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // 종가 라인 (파란색, 두께 2)
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);

        // 상단 밴드 (빨간색, 점선)
        renderer.setSeriesPaint(1, Color.RED);
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f));
        renderer.setSeriesShapesVisible(1, false);

        // 중간 밴드 (녹색)
        renderer.setSeriesPaint(2, Color.GREEN);
        renderer.setSeriesStroke(2, new BasicStroke(1.5f));
        renderer.setSeriesShapesVisible(2, false);

        // 하단 밴드 (빨간색, 점선)
        renderer.setSeriesPaint(3, Color.RED);
        renderer.setSeriesStroke(3, new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f));
        renderer.setSeriesShapesVisible(3, false);

        plot.setRenderer(renderer);

        // 배경색 설정
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 시그널 정보 텍스트 추가
        String signalText = String.format(
            "시그널: %s | 현재가: %.0f | 밴드폭: %.2f%% | %s",
            signal.getSignal().getDescription(),
            signal.getCurrentPrice(),
            signal.getBandWidth(),
            signal.getSignalReason()
        );

        XYTextAnnotation annotation = new XYTextAnnotation(signalText, 1,
            signal.getUpperBand() * 1.02);
        annotation.setFont(new Font("SansSerif", Font.BOLD, 12));

        // 시그널에 따른 색상 변경
        switch (signal.getSignal()) {
            case BUY:
                annotation.setPaint(Color.BLUE);
                break;
            case SELL:
                annotation.setPaint(Color.RED);
                break;
            default:
                annotation.setPaint(Color.BLACK);
        }

        plot.addAnnotation(annotation);
    }
}
