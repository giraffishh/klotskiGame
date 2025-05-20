// 华容道游戏查看器JavaScript
let socket;
let lastUpdateTime = null;
let reconnectAttempts = 0;
let debugLog = [];
const MAX_RECONNECT_ATTEMPTS = 3; // 最大重连次数改为3次
let serverShutdown = false;  // 服务器关闭标志

// 调试日志函数
function log(message, isError = false) {
    const timestamp = new Date().toLocaleTimeString();
    const logMessage = `${timestamp}: ${message}`;
    console.log(logMessage);
    
    debugLog.push(logMessage);
    if (debugLog.length > 50) debugLog.shift();
    
    updateConnectionStatus(message, isError);
}

// 更新连接状态显示
function updateConnectionStatus(message, isError) {
    const statusElement = document.getElementById('connection-status');
    if (!statusElement) return;
    
    if (serverShutdown) {
        statusElement.textContent = '游戏已关闭';
        statusElement.style.color = '#999';
        return;
    }
    
    if (message.includes('连接已建立')) {
        statusElement.textContent = '已连接';
        statusElement.style.color = 'green';
    } else if (message.includes('连接断开') || message.includes('错误') || isError) {
        if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
            statusElement.textContent = '游戏可能已关闭，请关闭此页面';
            statusElement.style.color = '#999';
        } else {
            statusElement.textContent = '已断开，正在重连...';
            statusElement.style.color = 'red';
        }
    } else if (message.includes('尝试连接')) {
        statusElement.textContent = '正在连接...';
        statusElement.style.color = 'orange';
    }
}

// 连接WebSocket
function connectWebSocket() {
    // 如果服务器已关闭或超过最大重试次数，不再尝试连接
    if (serverShutdown || reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
        log('游戏可能已关闭，停止重连');
        const gameBoard = document.getElementById('game-board');
        if (gameBoard) {
            gameBoard.innerHTML = '<div class="game-closed">游戏已关闭</div>';
        }
        return;
    }
    
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
                
                // 检查是否是服务器关闭消息
                if (data.type === 'server_shutdown') {
                    serverShutdown = true;
                    log('收到服务器关闭通知: ' + data.message);
                    const gameBoard = document.getElementById('game-board');
                    if (gameBoard) {
                        gameBoard.innerHTML = '<div class="game-closed">游戏已关闭</div>';
                    }
                    socket.close();
                    return;
                }
                
                if (data.sessionId === sessionId) {
                    updateGameBoard(data.matrix);
                    updateLastUpdateTime(data.timestamp);
                }
            } catch (e) {
                log('解析数据错误: ' + e.message, true);
            }
        };
    
        socket.onclose = function(event) {
            if (serverShutdown) {
                log('服务器已关闭，不再重连');
                return;
            }
            
            if (event.wasClean) {
                log(`连接已关闭，代码=${event.code} 原因=${event.reason}`);
            } else {
                log('连接断开');
            }
            
            // 尝试重新连接，使用指数退避策略，但有最大次数限制
            reconnectAttempts++;
            if (reconnectAttempts <= MAX_RECONNECT_ATTEMPTS) {
                const delay = Math.min(30000, 1000 * Math.pow(1.5, reconnectAttempts)); // 最多30秒
                log(`将在 ${delay}ms 后尝试重新连接，这是第 ${reconnectAttempts} 次尝试`);
                setTimeout(connectWebSocket, delay);
            } else {
                log('达到最大重连次数，游戏可能已关闭');
                const gameBoard = document.getElementById('game-board');
                if (gameBoard) {
                    gameBoard.innerHTML = '<div class="game-closed">游戏可能已关闭<br>请关闭此页面</div>';
                }
            }
        };
    
        socket.onerror = function(error) {
            log('WebSocket错误: ' + error, true);
            if (!serverShutdown && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                log('尝试备用连接方式...', true);
                tryAlternativeConnection();
            }
        };
    } catch (e) {
        log('创建WebSocket连接时出错: ' + e.message, true);
        if (!serverShutdown && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            log('尝试备用连接方式...', true);
            tryAlternativeConnection();
        }
    }
}

// 尝试备用连接方式
function tryAlternativeConnection() {
    if (serverShutdown || reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
        log('停止尝试备用连接');
        return;
    }
    
    try {
        const wsUrl = `ws://${localIpAddress}:${wsPort}?session=${sessionId}`;
        log(`尝试备用连接: ${wsUrl}`);
        
        socket = new WebSocket(wsUrl);
        socket.onopen = function(e) { log('备用连接已建立'); reconnectAttempts = 0; };
        socket.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                if (data.type === 'server_shutdown') {
                    serverShutdown = true;
                    log('收到服务器关闭通知: ' + data.message);
                    const gameBoard = document.getElementById('game-board');
                    if (gameBoard) {
                        gameBoard.innerHTML = '<div class="game-closed">游戏已关闭</div>';
                    }
                    socket.close();
                    return;
                }
                
                if (data.sessionId === sessionId) {
                    updateGameBoard(data.matrix);
                    updateLastUpdateTime(data.timestamp);
                }
            } catch (e) { log('解析数据错误: ' + e.message, true); }
        };
        socket.onclose = function(event) {
            if (serverShutdown) return;
            log('备用连接断开');
            setTimeout(connectWebSocket, 3000);
        };
        socket.onerror = function(error) { log('备用连接错误: ' + error, true); };
    } catch (e) {
        log('创建备用连接时出错: ' + e.message, true);
        if (!serverShutdown) {
            setTimeout(connectWebSocket, 3000);
        }
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
    // 开始连接
    connectWebSocket();
    
    // 添加页面关闭事件处理
    window.addEventListener('beforeunload', function() {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.close();
        }
    });
});
