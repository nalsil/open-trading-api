package com.kis.sample.controller;

import com.kis.sample.model.AskingPriceResponse;
import com.kis.sample.service.KisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final KisStockService kisStockService;

    /**
     * 주식 호가 조회
     *
     * @param stockCode     종목코드 (예: 005930)
     * @param marketDivCode 시장 구분 코드 (기본값: J - KRX)
     * @return 호가 정보
     */
    @GetMapping("/asking-price/{stockCode}")
    public ResponseEntity<AskingPriceResponse> getAskingPrice(
        @PathVariable String stockCode,
        @RequestParam(defaultValue = "J") String marketDivCode
    ) {
        log.info("GET /api/stock/asking-price/{} - marketDivCode: {}", stockCode, marketDivCode);

        AskingPriceResponse response = kisStockService.getAskingPrice(stockCode, marketDivCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 호가 조회 (심플 버전 - 주요 정보만 반환)
     *
     * @param stockCode     종목코드 (예: 005930)
     * @param marketDivCode 시장 구분 코드 (기본값: J - KRX)
     * @return 주요 호가 정보
     */
    @GetMapping("/asking-price/{stockCode}/simple")
    public ResponseEntity<SimpleAskingPriceResponse> getSimpleAskingPrice(
        @PathVariable String stockCode,
        @RequestParam(defaultValue = "J") String marketDivCode
    ) {
        log.info("GET /api/stock/asking-price/{}/simple - marketDivCode: {}", stockCode, marketDivCode);

        AskingPriceResponse response = kisStockService.getAskingPrice(stockCode, marketDivCode);

        // 간단한 응답으로 변환
        SimpleAskingPriceResponse simpleResponse = new SimpleAskingPriceResponse();
        simpleResponse.setStockCode(stockCode);

        if (response.getOutput1() != null) {
            AskingPriceResponse.Output1 output1 = response.getOutput1();
            simpleResponse.setAskPrice1(output1.getAskp1());
            simpleResponse.setAskPrice5(output1.getAskp5());
            simpleResponse.setBidPrice1(output1.getBidp1());
            simpleResponse.setBidPrice5(output1.getBidp5());
            simpleResponse.setTotalAskVolume(output1.getTotalAskpRsqn());
            simpleResponse.setTotalBidVolume(output1.getTotalBidpRsqn());
        }

        if (response.getOutput2() != null) {
            AskingPriceResponse.Output2 output2 = response.getOutput2();
            simpleResponse.setCurrentPrice(output2.getStckPrpr());
            simpleResponse.setPrevDayDiff(output2.getPrdyVrss());
            simpleResponse.setPrevDayDiffSign(output2.getPrdyVrssSign());
            simpleResponse.setPrevDayDiffRate(output2.getPrdyCtrt());
            simpleResponse.setAccumulatedVolume(output2.getAcmlVol());
        }

        return ResponseEntity.ok(simpleResponse);
    }

    /**
     * 간단한 호가 응답 DTO
     */
    @lombok.Data
    public static class SimpleAskingPriceResponse {
        private String stockCode;           // 종목코드
        private String currentPrice;        // 현재가
        private String prevDayDiff;         // 전일대비
        private String prevDayDiffSign;     // 전일대비부호
        private String prevDayDiffRate;     // 전일대비율
        private String accumulatedVolume;   // 누적거래량

        private String askPrice1;           // 매도호가1
        private String askPrice5;           // 매도호가5
        private String bidPrice1;           // 매수호가1
        private String bidPrice5;           // 매수호가5
        private String totalAskVolume;      // 총매도호가잔량
        private String totalBidVolume;      // 총매수호가잔량
    }
}
