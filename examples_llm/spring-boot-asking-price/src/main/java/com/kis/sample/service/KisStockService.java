package com.kis.sample.service;

import com.kis.sample.config.KisConfig;
import com.kis.sample.model.AskingPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockService {

    private final KisConfig kisConfig;
    private final KisAuthService kisAuthService;
    private final RestTemplate restTemplate;

    /**
     * 주식 현재가 호가/예상체결 조회
     *
     * @param stockCode        종목코드 (예: 005930 - 삼성전자)
     * @param marketDivCode    시장 구분 코드 (J:KRX, NX:NXT, UN:통합)
     * @return 호가 정보
     */
    public AskingPriceResponse getAskingPrice(String stockCode, String marketDivCode) {
        // API URL
        String apiUrl = "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn";
        String url = kisConfig.getBaseUrl() + apiUrl;

        // TR_ID 설정
        String trId = "FHKST01010200";  // 실전/모의 동일

        // 헤더 설정
        HttpHeaders headers = createHeaders(trId);

        // 쿼리 파라미터 설정
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("FID_COND_MRKT_DIV_CODE", marketDivCode)
            .queryParam("FID_INPUT_ISCD", stockCode)
            .build()
            .encode()
            .toUri();

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            log.info("Requesting asking price for stock: {}, market: {}", stockCode, marketDivCode);
            ResponseEntity<AskingPriceResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                AskingPriceResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AskingPriceResponse askingPriceResponse = response.getBody();

                // API 응답 코드 확인
                if ("0".equals(askingPriceResponse.getRtCd())) {
                    log.info("Successfully retrieved asking price for stock: {}", stockCode);
                    return askingPriceResponse;
                } else {
                    log.error("API Error - rt_cd: {}, msg_cd: {}, msg1: {}",
                        askingPriceResponse.getRtCd(),
                        askingPriceResponse.getMsgCd(),
                        askingPriceResponse.getMsg1());
                    throw new RuntimeException("API Error: " + askingPriceResponse.getMsg1());
                }
            } else {
                throw new RuntimeException("Failed to get asking price. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error while getting asking price for stock: {}", stockCode, e);
            throw new RuntimeException("Failed to get asking price", e);
        }
    }

    /**
     * API 호출을 위한 공통 헤더 생성
     *
     * @param trId TR ID
     * @return HTTP 헤더
     */
    private HttpHeaders createHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + kisAuthService.getAccessToken());
        headers.set("appkey", kisConfig.getAppKey());
        headers.set("appsecret", kisConfig.getAppSecret());
        headers.set("tr_id", trId);
        headers.set("custtype", "P");  // 개인

        return headers;
    }
}
