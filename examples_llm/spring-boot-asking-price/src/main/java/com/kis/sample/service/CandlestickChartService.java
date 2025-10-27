package com.kis.sample.service;

import com.kis.sample.model.DailyPriceResponse;
import com.kis.sample.model.TechnicalIndicators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 캔들스틱 차트 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandlestickChartService {

    private final KisDataServiceCached kisDataService;

    /**
     * 기술적 분석 캔들스틱 차트 생성
     */
    public void createTechnicalChart(
        String stockCode,
        String stockName,
        TechnicalIndicators indicators,
        double currentPrice,
        String outputPath,
        int displayDays
    ) {
        try {
            // 일봉 데이터 조회
            DailyPriceResponse dailyPrices = kisDataService.getDailyPrices(stockCode, "D");
            List<DailyPriceResponse.DailyPrice> priceList = dailyPrices.getOutput2();

            // 차트 생성
            JFreeChart chart = createCombinedChart(
                stockCode, stockName, priceList, indicators, currentPrice, displayDays
            );

            // 차트 저장
            File chartFile = new File(outputPath);
            ChartUtils.saveChartAsPNG(chartFile, chart, 1400, 800);

            log.info("Candlestick chart saved to: {}", outputPath);

        } catch (IOException e) {
            log.error("Error creating candlestick chart", e);
            throw new RuntimeException("Failed to create chart", e);
        }
    }

    /**
     * 결합 차트 생성 (가격 + 볼린저 밴드 + RSI)
     */
    private JFreeChart createCombinedChart(
        String stockCode,
        String stockName,
        List<DailyPriceResponse.DailyPrice> priceList,
        TechnicalIndicators indicators,
        double currentPrice,
        int displayDays
    ) {
        // 상위 플롯: 캔들스틱 + 볼린저 밴드
        XYPlot pricePlot = createPricePlot(priceList, indicators, displayDays);

        // 하위 플롯: RSI
        XYPlot rsiPlot = createRSIPlot(priceList, indicators, displayDays);

        // 결합 플롯
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new DateAxis("날짜"));
        combinedPlot.add(pricePlot, 3);  // 가격 차트는 3배 높이
        combinedPlot.add(rsiPlot, 1);    // RSI 차트는 1배 높이

        // 차트 생성
        JFreeChart chart = new JFreeChart(
            stockName + " (" + stockCode + ") - 기술적 분석",
            JFreeChart.DEFAULT_TITLE_FONT,
            combinedPlot,
            true
        );

        chart.setBackgroundPaint(Color.WHITE);

        return chart;
    }

    /**
     * 가격 플롯 생성 (캔들스틱 + 볼린저 밴드)
     */
    private XYPlot createPricePlot(
        List<DailyPriceResponse.DailyPrice> priceList,
        TechnicalIndicators indicators,
        int displayDays
    ) {
        int count = Math.min(displayDays, priceList.size());

        // 캔들스틱 데이터
        Date[] dates = new Date[count];
        double[] high = new double[count];
        double[] low = new double[count];
        double[] open = new double[count];
        double[] close = new double[count];
        double[] volume = new double[count];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        for (int i = 0; i < count; i++) {
            DailyPriceResponse.DailyPrice price = priceList.get(count - 1 - i);

            try {
                dates[i] = sdf.parse(price.getDate());
            } catch (ParseException e) {
                dates[i] = new Date();
            }

            high[i] = Double.parseDouble(price.getHighPrice());
            low[i] = Double.parseDouble(price.getLowPrice());
            open[i] = Double.parseDouble(price.getOpenPrice());
            close[i] = Double.parseDouble(price.getClosePrice());
            volume[i] = Double.parseDouble(price.getVolume());
        }

        // 캔들스틱 데이터셋
        DefaultHighLowDataset candlestickDataset = new DefaultHighLowDataset(
            "가격", dates, high, low, open, close, volume
        );

        // 볼린저 밴드 데이터셋
        XYSeriesCollection bbDataset = createBollingerBandsDataset(
            dates, indicators.getBollingerBands()
        );

        // 가격 축
        NumberAxis priceAxis = new NumberAxis("가격 (원)");
        priceAxis.setAutoRangeIncludesZero(false);

        // 캔들스틱 렌더러
        CandlestickRenderer candlestickRenderer = new CandlestickRenderer();
        candlestickRenderer.setUpPaint(Color.RED);      // 상승
        candlestickRenderer.setDownPaint(Color.BLUE);   // 하락
        candlestickRenderer.setDrawVolume(false);

        // 플롯 생성
        XYPlot plot = new XYPlot(candlestickDataset, null, priceAxis, candlestickRenderer);

        // 볼린저 밴드 추가
        XYLineAndShapeRenderer bbRenderer = new XYLineAndShapeRenderer();
        bbRenderer.setSeriesPaint(0, new Color(255, 0, 0, 100));     // 상단 (반투명 빨강)
        bbRenderer.setSeriesPaint(1, new Color(0, 128, 0, 150));     // 중간 (반투명 초록)
        bbRenderer.setSeriesPaint(2, new Color(255, 0, 0, 100));     // 하단 (반투명 빨강)
        bbRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f));
        bbRenderer.setSeriesStroke(1, new BasicStroke(2.0f));
        bbRenderer.setSeriesStroke(2, new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {5.0f}, 0.0f));
        bbRenderer.setSeriesShapesVisible(0, false);
        bbRenderer.setSeriesShapesVisible(1, false);
        bbRenderer.setSeriesShapesVisible(2, false);

        plot.setDataset(1, bbDataset);
        plot.setRenderer(1, bbRenderer);

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        return plot;
    }

    /**
     * RSI 플롯 생성
     */
    private XYPlot createRSIPlot(
        List<DailyPriceResponse.DailyPrice> priceList,
        TechnicalIndicators indicators,
        int displayDays
    ) {
        int count = Math.min(displayDays, priceList.size());

        XYSeries rsiSeries = new XYSeries("RSI");
        XYSeries overboughtSeries = new XYSeries("과매수선");
        XYSeries oversoldSeries = new XYSeries("과매도선");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        for (int i = 0; i < count; i++) {
            DailyPriceResponse.DailyPrice price = priceList.get(count - 1 - i);

            try {
                Date date = sdf.parse(price.getDate());
                long time = date.getTime();

                // 최신 데이터에만 RSI 값 표시
                if (i == count - 1) {
                    rsiSeries.add(time, indicators.getRsi().getValue());
                }

                // 기준선
                overboughtSeries.add(time, indicators.getRsi().getOverboughtLevel());
                oversoldSeries.add(time, indicators.getRsi().getOversoldLevel());
            } catch (ParseException e) {
                log.error("Error parsing date", e);
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(rsiSeries);
        dataset.addSeries(overboughtSeries);
        dataset.addSeries(oversoldSeries);

        NumberAxis rsiAxis = new NumberAxis("RSI");
        rsiAxis.setRange(0, 100);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.MAGENTA);
        renderer.setSeriesPaint(1, Color.RED);
        renderer.setSeriesPaint(2, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {3.0f}, 0.0f));
        renderer.setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {3.0f}, 0.0f));

        XYPlot plot = new XYPlot(dataset, null, rsiAxis, renderer);
        plot.setBackgroundPaint(new Color(240, 240, 240));
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        return plot;
    }

    /**
     * 볼린저 밴드 데이터셋 생성
     */
    private XYSeriesCollection createBollingerBandsDataset(
        Date[] dates,
        TechnicalIndicators.BollingerBands bb
    ) {
        XYSeries upperSeries = new XYSeries("상단 밴드");
        XYSeries middleSeries = new XYSeries("중간 밴드");
        XYSeries lowerSeries = new XYSeries("하단 밴드");

        for (Date date : dates) {
            long time = date.getTime();
            upperSeries.add(time, bb.getUpper());
            middleSeries.add(time, bb.getMiddle());
            lowerSeries.add(time, bb.getLower());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(upperSeries);
        dataset.addSeries(middleSeries);
        dataset.addSeries(lowerSeries);

        return dataset;
    }
}
