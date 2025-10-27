package com.kis.sample.service;

import com.kis.sample.config.KisConfig;
import com.kis.sample.model.DailyPriceResponse;
import com.kis.sample.model.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 한국투자증권 API - 캐싱 및 Rate Limiting이 적용된 데이터 조회 서비스
 *
 * 주요 기능:
 * - 일봉 데이터 캐싱 (5분)
 * - 현재가 캐싱 (10초)
 * - Rate Limiting (1초당 최대 20건)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisDataServiceCached {

    private final KisConfig kisConfig;
    private final KisAuthService kisAuthService;
    private final RestTemplate restTemplate;

    // Rate Limiting을 위한 요청 시간 추적
    private final ConcurrentHashMap<String, LocalDateTime> lastRequestTime = new ConcurrentHashMap<>();
    private static final long MIN_REQUEST_INTERVAL_MS = 100; // 0.1초 (초당 10건 제한)

    /**
     * 주식 현재가 시세 조회 (캐싱 적용)
     * 10초간 캐싱
     *
     * @param stockCode 종목코드
     * @return 현재가 정보
     */
    @Cacheable(value = "currentPrices", key = "#stockCode")
    public StockPriceResponse getCurrentPrice(String stockCode) {
        waitForRateLimit("getCurrentPrice:" + stockCode);

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
            log.info("API Call: Requesting current price for stock: {}", stockCode);
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
     * 주식 일봉 차트 조회 (캐싱 적용)
     * 5분간 캐싱
     *
     * @param stockCode 종목코드
     * @param period 조회 기간 (D: 일, W: 주, M: 월)
     * @return 일봉 데이터
     */
    @Cacheable(value = "dailyPrices", key = "#stockCode + '_' + #period")
    public DailyPriceResponse getDailyPrices(String stockCode, String period) {
        waitForRateLimit("getDailyPrices:" + stockCode);

        String apiUrl = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        String url = kisConfig.getBaseUrl() + apiUrl;
        String trId = "FHKST03010100";

        HttpHeaders headers = createHeaders(trId);

        // 조회 시작일: 100일 전 (충분한 데이터 확보)
        String endDate = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String startDate = java.time.LocalDate.now().minusDays(100)
            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

        URI uri = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", stockCode)
            .queryParam("FID_INPUT_DATE_1", startDate)
            .queryParam("FID_INPUT_DATE_2", endDate)
            .queryParam("FID_PERIOD_DIV_CODE", period)
            .queryParam("FID_ORG_ADJ_PRC", "0")  // 수정주가 반영
            .build()
            .encode()
            .toUri();

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            log.info("API Call: Requesting daily prices for stock: {}, period: {}, startDate: {}, endDate: {}",
                stockCode, period, startDate, endDate);

            ResponseEntity<DailyPriceResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                DailyPriceResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                DailyPriceResponse dailyPriceResponse = response.getBody();

                if ("0".equals(dailyPriceResponse.getRtCd())) {
                    log.info("Successfully retrieved {} daily prices for stock: {}",
                        dailyPriceResponse.getOutput2() != null ? dailyPriceResponse.getOutput2().size() : 0,
                        stockCode);
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
     * Rate Limiting 적용
     * 같은 요청이 짧은 시간 내에 반복되는 것을 방지
     *
     * @param requestKey 요청 키
     */
    private void waitForRateLimit(String requestKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastRequestTime.get(requestKey);

        if (lastTime != null) {
            long elapsedMs = java.time.Duration.between(lastTime, now).toMillis();

            if (elapsedMs < MIN_REQUEST_INTERVAL_MS) {
                long waitMs = MIN_REQUEST_INTERVAL_MS - elapsedMs;
                try {
                    log.debug("Rate limiting: waiting {}ms for key: {}", waitMs, requestKey);
                    TimeUnit.MILLISECONDS.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Rate limit wait interrupted", e);
                }
            }
        }

        lastRequestTime.put(requestKey, LocalDateTime.now());
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
