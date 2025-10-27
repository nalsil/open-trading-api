// 전역 변수
let autoRefreshInterval = null;
let isAutoRefresh = false;
let currentStockCode = '';

// 종목명 매핑 (실제로는 별도 API에서 가져오는 것이 좋습니다)
const stockNames = {
    '005930': '삼성전자',
    '000660': 'SK하이닉스',
    '035720': '카카오',
    '035420': 'NAVER',
    '005380': '현대차',
    '000270': '기아',
    '051910': 'LG화학',
    '207940': '삼성바이오로직스',
    '006400': '삼성SDI',
    '068270': '셀트리온'
};

// 페이지 로드 시 이벤트 리스너 설정
document.addEventListener('DOMContentLoaded', function() {
    // Enter 키로 검색
    document.getElementById('stockCode').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchStock();
        }
    });

    // 초기 안내 메시지
    console.log('한국투자증권 주식 호가 조회 시스템이 준비되었습니다.');
});

/**
 * 주식 조회
 */
async function searchStock() {
    const stockCode = document.getElementById('stockCode').value.trim();

    if (!stockCode) {
        showError('종목코드를 입력해주세요.');
        return;
    }

    if (stockCode.length !== 6 || !/^\d+$/.test(stockCode)) {
        showError('올바른 종목코드를 입력해주세요. (6자리 숫자)');
        return;
    }

    currentStockCode = stockCode;
    await fetchStockData(stockCode);
}

/**
 * 빠른 조회
 */
function quickSearch(stockCode) {
    document.getElementById('stockCode').value = stockCode;
    searchStock();
}

/**
 * API에서 주식 데이터 가져오기
 */
async function fetchStockData(stockCode) {
    showLoading();
    hideError();
    hideStockInfo();

    try {
        const response = await fetch(`/api/stock/asking-price/${stockCode}?marketDivCode=J`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.rt_cd === '0') {
            displayStockData(stockCode, data);
        } else {
            showError(`API 오류: ${data.msg1} (코드: ${data.msg_cd})`);
        }
    } catch (error) {
        console.error('데이터 조회 오류:', error);
        showError('데이터를 불러오는 중 오류가 발생했습니다. 서버가 실행 중인지 확인해주세요.');
    } finally {
        hideLoading();
    }
}

/**
 * 주식 데이터 표시
 */
function displayStockData(stockCode, data) {
    const output1 = data.output1;
    const output2 = data.output2;

    if (!output1 || !output2) {
        showError('데이터 형식이 올바르지 않습니다.');
        return;
    }

    // 종목명 표시
    const stockName = stockNames[stockCode] || '종목명 없음';
    document.getElementById('stockName').textContent = stockName;
    document.getElementById('stockCodeDisplay').textContent = stockCode;

    // 현재가 정보 표시
    displayCurrentPrice(output2);

    // 호가 정보 표시
    displayOrderBook(output1);

    // 마지막 업데이트 시간
    updateLastUpdateTime();

    // 주식 정보 표시
    showStockInfo();
}

/**
 * 현재가 정보 표시
 */
function displayCurrentPrice(output2) {
    const currentPrice = parseInt(output2.stck_prpr);
    const priceChange = parseInt(output2.prdy_vrss);
    const priceChangeRate = parseFloat(output2.prdy_ctrt);
    const priceSign = output2.prdy_vrss_sign;
    const volume = parseInt(output2.acml_vol);

    // 현재가
    document.getElementById('currentPrice').textContent = formatNumber(currentPrice) + '원';

    // 전일대비
    const priceChangeElement = document.querySelector('.price-change');
    const changeValueElement = document.getElementById('priceChange');
    const changeRateElement = document.getElementById('priceChangeRate');

    let changeClass = 'unchanged';
    let changeSign = '';

    if (priceSign === '1' || priceSign === '2') {
        // 상한가 또는 상승
        changeClass = 'up';
        changeSign = '▲';
    } else if (priceSign === '4' || priceSign === '5') {
        // 하한가 또는 하락
        changeClass = 'down';
        changeSign = '▼';
    }

    priceChangeElement.className = `price-change ${changeClass}`;
    changeValueElement.textContent = `${changeSign} ${formatNumber(Math.abs(priceChange))}원`;
    changeRateElement.textContent = `${priceChangeRate}%`;

    // 거래량
    document.getElementById('volume').textContent = formatNumber(volume) + '주';
}

/**
 * 호가판 표시
 */
function displayOrderBook(output1) {
    const orderbookRows = document.getElementById('orderbookRows');
    orderbookRows.innerHTML = '';

    // 총 매도/매수 잔량
    document.getElementById('totalAskVolume').textContent =
        formatNumber(parseInt(output1.total_askp_rsqn)) + '주';
    document.getElementById('totalBidVolume').textContent =
        formatNumber(parseInt(output1.total_bidp_rsqn)) + '주';

    // 호가 10단계 표시 (역순으로 표시 - 매도호가가 위에)
    for (let i = 10; i >= 1; i--) {
        const askPrice = output1[`askp${i}`];
        const askVolume = output1[`askp_rsqn${i}`];
        const bidPrice = output1[`bidp${i}`];
        const bidVolume = output1[`bidp_rsqn${i}`];

        const row = createOrderBookRow(
            askPrice, askVolume, bidPrice, bidVolume
        );

        orderbookRows.appendChild(row);
    }
}

/**
 * 호가 행 생성
 */
function createOrderBookRow(askPrice, askVolume, bidPrice, bidVolume) {
    const row = document.createElement('div');
    row.className = 'orderbook-row';

    const askVolumeCell = document.createElement('div');
    askVolumeCell.className = 'cell ask-volume number';
    askVolumeCell.textContent = formatNumber(parseInt(askVolume || 0));

    const askPriceCell = document.createElement('div');
    askPriceCell.className = 'cell ask-price number';
    askPriceCell.textContent = formatNumber(parseInt(askPrice || 0));

    const bidPriceCell = document.createElement('div');
    bidPriceCell.className = 'cell bid-price number';
    bidPriceCell.textContent = formatNumber(parseInt(bidPrice || 0));

    const bidVolumeCell = document.createElement('div');
    bidVolumeCell.className = 'cell bid-volume number';
    bidVolumeCell.textContent = formatNumber(parseInt(bidVolume || 0));

    row.appendChild(askVolumeCell);
    row.appendChild(askPriceCell);
    row.appendChild(bidPriceCell);
    row.appendChild(bidVolumeCell);

    return row;
}

/**
 * 자동 갱신 토글
 */
function toggleAutoRefresh() {
    const btn = document.getElementById('autoRefreshBtn');

    if (isAutoRefresh) {
        // 자동 갱신 중지
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
        isAutoRefresh = false;
        btn.textContent = '자동갱신 OFF';
        btn.classList.remove('active');
    } else {
        // 자동 갱신 시작
        if (!currentStockCode) {
            showError('먼저 종목을 조회해주세요.');
            return;
        }

        isAutoRefresh = true;
        btn.textContent = '자동갱신 ON';
        btn.classList.add('active');

        // 5초마다 갱신
        autoRefreshInterval = setInterval(() => {
            if (currentStockCode) {
                fetchStockData(currentStockCode);
            }
        }, 5000);
    }
}

/**
 * 마지막 업데이트 시간 표시
 */
function updateLastUpdateTime() {
    const now = new Date();
    const timeString = now.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
    document.getElementById('lastUpdate').textContent = timeString;
}

/**
 * 숫자 포맷팅 (천단위 콤마)
 */
function formatNumber(num) {
    if (isNaN(num)) return '0';
    return num.toLocaleString('ko-KR');
}

/**
 * 로딩 표시
 */
function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'block';
}

/**
 * 로딩 숨김
 */
function hideLoading() {
    document.getElementById('loadingIndicator').style.display = 'none';
}

/**
 * 에러 메시지 표시
 */
function showError(message) {
    const errorElement = document.getElementById('errorMessage');
    errorElement.textContent = message;
    errorElement.style.display = 'block';
}

/**
 * 에러 메시지 숨김
 */
function hideError() {
    document.getElementById('errorMessage').style.display = 'none';
}

/**
 * 주식 정보 표시
 */
function showStockInfo() {
    document.getElementById('stockInfo').style.display = 'block';
}

/**
 * 주식 정보 숨김
 */
function hideStockInfo() {
    document.getElementById('stockInfo').style.display = 'none';
}

// 페이지 떠날 때 자동 갱신 중지
window.addEventListener('beforeunload', function() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
});
