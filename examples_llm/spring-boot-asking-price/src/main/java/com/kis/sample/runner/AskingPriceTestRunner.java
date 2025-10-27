package com.kis.sample.runner;

import com.kis.sample.model.AskingPriceResponse;
import com.kis.sample.service.KisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 호가 조회 테스트를 실행하는 Runner
 * <p>
 * 이 컴포넌트는 Spring Boot가 시작될 때 자동으로 실행되어
 * 삼성전자(005930)의 호가 정보를 조회하고 콘솔에 출력합니다.
 * <p>
 * 활성화/비활성화: application.yml에서 설정
 * kis.test-on-startup: true/false
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AskingPriceTestRunner implements CommandLineRunner {

    private final KisStockService kisStockService;

    @Override
    public void run(String... args) throws Exception {
        // 시작 시 테스트를 원하지 않으면 이 메서드를 주석 처리하거나
        // @Component 어노테이션을 제거하세요

        log.info("========================================");
        log.info("Starting Asking Price Test...");
        log.info("========================================");

        try {
            // 삼성전자(005930) 호가 조회
            String stockCode = "005930";
            String marketDivCode = "J";

            log.info("Querying asking price for stock: {}", stockCode);

            AskingPriceResponse response = kisStockService.getAskingPrice(stockCode, marketDivCode);

            if (response != null && "0".equals(response.getRtCd())) {
                printAskingPriceInfo(stockCode, response);
            } else {
                log.error("Failed to get asking price. Response: {}", response);
            }

        } catch (Exception e) {
            log.error("Error during test", e);
        }

        log.info("========================================");
        log.info("Test completed. Application is ready.");
        log.info("Try: curl http://localhost:8080/api/stock/asking-price/005930");
        log.info("========================================");
    }

    /**
     * 호가 정보를 보기 좋게 출력
     */
    private void printAskingPriceInfo(String stockCode, AskingPriceResponse response) {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║           주식 호가 정보 - 종목코드: {}                      ║", stockCode);
        log.info("╠════════════════════════════════════════════════════════════════╣");

        if (response.getOutput2() != null) {
            AskingPriceResponse.Output2 output2 = response.getOutput2();
            log.info("║ [현재가 정보]                                                  ║");
            log.info("║  - 현재가: {} 원", String.format("%10s", output2.getStckPrpr()));
            log.info("║  - 전일대비: {} 원 ({})",
                    String.format("%10s", output2.getPrdyVrss()),
                    getPriceSignText(output2.getPrdyVrssSign()));
            log.info("║  - 전일대비율: {}%", output2.getPrdyCtrt());
            log.info("║  - 누적거래량: {} 주", String.format("%15s", output2.getAcmlVol()));
            log.info("╠════════════════════════════════════════════════════════════════╣");
        }

        if (response.getOutput1() != null) {
            AskingPriceResponse.Output1 output1 = response.getOutput1();
            log.info("║ [호가 정보]                                                    ║");
            log.info("╠════════════════════════════════════════════════════════════════╣");
            log.info("║    매도호가        잔량          매수호가        잔량          ║");
            log.info("╠════════════════════════════════════════════════════════════════╣");

            printOrderBook(1, output1.getAskp1(), output1.getAskpRsqn1(),
                    output1.getBidp1(), output1.getBidpRsqn1());
            printOrderBook(2, output1.getAskp2(), output1.getAskpRsqn2(),
                    output1.getBidp2(), output1.getBidpRsqn2());
            printOrderBook(3, output1.getAskp3(), output1.getAskpRsqn3(),
                    output1.getBidp3(), output1.getBidpRsqn3());
            printOrderBook(4, output1.getAskp4(), output1.getAskpRsqn4(),
                    output1.getBidp4(), output1.getBidpRsqn4());
            printOrderBook(5, output1.getAskp5(), output1.getAskpRsqn5(),
                    output1.getBidp5(), output1.getBidpRsqn5());

            log.info("╠════════════════════════════════════════════════════════════════╣");
            log.info("║  총 매도호가: {} 주", String.format("%15s", output1.getTotalAskpRsqn()));
            log.info("║  총 매수호가: {} 주", String.format("%15s", output1.getTotalBidpRsqn()));
        }

        log.info("╚════════════════════════════════════════════════════════════════╝");
        log.info("\n");
    }

    /**
     * 호가 라인 출력
     */
    private void printOrderBook(int level, String askPrice, String askVol,
                                String bidPrice, String bidVol) {
        log.info("║ {} │ {} │ {} │ {} │ {} │",
                String.format("%2d", level),
                String.format("%10s", askPrice != null ? askPrice : "0"),
                String.format("%12s", askVol != null ? askVol : "0"),
                String.format("%10s", bidPrice != null ? bidPrice : "0"),
                String.format("%12s", bidVol != null ? bidVol : "0")
        );
    }

    /**
     * 가격 등락 부호를 텍스트로 변환
     */
    private String getPriceSignText(String sign) {
        if (sign == null) return "보합";
        return switch (sign) {
            case "1" -> "상한";
            case "2" -> "상승";
            case "3" -> "보합";
            case "4" -> "하한";
            case "5" -> "하락";
            default -> "알수없음";
        };
    }
}
