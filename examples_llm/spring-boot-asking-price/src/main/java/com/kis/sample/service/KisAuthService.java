package com.kis.sample.service;

import com.kis.sample.config.KisConfig;
import com.kis.sample.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 한국투자증권 인증 서비스 (개선 버전)
 *
 * 주요 개선사항:
 * - 토큰 발급 1분당 1회 제한 준수
 * - 동시 요청 시 Lock으로 중복 발급 방지
 * - 토큰 만료 5분 전 자동 갱신
 * - 마지막 발급 시간 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisAuthService {

    private final KisConfig kisConfig;
    private final RestTemplate restTemplate;

    private String accessToken;
    private LocalDateTime tokenExpiredTime;
    private LocalDateTime lastTokenIssuedTime;

    // 토큰 발급 제한: 1분당 1회
    private static final long MIN_TOKEN_ISSUE_INTERVAL_SECONDS = 60;

    // 토큰 만료 전 갱신 시간: 5분
    private static final long TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 5;

    // 동시 요청 방지를 위한 Lock
    private final ReentrantLock tokenLock = new ReentrantLock();

    /**
     * 접근 토큰 발급
     * - 유효한 토큰이 있으면 재사용
     * - 만료 임박 시 자동 갱신
     * - 1분당 1회 제한 준수
     *
     * @return 접근 토큰
     */
    public String getAccessToken() {
        // 토큰이 유효하고 만료까지 충분한 시간이 남았으면 기존 토큰 반환
        if (isTokenValid()) {
            log.debug("Using existing token. Expires at: {}", tokenExpiredTime);
            return accessToken;
        }

        // Lock을 사용하여 동시 발급 방지
        tokenLock.lock();
        try {
            // Double-check: Lock 획득 후 다시 확인 (다른 스레드가 이미 발급했을 수 있음)
            if (isTokenValid()) {
                log.debug("Token already issued by another thread");
                return accessToken;
            }

            // 1분 제한 확인
            if (lastTokenIssuedTime != null) {
                long secondsSinceLastIssue = Duration.between(lastTokenIssuedTime, LocalDateTime.now()).getSeconds();

                if (secondsSinceLastIssue < MIN_TOKEN_ISSUE_INTERVAL_SECONDS) {
                    long waitSeconds = MIN_TOKEN_ISSUE_INTERVAL_SECONDS - secondsSinceLastIssue;

                    if (accessToken != null) {
                        // 기존 토큰이 있으면 일단 사용
                        log.warn("Token issue rate limit: returning existing token (wait {}s for new token)", waitSeconds);
                        return accessToken;
                    } else {
                        // 기존 토큰이 없으면 대기
                        log.warn("Token issue rate limit: waiting {}s before issuing new token", waitSeconds);
                        try {
                            Thread.sleep(waitSeconds * 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Token issue wait interrupted", e);
                        }
                    }
                }
            }

            // 새 토큰 발급
            return issueNewToken();

        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 토큰 유효성 확인
     * - 토큰이 존재하는지
     * - 만료 시간이 충분히 남았는지 (5분 이상)
     *
     * @return 토큰 유효 여부
     */
    private boolean isTokenValid() {
        if (accessToken == null || tokenExpiredTime == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refreshThreshold = tokenExpiredTime.minusMinutes(TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES);

        // 현재 시간이 갱신 임계값보다 이전이면 유효
        boolean valid = now.isBefore(refreshThreshold);

        if (!valid) {
            log.info("Token will expire soon. Expires at: {}, refresh threshold: {}",
                tokenExpiredTime, refreshThreshold);
        }

        return valid;
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
                this.lastTokenIssuedTime = LocalDateTime.now();

                // 토큰 만료 시간 저장 (yyyy-MM-dd HH:mm:ss 형식)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                this.tokenExpiredTime = LocalDateTime.parse(
                    tokenResponse.getAccessTokenTokenExpired(),
                    formatter
                );

                long validMinutes = Duration.between(LocalDateTime.now(), tokenExpiredTime).toMinutes();
                log.info("✓ New token issued successfully. Valid for {} minutes (expires at: {})",
                    validMinutes, tokenExpiredTime);

                return this.accessToken;
            } else {
                throw new RuntimeException("Failed to get access token. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("✗ Error while getting access token: {}", e.getMessage());
            throw new RuntimeException("Failed to get access token: " + e.getMessage(), e);
        }
    }

    /**
     * 토큰 강제 갱신
     * 주의: 1분 제한을 고려하여 사용
     */
    public void refreshToken() {
        tokenLock.lock();
        try {
            log.info("Forcing token refresh...");

            // 마지막 발급 시간 확인
            if (lastTokenIssuedTime != null) {
                long secondsSinceLastIssue = Duration.between(lastTokenIssuedTime, LocalDateTime.now()).getSeconds();

                if (secondsSinceLastIssue < MIN_TOKEN_ISSUE_INTERVAL_SECONDS) {
                    long waitSeconds = MIN_TOKEN_ISSUE_INTERVAL_SECONDS - secondsSinceLastIssue;
                    log.warn("Cannot refresh token yet. Wait {}s (1min rate limit)", waitSeconds);
                    return;
                }
            }

            this.accessToken = null;
            this.tokenExpiredTime = null;
            getAccessToken();
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 토큰 정보 조회 (디버깅용)
     */
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("hasToken", accessToken != null);
        info.put("tokenExpiredTime", tokenExpiredTime);
        info.put("lastTokenIssuedTime", lastTokenIssuedTime);

        if (tokenExpiredTime != null) {
            long remainingMinutes = Duration.between(LocalDateTime.now(), tokenExpiredTime).toMinutes();
            info.put("remainingMinutes", remainingMinutes);
        }

        if (lastTokenIssuedTime != null) {
            long secondsSinceIssue = Duration.between(lastTokenIssuedTime, LocalDateTime.now()).getSeconds();
            info.put("secondsSinceLastIssue", secondsSinceIssue);
        }

        return info;
    }
}
