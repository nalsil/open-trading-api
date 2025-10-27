# 한국투자증권 API - Spring Boot 주식 호가 조회 샘플

한국투자증권의 Open Trading API를 사용하여 국내 주식의 실시간 호가 정보를 조회하는 Spring Boot 샘플 애플리케이션입니다.

## 주요 기능

- OAuth2 토큰 발급 및 자동 관리
- 국내 주식 호가 조회 (매도/매수 호가 10단계)
- 예상 체결 정보 조회
- RESTful API 제공
- 웹 UI 제공 (HTML/CSS/JavaScript)
- 실시간 자동 갱신 기능

## 기술 스택

- Java 17
- Spring Boot 3.2.0
- Maven
- Lombok

## 프로젝트 구조

```
spring-boot-asking-price/
├── src/
│   └── main/
│       ├── java/com/kis/sample/
│       │   ├── config/
│       │   │   ├── KisConfig.java                  # 한투 API 설정
│       │   │   └── WebConfig.java                  # CORS 및 웹 설정
│       │   ├── controller/
│       │   │   └── StockController.java            # REST API 컨트롤러
│       │   ├── service/
│       │   │   ├── KisAuthService.java             # 인증 서비스
│       │   │   └── KisStockService.java            # 주식 조회 서비스
│       │   ├── model/
│       │   │   ├── TokenResponse.java              # 토큰 응답 모델
│       │   │   └── AskingPriceResponse.java        # 호가 응답 모델
│       │   ├── runner/
│       │   │   └── AskingPriceTestRunner.java      # 시작 시 테스트 실행
│       │   └── KisStockAskingPriceApplication.java # 메인 애플리케이션
│       └── resources/
│           ├── application.yml                      # 애플리케이션 설정
│           └── static/                              # 웹 UI 리소스
│               ├── index.html                       # 메인 페이지
│               ├── css/
│               │   └── style.css                    # 스타일시트
│               └── js/
│                   └── app.js                       # JavaScript
├── pom.xml                                          # Maven 설정
└── README.md                                        # 이 파일
```

## 설정 방법

### 1. 한국투자증권 API 인증 정보 설정

`src/main/resources/application.yml` 파일을 수정하거나 환경 변수를 설정합니다:

```yaml
kis:
  mode: demo  # prod 또는 demo

  prod:
    app-key: YOUR_PROD_APP_KEY
    app-secret: YOUR_PROD_APP_SECRET
    base-url: https://openapi.koreainvestment.com:9443

  demo:
    app-key: YOUR_DEMO_APP_KEY
    app-secret: YOUR_DEMO_APP_SECRET
    base-url: https://openapivts.koreainvestment.com:29443
```

또는 환경 변수로 설정:

```bash
export KIS_APP_KEY=your_app_key
export KIS_APP_SECRET=your_app_secret
export KIS_DEMO_APP_KEY=your_demo_app_key
export KIS_DEMO_APP_SECRET=your_demo_app_secret
```

### 2. 빌드 및 실행

```bash
# Maven으로 빌드
mvn clean package

# 애플리케이션 실행
mvn spring-boot:run

# 또는 JAR 파일 실행
java -jar target/kis-stock-asking-price-1.0.0.jar
```

## 웹 UI 사용 방법

애플리케이션을 실행한 후 웹 브라우저에서 다음 주소로 접속하세요:

```
http://localhost:8080
```

### 웹 UI 주요 기능

1. **종목 조회**
   - 종목코드 입력란에 6자리 종목코드 입력 (예: 005930)
   - "조회" 버튼 클릭 또는 Enter 키로 조회

2. **빠른 조회**
   - 주요 종목 버튼 클릭으로 즉시 조회
   - 삼성전자, SK하이닉스, 카카오, NAVER, 현대차, LG화학

3. **자동 갱신**
   - "자동갱신" 버튼으로 5초마다 자동 갱신
   - ON/OFF 토글 가능

4. **호가판 정보**
   - 매도/매수 호가 10단계 표시
   - 현재가, 전일대비, 거래량 표시
   - 총 매도/매수 잔량 표시

### 웹 UI 스크린샷 설명

- **상단**: 종목코드 입력 및 빠른 조회 버튼
- **중단**: 현재가, 전일대비, 거래량 정보
- **하단**: 호가판 (매도/매수 10단계)

## API 사용 방법 (REST API)

### 1. 전체 호가 정보 조회

삼성전자(005930)의 호가 정보를 조회합니다:

```bash
curl http://localhost:8080/api/stock/asking-price/005930
```

**응답 예시:**

```json
{
  "rt_cd": "0",
  "msg_cd": "MCA00000",
  "msg1": "정상처리 되었습니다.",
  "output1": {
    "askp1": "71500",
    "askp2": "71600",
    "askp3": "71700",
    "askp4": "71800",
    "askp5": "71900",
    "bidp1": "71400",
    "bidp2": "71300",
    "bidp3": "71200",
    "bidp4": "71100",
    "bidp5": "71000",
    "askp_rsqn1": "12345",
    "askp_rsqn2": "23456",
    "bidp_rsqn1": "34567",
    "bidp_rsqn2": "45678",
    "total_askp_rsqn": "234567",
    "total_bidp_rsqn": "345678"
  },
  "output2": {
    "stck_prpr": "71450",
    "prdy_vrss": "500",
    "prdy_vrss_sign": "2",
    "prdy_ctrt": "0.70",
    "acml_vol": "12345678"
  }
}
```

### 2. 간단한 호가 정보 조회

주요 호가 정보만 조회합니다:

```bash
curl http://localhost:8080/api/stock/asking-price/005930/simple
```

**응답 예시:**

```json
{
  "stockCode": "005930",
  "currentPrice": "71450",
  "prevDayDiff": "500",
  "prevDayDiffSign": "2",
  "prevDayDiffRate": "0.70",
  "accumulatedVolume": "12345678",
  "askPrice1": "71500",
  "askPrice5": "71900",
  "bidPrice1": "71400",
  "bidPrice5": "71000",
  "totalAskVolume": "234567",
  "totalBidVolume": "345678"
}
```

### 3. 시장 구분 지정

KOSPI와 KOSDAQ을 구분하여 조회할 수 있습니다:

```bash
# KRX (KOSPI + KOSDAQ)
curl "http://localhost:8080/api/stock/asking-price/005930?marketDivCode=J"

# KOSDAQ
curl "http://localhost:8080/api/stock/asking-price/035720?marketDivCode=J"
```

## 응답 필드 설명

### Output1 (호가 정보)

| 필드 | 설명 |
|------|------|
| askp1~10 | 매도호가 1~10 |
| bidp1~10 | 매수호가 1~10 |
| askp_rsqn1~10 | 매도호가잔량 1~10 |
| bidp_rsqn1~10 | 매수호가잔량 1~10 |
| total_askp_rsqn | 총 매도호가 잔량 |
| total_bidp_rsqn | 총 매수호가 잔량 |

### Output2 (예상체결 정보)

| 필드 | 설명 |
|------|------|
| stck_prpr | 주식 현재가 |
| prdy_vrss | 전일 대비 |
| prdy_vrss_sign | 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락) |
| prdy_ctrt | 전일 대비율 |
| acml_vol | 누적 거래량 |

## 주요 종목 코드

| 종목명 | 종목코드 |
|--------|----------|
| 삼성전자 | 005930 |
| SK하이닉스 | 000660 |
| 카카오 | 035720 |
| NAVER | 035420 |
| 삼성바이오로직스 | 207940 |
| LG화학 | 051910 |
| 현대차 | 005380 |
| 기아 | 000270 |

## 참고 사항

1. **API 호출 제한**: 한국투자증권 API는 초당 20건의 호출 제한이 있습니다.
2. **토큰 유효기간**: 접근 토큰의 유효기간은 24시간입니다. 애플리케이션이 자동으로 토큰을 갱신합니다.
3. **실전/모의 구분**: `application.yml`의 `kis.mode`를 `prod` 또는 `demo`로 설정하여 전환할 수 있습니다.
4. **시장 시간**: 실시간 호가는 장 운영 시간(09:00~15:30)에만 제공됩니다.

## 트러블슈팅

### 1. 401 Unauthorized 오류

- API Key와 Secret이 올바른지 확인
- 토큰이 만료되었을 수 있으니 애플리케이션 재시작

### 2. 응답이 없거나 빈 데이터

- 종목코드가 올바른지 확인
- 시장 구분 코드가 올바른지 확인
- 장 마감 후에는 데이터가 제공되지 않을 수 있음

## 라이선스

이 샘플 코드는 교육 목적으로 제공됩니다.

## 관련 문서

- [한국투자증권 Open Trading API](https://apiportal.koreainvestment.com/)
- [API 개발 가이드](https://apiportal.koreainvestment.com/apiservice/apiservice-domestic-stock)
