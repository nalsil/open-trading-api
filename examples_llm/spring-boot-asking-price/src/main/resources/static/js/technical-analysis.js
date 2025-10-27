// 기술적 분석 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // 요소 참조
    const analyzeBtn = document.getElementById('analyzeBtn');
    const resetBtn = document.getElementById('resetBtn');
    const generateChartBtn = document.getElementById('generateChartBtn');
    const downloadChartBtn = document.getElementById('downloadChartBtn');

    // 파라미터 입력
    const stockCodeInput = document.getElementById('stockCode');
    const stockNameInput = document.getElementById('stockName');
    const displayDaysInput = document.getElementById('displayDays');

    const bbPeriodInput = document.getElementById('bbPeriod');
    const bbMultiplierInput = document.getElementById('bbMultiplier');
    const rsiPeriodInput = document.getElementById('rsiPeriod');
    const rsiOverboughtInput = document.getElementById('rsiOverbought');
    const rsiOversoldInput = document.getElementById('rsiOversold');

    // 파라미터 값 표시
    const bbPeriodValue = document.getElementById('bbPeriodValue');
    const bbMultiplierValue = document.getElementById('bbMultiplierValue');
    const rsiPeriodValue = document.getElementById('rsiPeriodValue');
    const rsiOverboughtValue = document.getElementById('rsiOverboughtValue');
    const rsiOversoldValue = document.getElementById('rsiOversoldValue');

    // 결과 표시 영역
    const resultSection = document.getElementById('resultSection');
    const loading = document.getElementById('loading');
    const errorMessage = document.getElementById('errorMessage');

    // 차트 블롭 URL (메모리 관리용)
    let chartBlobUrl = null;

    // 슬라이더 값 업데이트
    bbPeriodInput.addEventListener('input', (e) => {
        bbPeriodValue.textContent = e.target.value;
    });

    bbMultiplierInput.addEventListener('input', (e) => {
        bbMultiplierValue.textContent = parseFloat(e.target.value).toFixed(1);
    });

    rsiPeriodInput.addEventListener('input', (e) => {
        rsiPeriodValue.textContent = e.target.value;
    });

    rsiOverboughtInput.addEventListener('input', (e) => {
        rsiOverboughtValue.textContent = e.target.value;
    });

    rsiOversoldInput.addEventListener('input', (e) => {
        rsiOversoldValue.textContent = e.target.value;
    });

    // 분석 실행
    analyzeBtn.addEventListener('click', async () => {
        const params = getParameters();

        if (!params.stockCode) {
            showError('종목 코드를 입력하세요.');
            return;
        }

        try {
            showLoading(true);
            hideError();
            resultSection.style.display = 'none';

            const result = await analyzeStock(params);
            displayResult(result, params.stockName);

            resultSection.style.display = 'block';
        } catch (error) {
            showError('분석 실패: ' + error.message);
        } finally {
            showLoading(false);
        }
    });

    // 초기화
    resetBtn.addEventListener('click', () => {
        stockCodeInput.value = '005930';
        stockNameInput.value = '삼성전자';
        displayDaysInput.value = '60';

        bbPeriodInput.value = '20';
        bbMultiplierInput.value = '2.0';
        rsiPeriodInput.value = '14';
        rsiOverboughtInput.value = '70';
        rsiOversoldInput.value = '30';

        bbPeriodValue.textContent = '20';
        bbMultiplierValue.textContent = '2.0';
        rsiPeriodValue.textContent = '14';
        rsiOverboughtValue.textContent = '70';
        rsiOversoldValue.textContent = '30';

        resultSection.style.display = 'none';
        hideError();
    });

    // 차트 생성
    generateChartBtn.addEventListener('click', async () => {
        const params = getParameters();

        if (!params.stockCode) {
            showError('종목 코드를 입력하세요.');
            return;
        }

        try {
            document.getElementById('chartLoading').style.display = 'block';
            document.getElementById('chartContainer').style.display = 'none';
            hideError();

            const blob = await generateChart(params);

            // 이전 블롭 URL 해제
            if (chartBlobUrl) {
                URL.revokeObjectURL(chartBlobUrl);
            }

            chartBlobUrl = URL.createObjectURL(blob);

            const chartImage = document.getElementById('chartImage');
            chartImage.src = chartBlobUrl;

            document.getElementById('chartContainer').style.display = 'block';
            downloadChartBtn.style.display = 'inline-flex';

        } catch (error) {
            showError('차트 생성 실패: ' + error.message);
        } finally {
            document.getElementById('chartLoading').style.display = 'none';
        }
    });

    // 차트 다운로드
    downloadChartBtn.addEventListener('click', () => {
        if (!chartBlobUrl) return;

        const params = getParameters();
        const link = document.createElement('a');
        link.href = chartBlobUrl;
        link.download = `technical_${params.stockCode}_${Date.now()}.png`;
        link.click();
    });

    // 파라미터 가져오기
    function getParameters() {
        return {
            stockCode: stockCodeInput.value.trim(),
            stockName: stockNameInput.value.trim() || '종목',
            displayDays: parseInt(displayDaysInput.value),
            bbPeriod: parseInt(bbPeriodInput.value),
            bbMultiplier: parseFloat(bbMultiplierInput.value),
            rsiPeriod: parseInt(rsiPeriodInput.value),
            rsiOverbought: parseFloat(rsiOverboughtInput.value),
            rsiOversold: parseFloat(rsiOversoldInput.value)
        };
    }

    // 주식 분석 API 호출
    async function analyzeStock(params) {
        const response = await fetch('/api/technical-analysis/analyze', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        });

        if (!response.ok) {
            throw new Error('분석 요청 실패');
        }

        return await response.json();
    }

    // 차트 생성 API 호출
    async function generateChart(params) {
        const response = await fetch('/api/technical-analysis/chart', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        });

        if (!response.ok) {
            throw new Error('차트 생성 실패');
        }

        return await response.blob();
    }

    // 결과 표시
    function displayResult(result, stockName) {
        // 종목 정보
        document.getElementById('stockTitle').textContent = `${stockName} (${result.stockCode})`;
        document.getElementById('currentPrice').textContent = formatNumber(result.currentPrice) + ' 원';

        // 복합 시그널
        const signal = result.indicators.combinedSignal;
        displaySignal(signal);

        // 볼린저 밴드
        const bb = result.indicators.bollingerBands;
        document.getElementById('bbUpper').textContent = formatNumber(bb.upper);
        document.getElementById('bbMiddle').textContent = formatNumber(bb.middle);
        document.getElementById('bbLower').textContent = formatNumber(bb.lower);
        document.getElementById('bbWidth').textContent = bb.bandWidth.toFixed(2) + '%';

        // RSI
        const rsi = result.indicators.rsi;
        displayRSI(rsi);

        // 차트 초기화
        document.getElementById('chartContainer').style.display = 'none';
        downloadChartBtn.style.display = 'none';
    }

    // 시그널 표시
    function displaySignal(signal) {
        const signalBadge = document.getElementById('signalBadge');
        const signalReason = document.getElementById('signalReason');
        const confidenceBar = document.getElementById('confidenceBar');
        const confidenceValue = document.getElementById('confidenceValue');

        // 시그널 타입별 스타일
        const signalClass = signal.signal.toLowerCase().replace('_', '-');
        signalBadge.className = `signal-badge ${signalClass}`;
        signalBadge.textContent = getSignalText(signal.signal);

        signalReason.textContent = signal.reason;

        confidenceBar.style.width = signal.confidence + '%';
        confidenceValue.textContent = signal.confidence.toFixed(0) + '%';
    }

    // RSI 표시
    function displayRSI(rsi) {
        const rsiBar = document.getElementById('rsiBar');
        const rsiValue = document.getElementById('rsiValue');
        const rsiStatus = document.getElementById('rsiStatus');

        rsiBar.style.width = rsi.value + '%';
        rsiValue.textContent = rsi.value.toFixed(1);

        const statusClass = rsi.status.toLowerCase();
        rsiStatus.className = `rsi-status ${statusClass}`;
        rsiStatus.textContent = getStatusText(rsi.status);
    }

    // 시그널 텍스트 변환
    function getSignalText(signal) {
        const signalMap = {
            'STRONG_BUY': '강력 매수',
            'BUY': '매수',
            'HOLD': '보유',
            'SELL': '매도',
            'STRONG_SELL': '강력 매도'
        };
        return signalMap[signal] || signal;
    }

    // 상태 텍스트 변환
    function getStatusText(status) {
        const statusMap = {
            'OVERBOUGHT': '과매수',
            'NEUTRAL': '중립',
            'OVERSOLD': '과매도'
        };
        return statusMap[status] || status;
    }

    // 숫자 포맷
    function formatNumber(num) {
        return Math.round(num).toLocaleString('ko-KR');
    }

    // 로딩 표시
    function showLoading(show) {
        loading.style.display = show ? 'block' : 'none';
    }

    // 에러 표시
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
    }

    // 에러 숨김
    function hideError() {
        errorMessage.style.display = 'none';
    }

    // 페이지 언로드 시 블롭 URL 해제
    window.addEventListener('beforeunload', () => {
        if (chartBlobUrl) {
            URL.revokeObjectURL(chartBlobUrl);
        }
    });
});
