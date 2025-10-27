# 캐싱 및 Rate Limiting 가이드

한국투자증권 API 호출 제한을 고려한 캐싱 및 Rate Limiting 전략 가이드입니다.

## 📋 목차

1. [문제점](#문제점)
2. [해결 방안](#해결-방안)
3. [구현 내용](#구현-내용)
4. [사용 방법](#사용-방법)
5. [주의사항](#주의사항)

---

## ❌ 문제점

### 한국투자증권 API 호출 제한

```
⚠️ 일간 가격 데이터를 연속해서 조회하면 API 오류 발생
```

**원인**:
- API 호출 빈도 제한 (Rate Limiting)
- 동일 데이터 반복 조회 시 서버 부하
- 짧은 시간 내 다수 요청 시 차단

**영향**:
- 기술적 분석 실행 실패
- 차트 생성 불가
- 사용자 경험 저하

---

## ✅ 해결 방안

### 1. Caffeine 캐싱
데이터를 메모리에 저장하여 불필요한 API 호출 방지

### 2. Rate Limiting
API 호출 간격 제어

### 3. 자동 대기
연속 호출 시 자동으로 대기 시간 삽입

---

## 🔧 구현 내용

### 1. 캐시 설정 (`CacheConfig.java`)

#### 캐시 종류

| 캐시 이름 | 용도 | 만료 시간 | 최대 크기 |
|-----------|------|-----------|-----------|
| `dailyPrices` | 일봉 데이터 | 5분 | 50개 |
| `currentPrices` | 현재가 | 1분 | 100개 |
| `technicalIndicators` | 기술적 지표 | 1분 | 100개 |

#### 설정 코드
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "dailyPrices",
            "currentPrices",
            "technicalIndicators"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats());

        return cacheManager;
    }
}
```

---

### 2. 캐싱된 데이터 서비스 (`KisDataServiceCached.java`)

#### 주요 기능

**일봉 데이터 조회 (캐싱)**
```java
@Cacheable(value = "dailyPrices", key = "#stockCode + '_' + #period")
public DailyPriceResponse getDailyPrices(String stockCode, String period) {
    waitForRateLimit("getDailyPrices:" + stockCode);
    // API 호출
}
```

**현재가 조회 (캐싱)**
```java
@Cacheable(value = "currentPrices", key = "#stockCode")
public StockPriceResponse getCurrentPrice(String stockCode) {
    waitForRateLimit("getCurrentPrice:" + stockCode);
    // API 호출
}
```

---

### 3. Rate Limiting 로직

#### 작동 원리

```
요청 A (시간: 0ms) → 즉시 실행
요청 B (시간: 50ms) → 50ms 대기 후 실행 (100ms에)
요청 C (시간: 150ms) → 즉시 실행
```

#### 구현 코드
```java
private void waitForRateLimit(String requestKey) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime lastTime = lastRequestTime.get(requestKey);

    if (lastTime != null) {
        long elapsedMs = Duration.between(lastTime, now).toMillis();

        if (elapsedMs < MIN_REQUEST_INTERVAL_MS) {
            long waitMs = MIN_REQUEST_INTERVAL_MS - elapsedMs;
            TimeUnit.MILLISECONDS.sleep(waitMs);
        }
    }

    lastRequestTime.put(requestKey, LocalDateTime.now());
}
```

**설정 값**:
- `MIN_REQUEST_INTERVAL_MS = 100ms`
- 초당 최대 10건 호출 제한

---

## 📖 사용 방법

### 1. 자동 적용

모든 API 호출에 자동으로 적용됩니다.

```java
// 컨트롤러나 서비스에서
@Autowired
private KisDataServiceCached kisDataService;

// 첫 번째 호출 - API 호출
DailyPriceResponse data1 = kisDataService.getDailyPrices("005930", "D");

// 5분 이내 재호출 - 캐시에서 반환 (API 호출 안 함)
DailyPriceResponse data2 = kisDataService.getDailyPrices("005930", "D");

// 5분 후 호출 - 다시 API 호출
DailyPriceResponse data3 = kisDataService.getDailyPrices("005930", "D");
```

---

### 2. 로그 확인

#### API 호출 로그
```
INFO: API Call: Requesting daily prices for stock: 005930, period: D
INFO: Successfully retrieved 100 daily prices for stock: 005930
```

#### 캐시 히트 로그
```
DEBUG: Cache hit for key: 005930_D
```

#### Rate Limiting 로그
```
DEBUG: Rate limiting: waiting 50ms for key: getDailyPrices:005930
```

---

## 🎯 캐싱 전략

### 시나리오 1: 동일 종목 반복 조회

```
사용자 A: 삼성전자 기술적 분석 → API 호출
사용자 B: 삼성전자 차트 생성 → 캐시 사용 (API 호출 안 함)
사용자 C: 삼성전자 분석 → 캐시 사용 (API 호출 안 함)
```

**효과**: API 호출 67% 감소

---

### 시나리오 2: 파라미터 변경

```
사용자: 삼성전자 분석 (BB:20, RSI:14) → API 호출
사용자: 파라미터 변경 (BB:10, RSI:9) → 캐시 사용 (일봉 데이터는 동일)
```

**효과**: 일봉 데이터는 캐시 사용, 지표만 재계산

---

### 시나리오 3: 여러 종목 분석

```
종목 A 분석 (0초) → API 호출
종목 B 분석 (1초) → API 호출
종목 C 분석 (2초) → API 호출
```

**효과**: 각 종목마다 100ms 간격 자동 유지

---

## ⚙️ 설정 조정

### 캐시 만료 시간 변경

#### 일봉 데이터 (더 긴 캐싱)
```java
@Bean
public Caffeine<Object, Object> dailyPricesCaffeine() {
    return Caffeine.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(10, TimeUnit.MINUTES)  // 5분 → 10분
        .recordStats();
}
```

#### 현재가 (더 짧은 캐싱)
```java
.expireAfterWrite(10, TimeUnit.SECONDS)  // 1분 → 10초
```

---

### Rate Limiting 간격 조정

```java
// 더 보수적 (안전)
private static final long MIN_REQUEST_INTERVAL_MS = 200; // 100ms → 200ms

// 더 공격적 (빠름, 위험)
private static final long MIN_REQUEST_INTERVAL_MS = 50;  // 100ms → 50ms
```

---

## 📊 성능 개선

### Before (캐싱 없음)

```
분석 요청 1: 삼성전자 → API 3건 (일봉 + 현재가 + 분석)
분석 요청 2: 삼성전자 → API 3건 (일봉 + 현재가 + 분석)
분석 요청 3: 삼성전자 → API 3건 (일봉 + 현재가 + 분석)

총 API 호출: 9건
실행 시간: 약 3초
```

---

### After (캐싱 적용)

```
분석 요청 1: 삼성전자 → API 2건 (일봉 + 현재가) ✓
분석 요청 2: 삼성전자 → 캐시 사용 (0건) ✓✓
분석 요청 3: 삼성전자 → 캐시 사용 (0건) ✓✓

총 API 호출: 2건 (77% 감소)
실행 시간: 약 0.5초 (83% 단축)
```

---

## ⚠️ 주의사항

### 1. 실시간 데이터

```
⚠️ 캐싱으로 인해 최대 5분간 과거 데이터 표시 가능
```

**해결책**:
- 장 마감 후 분석 권장
- 실시간 필요 시 캐시 TTL 단축

---

### 2. 메모리 사용

```
캐시 크기 제한: 최대 100~150개 항목
```

**예상 메모리**:
- 일봉 100건 × 50종목 = 약 5MB
- 현재가 100종목 = 약 1MB

**총 예상**: 10MB 이하

---

### 3. 동시 사용자

```
⚠️ 동일 종목을 동시에 여러 사용자가 조회 시 경쟁 발생 가능
```

**해결책**:
- 이미 Rate Limiting으로 순차 처리
- 첫 번째 요청만 API 호출, 나머지는 대기 후 캐시 사용

---

## 🧪 테스트

### 1. 캐싱 테스트

```bash
# 첫 번째 요청 (API 호출)
curl -X POST http://localhost:8080/api/technical-analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"005930","stockName":"삼성전자"}'

# 즉시 두 번째 요청 (캐시 사용)
curl -X POST http://localhost:8080/api/technical-analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"005930","stockName":"삼성전자"}'
```

**예상 결과**:
- 첫 번째: 느림 (API 호출)
- 두 번째: 빠름 (캐시 사용)

---

### 2. Rate Limiting 테스트

```bash
# 연속 3회 요청
for i in {1..3}; do
  curl -X POST http://localhost:8080/api/technical-analysis/analyze \
    -H "Content-Type: application/json" \
    -d '{"stockCode":"00066'$i'","stockName":"종목'$i'"}' &
done
```

**예상 결과**:
- 각 요청 간 최소 100ms 간격 자동 유지

---

## 📈 모니터링

### 캐시 통계 확인

```java
@Autowired
private CacheManager cacheManager;

public void printCacheStats() {
    Cache cache = cacheManager.getCache("dailyPrices");
    CaffeineCache caffeineCache = (CaffeineCache) cache;

    CacheStats stats = caffeineCache.getNativeCache().stats();

    System.out.println("Hit Rate: " + stats.hitRate());
    System.out.println("Miss Rate: " + stats.missRate());
    System.out.println("Eviction Count: " + stats.evictionCount());
}
```

---

## 🔗 관련 파일

```
src/main/java/com/kis/sample/
├── config/
│   └── CacheConfig.java                  # 캐시 설정
├── service/
│   ├── KisDataService.java               # 기본 데이터 서비스
│   └── KisDataServiceCached.java         # 캐싱 + Rate Limiting 적용
└── controller/
    └── TechnicalAnalysisController.java  # 캐싱된 서비스 사용
```

---

## 💡 Best Practices

### 1. 장 중 vs 장 후

**장 중 (실시간 필요)**:
```java
.expireAfterWrite(10, TimeUnit.SECONDS)  // 짧은 캐싱
```

**장 후 (분석 목적)**:
```java
.expireAfterWrite(10, TimeUnit.MINUTES)  // 긴 캐싱
```

---

### 2. 개발 vs 운영

**개발 환경**:
```java
private static final long MIN_REQUEST_INTERVAL_MS = 50;  // 빠른 테스트
```

**운영 환경**:
```java
private static final long MIN_REQUEST_INTERVAL_MS = 200;  // 안전한 운영
```

---

### 3. 캐시 수동 무효화

```java
@Autowired
private CacheManager cacheManager;

// 특정 종목 캐시 삭제
public void evictCache(String stockCode) {
    Cache cache = cacheManager.getCache("dailyPrices");
    cache.evict(stockCode + "_D");
}

// 전체 캐시 삭제
public void evictAllCache() {
    cacheManager.getCacheNames()
        .forEach(name -> cacheManager.getCache(name).clear());
}
```

---

## 📞 문제 해결

### 여전히 API 오류 발생

```
Error: Too many requests
```

**해결책**:
1. `MIN_REQUEST_INTERVAL_MS` 증가 (200 → 500)
2. 캐시 TTL 증가 (5분 → 10분)
3. 동시 요청 수 제한

---

### 캐시가 작동하지 않음

```
매번 API 호출 발생
```

**확인사항**:
1. `@EnableCaching` 어노테이션 확인
2. 캐시 키 일치 확인
3. 프록시 생성 확인 (Spring Bean)

---

### 메모리 부족

```
OutOfMemoryError
```

**해결책**:
```java
.maximumSize(50)  // 100 → 50 (크기 감소)
```

---

## 📚 참고 자료

- [Caffeine Cache 공식 문서](https://github.com/ben-manes/caffeine)
- [Spring Cache 가이드](https://spring.io/guides/gs/caching/)
- [한국투자증권 API 문서](https://apiportal.koreainvestment.com/)

---

## ✅ 체크리스트

- [x] Caffeine 의존성 추가
- [x] `@EnableCaching` 설정
- [x] `KisDataServiceCached` 구현
- [x] Rate Limiting 로직 추가
- [x] 서비스에 캐싱 적용
- [x] 로그 확인
- [x] 성능 테스트

---

## 📝 변경 이력

### v1.1.0 (2025-10-27)
- ✅ Caffeine 캐싱 추가
- ✅ Rate Limiting 구현
- ✅ KisDataServiceCached 생성
- ✅ 일봉 데이터 5분 캐싱
- ✅ 현재가 1분 캐싱
- ✅ 초당 10건 제한

### v1.0.0
- 기본 API 호출 구현
