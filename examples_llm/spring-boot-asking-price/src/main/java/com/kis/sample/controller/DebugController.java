package com.kis.sample.controller;

import com.kis.sample.service.KisAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 디버깅용 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final KisAuthService kisAuthService;

    /**
     * 토큰 정보 조회
     */
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo() {
        Map<String, Object> info = kisAuthService.getTokenInfo();
        return ResponseEntity.ok(info);
    }

    /**
     * 토큰 강제 갱신
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<String> refreshToken() {
        try {
            kisAuthService.refreshToken();
            return ResponseEntity.ok("Token refresh requested");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Token refresh failed: " + e.getMessage());
        }
    }
}
