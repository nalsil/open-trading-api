package com.kis.sample.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정
 * 한국투자증권 API 호출 제한을 고려한 캐싱 전략
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 매니저 설정
     * - dailyPrices: 일봉 데이터 (1분 캐싱)
     * - currentPrices: 현재가 (10초 캐싱)
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "dailyPrices",
            "currentPrices",
            "technicalIndicators"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(100)  // 최대 100개 항목
            .expireAfterWrite(1, TimeUnit.MINUTES)  // 1분 후 만료
            .recordStats());  // 통계 기록

        return cacheManager;
    }

    /**
     * 일봉 데이터 전용 캐시
     * 더 긴 캐싱 시간 (5분)
     */
    @Bean
    public Caffeine<Object, Object> dailyPricesCaffeine() {
        return Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats();
    }
}
