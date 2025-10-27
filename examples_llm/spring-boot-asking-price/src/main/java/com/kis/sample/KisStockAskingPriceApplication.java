package com.kis.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 한국투자증권 API를 이용한 주식 호가 조회 샘플 애플리케이션
 *
 * 이 애플리케이션은 한국투자증권의 Open Trading API를 사용하여
 * 국내 주식의 실시간 호가 정보를 조회하는 샘플 프로그램입니다.
 *
 * 주요 기능:
 * - OAuth2 토큰 발급 및 관리
 * - 주식 현재가 호가/예상체결 조회
 * - RESTful API 제공
 *
 * API 엔드포인트:
 * - GET /api/stock/asking-price/{stockCode} - 전체 호가 정보 조회
 * - GET /api/stock/asking-price/{stockCode}/simple - 주요 호가 정보 조회
 *
 * 사용 예시:
 * curl http://localhost:8080/api/stock/asking-price/005930
 * curl http://localhost:8080/api/stock/asking-price/005930/simple
 *
 * @author KIS Sample
 * @version 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
public class KisStockAskingPriceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KisStockAskingPriceApplication.class, args);
    }
}
