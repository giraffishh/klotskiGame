// 华容道游戏查看器JavaScript
let socket;
let lastUpdateTime = null;
let reconnectAttempts = 0;
let debugLog = [];

// 调试日志函数
function log(message, isError = false) {
    const timestamp = new Date().toLocaleTimeString();
    const logMessage = `${timestamp}: ${message}`;
    console.log(logMessage);
    
    debugLog.push(logMessage);
    if (debugLog.length > 50) debugLog.shift();
    
    const logContainer = document.getElementById('log-container');
    if (logContainer) {
        const logEntry = document.createElement('div');
        logEntry.textContent = logMessage;
        if (isError) logEntry.style.color = 'red';
        logContainer.appendChild(logEntry);
        logContainer.scrollTop = logContainer.scrollHeight;
    }
    
    updateConnectionStatus(message, isError);
}

// 更新连接状态显示
function updateConnectionStatus(message, isError) {
    const statusElement = document.getElementById('connection-status');
    if (!statusElement) return;
    
    if (message.includes('连接已建立')) {
        statusElement.textContent = '已连接';
        statusElement.style.color = 'green';
    } else if (message.includes('连接断开') || message.includes('错误') || isError) {
        statusElement.textContent = '已断开，正在重连...';
        statusElement.style.color = 'red';
    } else if (message.includes('尝试连接')) {
        statusElement.textContent = '正在连接...';
        statusElement.style.color = 'orange';
    }
}

// 连接WebSocket
function connectWebSocket() {
    try {
        const host = window.location.hostname;
        const wsUrl = `ws://${host}:${wsPort}?session=${sessionId}`;
        log(`尝试连接WebSocket: ${wsUrl}`);
        
        socket = new WebSocket(wsUrl);
    
        socket.onopen = function(e) {
            log('WebSocket连接已建立');
            reconnectAttempts = 0; // 重置重连计数
        };
    
        socket.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                if (data.sessionId === sessionId) {
                    updateGameBoard(data.matrix);
                    updateLastUpdateTime(data.timestamp);
                }
            } catch (e) {
                log('解析数据错误: ' + e.message, true);
            }
        };
    
        socket.onclose = function(event) {
            if (event.wasClean) {
                log(`连接已关闭，代码=${event.code} 原因=${event.reason}`);
            } else {
                log('连接断开');
            }
            // 尝试重新连接，使用指数退避策略
            reconnectAttempts++;
            const delay = Math.min(30000, 1000 * Math.pow(1.5, reconnectAttempts)); // 最多30秒
            log(`将在 ${delay}ms 后尝试重新连接，这是第 ${reconnectAttempts} 次尝试`);
            setTimeout(connectWebSocket, delay);
        };
    
        socket.onerror = function(error) {
            log('WebSocket错误: ' + error, true);
            log('尝试备用连接方式...', true);
            tryAlternativeConnection();
        };
    } catch (e) {
        log('创建WebSocket连接时出错: ' + e.message, true);
        log('尝试备用连接方式...', true);
        tryAlternativeConnection();
    }
}

// 尝试备用连接方式
function tryAlternativeConnection() {
    try {
        const wsUrl = `ws://${localIpAddress}:${wsPort}?session=${sessionId}`;
        log(`尝试备用连接: ${wsUrl}`);
        
        socket = new WebSocket(wsUrl);
        socket.onopen = function(e) { log('备用连接已建立'); reconnectAttempts = 0; };
        socket.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                if (data.sessionId === sessionId) {
                    updateGameBoard(data.matrix);
                    updateLastUpdateTime(data.timestamp);
                }
            } catch (e) { log('解析数据错误: ' + e.message, true); }
        };
        socket.onclose = function(event) {
            log('备用连接断开');
            setTimeout(connectWebSocket, 3000);
        };
        socket.onerror = function(error) { log('备用连接错误: ' + error, true); };
    } catch (e) {
        log('创建备用连接时出错: ' + e.message, true);
        setTimeout(connectWebSocket, 3000);
    }
}

// 更新游戏板
function updateGameBoard(matrix) {
    if (!matrix) return;
    
    const board = document.getElementById('game-board');
    board.innerHTML = '';
    
    // 创建棋盘格子
    for (let r = 0; r < matrix.length; r++) {
        for (let c = 0; c < matrix[r].length; c++) {
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.setAttribute('data-row', r);
            cell.setAttribute('data-col', c);
            
            // 设置不同方块的类型
            const pieceId = matrix[r][c];
            if (pieceId > 0) {
                cell.classList.add('piece');
                cell.classList.add(`piece-${pieceId}`);
                cell.textContent = getPieceLabel(pieceId);
            } else {
                cell.classList.add('empty');
            }
            
            board.appendChild(cell);
        }
    }
    
    // 设置棋盘样式，基于矩阵尺寸
    board.style.gridTemplateRows = `repeat(${matrix.length}, 1fr)`;
    board.style.gridTemplateColumns = `repeat(${matrix[0].length}, 1fr)`;
}

// 获取方块标签
function getPieceLabel(pieceId) {
    switch(pieceId) {
        case 1: return '兵';
        case 2: return '横';
        case 3: return '竖';
        case 4: return '曹';
        default: return '';
    }
}

// 更新最后更新时间
function updateLastUpdateTime(timestamp) {
    lastUpdateTime = new Date(timestamp);
    const element = document.getElementById('last-update');
    element.textContent = lastUpdateTime.toLocaleTimeString();
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 注册调试按钮事件
    const toggleButton = document.getElementById('toggle-debug');
    const debugPanel = document.getElementById('debug-panel');
    if (toggleButton && debugPanel) {
        toggleButton.addEventListener('click', function() {
            if (debugPanel.style.display === 'none') {
                debugPanel.style.display = 'block';
                toggleButton.textContent = '隐藏调试信息';
            } else {
                debugPanel.style.display = 'none';
                toggleButton.textContent = '显示调试信息';
            }
        });
    }
    
    // 开始连接
    connectWebSocket();
});
