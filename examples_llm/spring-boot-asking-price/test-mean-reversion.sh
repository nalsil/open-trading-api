#!/bin/bash

# 평균 회귀 전략 테스트 스크립트

echo "=== 평균 회귀 전략 (Mean Reversion) 테스트 ==="
echo ""

# 서버 URL
BASE_URL="http://localhost:8080"

# 테스트할 종목들
declare -A STOCKS=(
    ["005930"]="삼성전자"
    ["000660"]="SK하이닉스"
    ["035420"]="NAVER"
    ["035720"]="카카오"
)

echo "1. 매매 시그널 조회 테스트"
echo "----------------------------"

for CODE in "${!STOCKS[@]}"; do
    NAME="${STOCKS[$CODE]}"
    echo ""
    echo "종목: $NAME ($CODE)"

    # 시그널 조회
    curl -s "${BASE_URL}/api/strategy/mean-reversion/signal?stockCode=${CODE}&stockName=${NAME}" | jq '.'

    sleep 1
done

echo ""
echo ""
echo "2. 분석 결과 (시그널 + 차트 URL) 조회 테스트"
echo "-------------------------------------------"

for CODE in "${!STOCKS[@]}"; do
    NAME="${STOCKS[$CODE]}"
    echo ""
    echo "종목: $NAME ($CODE)"

    # 분석 조회
    RESULT=$(curl -s "${BASE_URL}/api/strategy/mean-reversion/analyze?stockCode=${CODE}&stockName=${NAME}")
    echo "$RESULT" | jq '.'

    # 차트 URL 추출
    CHART_URL=$(echo "$RESULT" | jq -r '.chartUrl')
    echo "차트 다운로드: ${BASE_URL}${CHART_URL}"

    sleep 1
done

echo ""
echo ""
echo "3. 차트 다운로드 테스트"
echo "----------------------"

# 삼성전자 차트 다운로드
CODE="005930"
NAME="삼성전자"
OUTPUT_FILE="samsung_mean_reversion_chart.png"

echo "다운로드 중: $NAME ($CODE)"
curl -s -o "$OUTPUT_FILE" "${BASE_URL}/api/strategy/mean-reversion/chart?stockCode=${CODE}&stockName=${NAME}"

if [ -f "$OUTPUT_FILE" ]; then
    echo "✓ 차트 저장 완료: $OUTPUT_FILE"
    echo "  파일 크기: $(du -h "$OUTPUT_FILE" | cut -f1)"
else
    echo "✗ 차트 다운로드 실패"
fi

echo ""
echo "=== 테스트 완료 ==="
