# 토큰 관리 가이드

한국투자증권 API 접근 토큰 발급 제한(1분당 1회)을 해결하기 위한 토큰 관리 전략입니다.

## 📋 목차

1. [문제점](#문제점)
2. [해결 방안](#해결-방안)
3. [구현 내용](#구현-내용)
4. [사용 방법](#사용-방법)
5. [토큰 정보 확인](#토큰-정보-확인)

---

## ❌ 문제점

### 토큰 발급 오류

```
Error: 접근토큰 발급 잠시 후 다시 시도하세요(1분당 1회)
```

**원인**:
- 한국투자증권 API는 토큰 발급을 **1분당 1회**로 제한
- 애플리케이션 시작 시 여러 서비스가 동시에 토큰 요청
- `refreshToken()` 호출 시 1분 제한 무시

**영향**:
- API 호출 실패
- 애플리케이션 시작 오류
- 사용자 경험 저하

---

## ✅ 해결 방안

### 1. 토큰 재사용
유효한 토큰이 있으면 재발급하지 않음

### 2. 만료 전 자동 갱신
만료 5분 전에 자동으로 갱신

### 3. 동시 요청 방지
Lock을 사용하여 중복 발급 방지

### 4. 1분 제한 준수
마지막 발급 시간 추적 및 대기

---

## 🔧 구현 내용

### 1. 토큰 캐싱 및 재사용

#### 기존 코드 (문제)
```java
// 매번 새로 발급 시도 → 1분 제한 위반
public String getAccessToken() {
    return issueNewToken();  // ❌
}
```

#### 개선된 코드 (해결)
```java
// 유효한 토큰 재사용
public String getAccessToken() {
    if (isTokenValid()) {
        return accessToken;  // ✓ 기존 토큰 사용
    }
    return issueNewToken();  // 필요할 때만 발급
}
```

---

### 2. 토큰 유효성 확인

```java
private boolean isTokenValid() {
    if (accessToken == null || tokenExpiredTime == null) {
        return false;
    }

    // 만료 5분 전까지 유효
    LocalDateTime refreshThreshold =
        tokenExpiredTime.minusMinutes(5);

    return LocalDateTime.now().isBefore(refreshThreshold);
}
```

**작동 원리**:
```
발급 시간:     00:00
만료 시간:     12:00
갱신 임계값:   11:55 (만료 5분 전)

00:00 ~ 11:54 → 기존 토큰 사용 ✓
11:55 ~ 12:00 → 새 토큰 발급 ✓
```

---

### 3. 1분 제한 준수

```java
private String issueNewToken() {
    // 마지막 발급 시간 확인
    if (lastTokenIssuedTime != null) {
        long secondsSinceLastIssue =
            Duration.between(lastTokenIssuedTime, LocalDateTime.now())
                .getSeconds();

        if (secondsSinceLastIssue < 60) {
            // 1분 미만이면 대기 또는 기존 토큰 사용
            if (accessToken != null) {
                return accessToken;  // 기존 토큰 사용
            } else {
                Thread.sleep((60 - secondsSinceLastIssue) * 1000);  // 대기
            }
        }
    }

    // 토큰 발급
    // ...
    this.lastTokenIssuedTime = LocalDateTime.now();
}
```

---

### 4. 동시 요청 방지 (Lock)

```java
private final ReentrantLock tokenLock = new ReentrantLock();

public String getAccessToken() {
    if (isTokenValid()) {
        return accessToken;
    }

    tokenLock.lock();  // Lock 획득
    try {
        // Double-check
        if (isTokenValid()) {
            return accessToken;  // 다른 스레드가 이미 발급
        }

        return issueNewToken();
    } finally {
        tokenLock.unlock();  // Lock 해제
    }
}
```

**시나리오**:
```
스레드 A: Lock 획득 → 토큰 발급 중...
스레드 B: Lock 대기 →
스레드 C: Lock 대기 →

스레드 A: 토큰 발급 완료 → Lock 해제
스레드 B: Lock 획득 → 이미 토큰 있음 → 즉시 반환
스레드 C: Lock 획득 → 이미 토큰 있음 → 즉시 반환
```

---

## 📖 사용 방법

### 1. 자동 토큰 관리

코드 변경 없이 자동으로 적용됩니다.

```java
@Autowired
private KisAuthService kisAuthService;

// 첫 번째 호출 - 토큰 발급
String token1 = kisAuthService.getAccessToken();

// 이후 호출 - 캐시된 토큰 사용 (발급 안 함)
String token2 = kisAuthService.getAccessToken();
String token3 = kisAuthService.getAccessToken();
```

---

### 2. 토큰 정보 확인

#### API 호출
```bash
curl http://localhost:8080/api/debug/token-info
```

#### 응답 예시
```json
{
  "hasToken": true,
  "tokenExpiredTime": "2025-10-27T20:00:00",
  "lastTokenIssuedTime": "2025-10-27T08:00:00",
  "remainingMinutes": 720,
  "secondsSinceLastIssue": 3600
}
```

**필드 설명**:
- `hasToken`: 토큰 보유 여부
- `tokenExpiredTime`: 토큰 만료 시간
- `lastTokenIssuedTime`: 마지막 발급 시간
- `remainingMinutes`: 남은 유효 시간 (분)
- `secondsSinceLastIssue`: 마지막 발급 후 경과 시간 (초)

---

### 3. 토큰 강제 갱신

**주의**: 1분 제한을 고려하여 사용

```bash
# API 호출
curl -X POST http://localhost:8080/api/debug/refresh-token
```

**작동 로직**:
```
마지막 발급 후 60초 미만 → 갱신 거부 (로그 경고)
마지막 발급 후 60초 이상 → 새 토큰 발급
```

---

## 🎯 토큰 라이프사이클

### 정상 흐름

```
00:00 - 애플리케이션 시작
00:00 - 첫 API 호출 → 토큰 발급 (유효: 12시간)
00:01 - API 호출 → 캐시 사용
01:00 - API 호출 → 캐시 사용
11:55 - API 호출 → 만료 5분 전 → 새 토큰 발급
12:00 - (이전 토큰 만료)
23:55 - API 호출 → 새 토큰 발급
```

---

### 오류 방지 흐름

```
시나리오 1: 동시 요청
스레드 A, B, C가 동시에 토큰 요청
→ Lock으로 순차 처리
→ A만 발급, B/C는 A의 토큰 사용

시나리오 2: 1분 내 재발급 시도
00:00 - 토큰 발급
00:30 - refreshToken() 호출
→ 30초 < 60초 → 대기 30초
→ 01:00 - 새 토큰 발급

시나리오 3: 기존 토큰 있을 때 1분 내 재요청
00:00 - 토큰 발급
00:30 - getAccessToken() 호출
→ 기존 토큰 유효 → 즉시 반환 (발급 안 함)
```

---

## 📊 성능 개선

### Before (문제 발생)

```
00:00 - 서비스 A: 토큰 발급
00:00 - 서비스 B: 토큰 발급 시도 → ❌ 오류 (1분 제한)
00:00 - 서비스 C: 토큰 발급 시도 → ❌ 오류 (1분 제한)

결과: API 호출 실패, 서비스 시작 실패
```

---

### After (해결)

```
00:00 - 서비스 A: 토큰 발급 (Lock 획득)
00:00 - 서비스 B: Lock 대기 → A의 토큰 사용 ✓
00:00 - 서비스 C: Lock 대기 → A의 토큰 사용 ✓

결과: 토큰 1번 발급, 모든 서비스 정상 작동 ✓
```

---

## ⚙️ 설정 조정

### 1. 토큰 갱신 시간 변경

```java
// 기본: 만료 5분 전 갱신
private static final long TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 5;

// 더 보수적: 만료 10분 전
private static final long TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 10;

// 더 공격적: 만료 1분 전
private static final long TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 1;
```

---

### 2. 발급 간격 조정

```java
// 기본: 60초 (1분당 1회)
private static final long MIN_TOKEN_ISSUE_INTERVAL_SECONDS = 60;

// 더 보수적: 70초
private static final long MIN_TOKEN_ISSUE_INTERVAL_SECONDS = 70;
```

---

## 🔍 로그 확인

### 정상 작동 로그

```
INFO: Requesting new token from KIS API...
INFO: ✓ New token issued successfully. Valid for 720 minutes (expires at: 2025-10-27T20:00:00)
DEBUG: Using existing token. Expires at: 2025-10-27T20:00:00
DEBUG: Using existing token. Expires at: 2025-10-27T20:00:00
```

---

### 만료 임박 로그

```
INFO: Token will expire soon. Expires at: 2025-10-27T12:00:00, refresh threshold: 2025-10-27T11:55:00
INFO: Requesting new token from KIS API...
INFO: ✓ New token issued successfully. Valid for 720 minutes
```

---

### 1분 제한 경고 로그

```
WARN: Token issue rate limit: returning existing token (wait 30s for new token)
```

---

### 강제 갱신 거부 로그

```
INFO: Forcing token refresh...
WARN: Cannot refresh token yet. Wait 45s (1min rate limit)
```

---

## ⚠️ 주의사항

### 1. refreshToken() 사용 제한

```java
// ❌ 잘못된 사용
for (int i = 0; i < 10; i++) {
    kisAuthService.refreshToken();  // 1분 제한 위반!
}

// ✓ 올바른 사용
String token = kisAuthService.getAccessToken();  // 자동 관리
```

---

### 2. 토큰 만료 시간

```
토큰 유효 시간: 약 12시간
자동 갱신: 만료 5분 전
→ 장시간 실행 서비스는 자동으로 토큰 갱신됨
```

---

### 3. 애플리케이션 재시작

```
애플리케이션 재시작 → 토큰 소실 → 새로 발급 필요
→ 1분 제한 고려하여 재시작 간격 조절 권장
```

---

## 🧪 테스트

### 1. 토큰 캐싱 테스트

```bash
# 토큰 정보 확인
curl http://localhost:8080/api/debug/token-info

# API 호출 (토큰 사용)
curl http://localhost:8080/api/technical-analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"005930"}'

# 다시 토큰 정보 확인 (변화 없음)
curl http://localhost:8080/api/debug/token-info
```

**예상 결과**: 토큰 정보 동일, 새 발급 없음

---

### 2. 1분 제한 테스트

```bash
# 강제 갱신 (성공)
curl -X POST http://localhost:8080/api/debug/refresh-token

# 즉시 다시 갱신 시도 (거부)
curl -X POST http://localhost:8080/api/debug/refresh-token
```

**예상 결과**:
- 첫 번째: 성공
- 두 번째: "Cannot refresh token yet. Wait Xs"

---

### 3. 동시 요청 테스트

```bash
# 동시에 3개 요청
for i in {1..3}; do
  curl http://localhost:8080/api/technical-analysis/analyze \
    -H "Content-Type: application/json" \
    -d '{"stockCode":"00066'$i'"}' &
done
```

**예상 결과**: 토큰 1번만 발급, 모든 요청 성공

---

## 📈 모니터링

### 토큰 상태 확인 스크립트

```bash
#!/bin/bash

while true; do
  echo "=== Token Info $(date) ==="
  curl -s http://localhost:8080/api/debug/token-info | jq .
  echo ""
  sleep 60
done
```

---

## 🔗 관련 파일

```
src/main/java/com/kis/sample/
├── service/
│   ├── KisAuthService.java          # 개선된 토큰 관리 서비스
│   └── KisAuthService.java.old      # 기존 버전 (백업)
└── controller/
    └── DebugController.java         # 토큰 정보 확인 API
```

---

## 💡 Best Practices

### 1. 토큰 자동 관리 활용

```java
// ✓ 권장
String token = kisAuthService.getAccessToken();

// ❌ 비권장
kisAuthService.refreshToken();  // 수동 갱신
```

---

### 2. 애플리케이션 재시작 간격

```
최소 재시작 간격: 1분 이상
권장 재시작 간격: 2분 이상
```

---

### 3. 개발 vs 운영

**개발 환경**:
```java
TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 1  // 빠른 테스트
```

**운영 환경**:
```java
TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 5  // 안전한 운영
```

---

## 📞 문제 해결

### 여전히 1분 오류 발생

```
Error: 접근토큰 발급 잠시 후 다시 시도하세요
```

**확인사항**:
1. 다른 인스턴스에서 동일 계정 사용 여부
2. 애플리케이션 재시작 빈도
3. 로그에서 실제 발급 시간 확인

**해결책**:
```java
// 발급 간격 증가
MIN_TOKEN_ISSUE_INTERVAL_SECONDS = 70
```

---

### 토큰 만료 오류

```
Error: 접근토큰이 유효하지 않습니다
```

**확인사항**:
1. 토큰 정보 확인: `/api/debug/token-info`
2. 만료 시간 확인

**해결책**:
```java
// 갱신 시간 증가
TOKEN_REFRESH_BEFORE_EXPIRY_MINUTES = 10
```

---

## 📚 참고 자료

- [한국투자증권 API 문서](https://apiportal.koreainvestment.com/)
- [OAuth 2.0 표준](https://oauth.net/2/)

---

## ✅ 체크리스트

- [x] 토큰 캐싱 구현
- [x] 1분 제한 준수
- [x] 동시 요청 방지 (Lock)
- [x] 만료 전 자동 갱신
- [x] 디버그 API 추가
- [x] 로그 개선

---

## 📝 변경 이력

### v1.2.0 (2025-10-27)
- ✅ 토큰 1분 제한 준수 로직 추가
- ✅ ReentrantLock으로 동시 요청 방지
- ✅ 만료 5분 전 자동 갱신
- ✅ 마지막 발급 시간 추적
- ✅ DebugController 추가
- ✅ 상세 로그 추가

### v1.1.0
- Caffeine 캐싱 추가

### v1.0.0
- 기본 토큰 발급 구현
