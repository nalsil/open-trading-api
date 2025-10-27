package com.kis.sample.service;

import com.kis.sample.config.KisConfig;
import com.kis.sample.model.DailyPriceResponse;
import com.kis.sample.model.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 한국투자증권 API - 데이터 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisDataService {

    private final KisConfig kisConfig;
    private final KisAuthService kisAuthService;
    private final RestTemplate restTemplate;

    /**
     * 주식 현재가 시세 조회
     *
     * @param stockCode 종목코드 (예: 005930 - 삼성전자)
     * @return 현재가 정보
     */
    public StockPriceResponse getCurrentPrice(String stockCode) {
        String apiUrl = "/uapi/domestic-stock/v1/quotations/inquire-price";
        String url = kisConfig.getBaseUrl() + apiUrl;
        String trId = "FHKST01010100";

        HttpHeaders headers = createHeaders(trId);

        URI uri = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", stockCode)
            .build()
            .encode()
            .toUri();

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            log.info("Requesting current price for stock: {}", stockCode);
            ResponseEntity<StockPriceResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                StockPriceResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                StockPriceResponse priceResponse = response.getBody();

                if ("0".equals(priceResponse.getRtCd())) {
                    log.info("Successfully retrieved current price for stock: {}", stockCode);
                    return priceResponse;
                } else {
                    log.error("API Error - rt_cd: {}, msg_cd: {}, msg1: {}",
                        priceResponse.getRtCd(),
                        priceResponse.getMsgCd(),
                        priceResponse.getMsg1());
                    throw new RuntimeException("API Error: " + priceResponse.getMsg1());
                }
            } else {
                throw new RuntimeException("Failed to get current price. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error while getting current price for stock: {}", stockCode, e);
            throw new RuntimeException("Failed to get current price", e);
        }
    }

    /**
     * 주식 일봉 차트 조회
     *
     * @param stockCode 종목코드
     * @param period    조회 기간 (D: 일, W: 주, M: 월)
     * @return 일봉 데이터
     */
    public DailyPriceResponse getDailyPrices(String stockCode, String period) {
        String apiUrl = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        String url = kisConfig.getBaseUrl() + apiUrl;
        String trId = "FHKST03010100";

        HttpHeaders headers = createHeaders(trId);

        // 조회 시작일: 100일 전 (충분한 데이터 확보)
        String endDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String startDate = java.time.LocalDate.now().minusDays(100)
            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

        URI uri = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", stockCode)
            .queryParam("FID_INPUT_DATE_1", startDate)
            .queryParam("FID_INPUT_DATE_2", endDate)
            .queryParam("FID_PERIOD_DIV_CODE", period)
            .queryParam("FID_ORG_ADJ_PRC", "0")  // 수정주가 미반영
            .build()
            .encode()
            .toUri();

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            log.info("Requesting daily prices for stock: {}, period: {}", stockCode, period);
            ResponseEntity<DailyPriceResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                DailyPriceResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                DailyPriceResponse dailyPriceResponse = response.getBody();

                if ("0".equals(dailyPriceResponse.getRtCd())) {
                    log.info("Successfully retrieved daily prices for stock: {}", stockCode);
                    return dailyPriceResponse;
                } else {
                    log.error("API Error - rt_cd: {}, msg_cd: {}, msg1: {}",
                        dailyPriceResponse.getRtCd(),
                        dailyPriceResponse.getMsgCd(),
                        dailyPriceResponse.getMsg1());
                    throw new RuntimeException("API Error: " + dailyPriceResponse.getMsg1());
                }
            } else {
                throw new RuntimeException("Failed to get daily prices. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error while getting daily prices for stock: {}", stockCode, e);
            throw new RuntimeException("Failed to get daily prices", e);
        }
    }

    /**
     * API 호출을 위한 공통 헤더 생성
     */
    private HttpHeaders createHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", kisConfig.getAppKey());
        headers.set("appsecret", kisConfig.getAppSecret());
        headers.set("tr_id", trId);
        headers.set("custtype", "P");

        return headers;
    }
}
