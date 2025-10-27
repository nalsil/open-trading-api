package com.kis.sample.service;

import com.kis.sample.config.KisConfig;
import com.kis.sample.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisAuthService {

    private final KisConfig kisConfig;
    private final RestTemplate restTemplate;

    private String accessToken;
    private LocalDateTime tokenExpiredTime;

    /**
     * 접근 토큰 발급
     *
     * @return 접근 토큰
     */
    public String getAccessToken() {
        // 토큰이 유효하면 기존 토큰 반환
        if (accessToken != null && tokenExpiredTime != null
            && LocalDateTime.now().isBefore(tokenExpiredTime)) {
            log.debug("Using existing token. Expires at: {}", tokenExpiredTime);
            return accessToken;
        }

        // 새 토큰 발급
        return issueNewToken();
    }

    /**
     * 새로운 접근 토큰 발급
     *
     * @return 접근 토큰
     */
    private String issueNewToken() {
        String url = kisConfig.getBaseUrl() + "/oauth2/tokenP";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 바디 설정
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credentials");
        requestBody.put("appkey", kisConfig.getAppKey());
        requestBody.put("appsecret", kisConfig.getAppSecret());

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Requesting new token from KIS API...");
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                TokenResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                this.accessToken = tokenResponse.getAccessToken();

                // 토큰 만료 시간 저장 (yyyy-MM-dd HH:mm:ss 형식)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                this.tokenExpiredTime = LocalDateTime.parse(
                    tokenResponse.getAccessTokenTokenExpired(),
                    formatter
                );

                log.info("New token issued successfully. Expires at: {}", tokenExpiredTime);
                return this.accessToken;
            } else {
                throw new RuntimeException("Failed to get access token. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error while getting access token", e);
            throw new RuntimeException("Failed to get access token", e);
        }
    }

    /**
     * 토큰 강제 갱신
     */
    public void refreshToken() {
        log.info("Forcing token refresh...");
        this.accessToken = null;
        this.tokenExpiredTime = null;
        getAccessToken();
    }
}
