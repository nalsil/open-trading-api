# 평균 회귀 전략 (Mean Reversion Strategy) 가이드

한국투자증권 API를 활용한 볼린저 밴드 기반 평균 회귀 전략 구현 가이드입니다.

## 목차

1. [전략 개요](#전략-개요)
2. [기술 스택](#기술-스택)
3. [API 엔드포인트](#api-엔드포인트)
4. [사용 방법](#사용-방법)
5. [전략 설명](#전략-설명)
6. [차트 해석](#차트-해석)

---

## 전략 개요

### 평균 회귀 (Mean Reversion)란?

주가가 평균에서 크게 벗어났을 때 다시 평균으로 회귀하려는 성질을 이용한 매매 전략입니다.

### 볼린저 밴드 (Bollinger Bands)

- **중간 밴드**: 20일 이동평균선
- **상단 밴드**: 중간 밴드 + (2 × 표준편차)
- **하단 밴드**: 중간 밴드 - (2 × 표준편차)

### 매매 시그널

- **매수 (BUY)**: 현재가가 하단 밴드 아래 → 과매도 상태, 반등 기대
- **매도 (SELL)**: 현재가가 상단 밴드 위 → 과매수 상태, 조정 예상
- **보유 (HOLD)**: 현재가가 밴드 내부 → 정상 범위

---

## 기술 스택

### Backend
- Java 17
- Spring Boot 3.2.0
- Maven

### Dependencies
- JFreeChart 1.5.4 (차트 생성)
- Spring Web (RESTful API)
- Jackson (JSON 처리)
- Lombok (코드 간소화)

### 한국투자증권 API
- 주식 현재가 시세 조회 (`FHKST01010100`)
- 주식 일봉 차트 조회 (`FHKST03010100`)

---

## API 엔드포인트

### 1. 매매 시그널 조회

**GET** `/api/strategy/mean-reversion/signal`

#### 파라미터
- `stockCode` (필수): 종목 코드 (예: 005930)
- `stockName` (선택): 종목 이름 (예: 삼성전자)

#### 응답 예시
```json
{
  "stockCode": "005930",
  "stockName": "삼성전자",
  "currentPrice": 71000,
  "upperBand": 73500,
  "middleBand": 70000,
  "lowerBand": 66500,
  "stdDev": 1750,
  "signal": "HOLD",
  "signalReason": "정상 범위: 밴드 내 61.5% 위치 (중간밴드: 70000)",
  "bandWidth": 10.0
}
```

#### 시그널 타입
- `BUY`: 매수 시그널 (과매도)
- `SELL`: 매도 시그널 (과매수)
- `HOLD`: 보유 (정상 범위)

---

### 2. 분석 결과 조회 (시그널 + 차트 URL)

**GET** `/api/strategy/mean-reversion/analyze`

#### 파라미터
- `stockCode` (필수): 종목 코드
- `stockName` (선택): 종목 이름

#### 응답 예시
```json
{
  "signal": {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "currentPrice": 71000,
    "upperBand": 73500,
    "middleBand": 70000,
    "lowerBand": 66500,
    "signal": "HOLD",
    "signalReason": "정상 범위: 밴드 내 61.5% 위치",
    "bandWidth": 10.0
  },
  "chartUrl": "/api/strategy/mean-reversion/chart?stockCode=005930&stockName=삼성전자"
}
```

---

### 3. 차트 다운로드

**GET** `/api/strategy/mean-reversion/chart`

#### 파라미터
- `stockCode` (필수): 종목 코드
- `stockName` (선택): 종목 이름

#### 응답
- Content-Type: `image/png`
- 1200x600 픽셀 PNG 이미지

---

## 사용 방법

### 1. 애플리케이션 실행

```bash
# 환경 변수 설정 (.env 파일 또는 직접 설정)
export KIS_MODE=demo
export KIS_DEMO_APP_KEY=your_app_key
export KIS_DEMO_APP_SECRET=your_app_secret
export KIS_DEMO_BASE_URL=https://openapi.koreainvestment.com:9443

# 실행
./run.sh
# 또는
mvn spring-boot:run
```

### 2. API 호출 예시

#### cURL로 시그널 조회

```bash
# 삼성전자 시그널 조회
curl "http://localhost:8080/api/strategy/mean-reversion/signal?stockCode=005930&stockName=삼성전자"

# SK하이닉스 시그널 조회
curl "http://localhost:8080/api/strategy/mean-reversion/signal?stockCode=000660&stockName=SK하이닉스"
```

#### cURL로 차트 다운로드

```bash
# 삼성전자 차트 다운로드
curl -o samsung_chart.png "http://localhost:8080/api/strategy/mean-reversion/chart?stockCode=005930&stockName=삼성전자"

# NAVER 차트 다운로드
curl -o naver_chart.png "http://localhost:8080/api/strategy/mean-reversion/chart?stockCode=035420&stockName=NAVER"
```

#### 테스트 스크립트 실행

```bash
# 제공된 테스트 스크립트 실행
./test-mean-reversion.sh
```

---

## 전략 설명

### 볼린저 밴드 계산 로직

#### 1. 최근 20일 종가 데이터 수집
```java
// KisDataService에서 일봉 데이터 조회
DailyPriceResponse dailyPrices = kisDataService.getDailyPrices(stockCode, "D");
```

#### 2. 이동평균 (중간 밴드) 계산
```
중간 밴드 = (최근 20일 종가 합계) / 20
```

#### 3. 표준편차 계산
```
분산 = Σ(종가 - 이동평균)² / 20
표준편차 = √분산
```

#### 4. 상단/하단 밴드 계산
```
상단 밴드 = 중간 밴드 + (2 × 표준편차)
하단 밴드 = 중간 밴드 - (2 × 표준편차)
```

### 시그널 판단 로직

```java
if (현재가 < 하단 밴드) {
    시그널 = BUY;  // 과매도
} else if (현재가 > 상단 밴드) {
    시그널 = SELL; // 과매수
} else {
    시그널 = HOLD; // 정상 범위
}
```

### 밴드 폭 (Band Width) 계산

```
밴드폭 = (상단밴드 - 하단밴드) / 중간밴드 × 100
```

- **높은 밴드폭**: 변동성이 큼 (급등락 가능성)
- **낮은 밴드폭**: 변동성이 작음 (안정적)

---

## 차트 해석

### 차트 구성 요소

1. **파란색 실선**: 주가 (최근 30일 종가)
2. **초록색 실선**: 중간 밴드 (20일 이동평균)
3. **빨간색 점선**: 상단/하단 밴드
4. **상단 텍스트**: 시그널 정보

### 차트 읽는 법

#### 매수 시그널 예시
```
현재가가 하단 밴드 아래로 이탈
→ 과매도 상태
→ 평균으로 회귀 가능성 ↑
→ 매수 고려
```

#### 매도 시그널 예시
```
현재가가 상단 밴드 위로 이탈
→ 과매수 상태
→ 조정 가능성 ↑
→ 매도 고려
```

#### 보유 시그널 예시
```
현재가가 밴드 내부
→ 정상 범위
→ 관망 또는 기존 포지션 유지
```

### 차트 저장 위치

생성된 차트는 다음 경로에 저장됩니다:

```
~/kis-charts/mean_reversion_{종목코드}_{타임스탬프}.png
```

예시:
```
~/kis-charts/mean_reversion_005930_1698745123456.png
```

---

## 주의사항

### 전략 한계
1. **횡보장에서 효과적**: 추세장에서는 손실 가능
2. **단기 전략**: 중장기 투자에는 부적합
3. **보조 지표 필요**: 거래량, RSI 등 병행 권장

### 리스크 관리
1. **손절매 설정**: 일정 손실률 도달 시 손절
2. **분할 매매**: 한 번에 전액 투자 지양
3. **변동성 확인**: 밴드폭이 너무 좁거나 넓을 때 주의

### API 제한사항
- **호출 제한**: 한국투자증권 API 호출 횟수 제한 (1초당 20건)
- **데이터 지연**: 실시간이 아닌 지연 데이터일 수 있음
- **장 시간**: 장 마감 후 데이터는 당일 종가 기준

---

## 주요 종목 코드

| 종목명 | 종목코드 |
|--------|----------|
| 삼성전자 | 005930 |
| SK하이닉스 | 000660 |
| NAVER | 035420 |
| 카카오 | 035720 |
| LG에너지솔루션 | 373220 |
| 삼성바이오로직스 | 207940 |
| 현대차 | 005380 |
| 기아 | 000270 |

---

## 문제 해결

### 토큰 발급 실패
```
Error: Failed to get access token
```
→ `.env` 파일의 API 키/시크릿 확인

### 데이터 부족 오류
```
Error: Not enough data to calculate Bollinger Bands
```
→ 상장한 지 얼마 안 된 종목 (최소 20일 데이터 필요)

### 차트 생성 실패
```
Error: Failed to create chart
```
→ `~/kis-charts` 디렉토리 쓰기 권한 확인

---

## 참고 자료

- [한국투자증권 Open API 문서](https://apiportal.koreainvestment.com/)
- [볼린저 밴드 이론](https://www.investopedia.com/terms/b/bollingerbands.asp)
- [평균 회귀 전략](https://www.investopedia.com/terms/m/meanreversion.asp)

---

## 라이선스

이 프로젝트는 교육 목적으로 제공되며, 실제 투자에 사용 시 발생하는 손실에 대해 책임지지 않습니다.
