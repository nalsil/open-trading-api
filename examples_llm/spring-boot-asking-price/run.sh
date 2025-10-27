#!/bin/bash

# Spring Boot 호가 조회 애플리케이션 실행 스크립트

echo "============================================"
echo "한국투자증권 API - 주식 호가 조회 샘플"
echo "============================================"
echo ""

# 환경 변수 확인
if [ -z "$KIS_APP_KEY" ] || [ -z "$KIS_APP_SECRET" ]; then
    echo "⚠️  경고: KIS API 인증 정보가 설정되지 않았습니다."
    echo ""
    echo "환경 변수를 설정하거나 application.yml을 수정하세요:"
    echo "  export KIS_APP_KEY=your_app_key"
    echo "  export KIS_APP_SECRET=your_app_secret"
    echo ""
    echo "또는 env.example 파일을 참고하세요."
    echo ""
fi

# Maven이 설치되어 있는지 확인
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven이 설치되어 있지 않습니다."
    echo "Maven을 설치한 후 다시 시도하세요."
    exit 1
fi

# 빌드 여부 확인
if [ "$1" == "--build" ] || [ ! -d "target" ]; then
    echo "📦 애플리케이션 빌드 중..."
    mvn clean package -DskipTests
    echo ""
fi

# 애플리케이션 실행
echo "🚀 애플리케이션 시작 중..."
echo ""

mvn spring-boot:run
