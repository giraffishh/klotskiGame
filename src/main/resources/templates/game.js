// 华容道游戏查看器JavaScript
let socket;
let lastUpdateTime = null;
let reconnectAttempts = 0;
let debugLog = [];
const MAX_RECONNECT_ATTEMPTS = 3; // 最大重连次数改为3次
let serverShutdown = false;  // 服务器关闭标志
let gameIsWon = false; // 游戏胜利标志

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

    if (gameIsWon) { // 游戏胜利状态优先显示
        statusElement.textContent = '游戏已胜利';
        statusElement.style.color = 'green'; // 更改为绿色
        return;
    }
    
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
    if (serverShutdown || reconnectAttempts > MAX_RECONNECT_ATTEMPTS || gameIsWon) {
        log('停止重连 (服务器关闭、达到最大重试次数或游戏已胜利)');
        if (!gameIsWon && serverShutdown) { // 只有在非胜利且服务器关闭时才显示游戏已关闭
            const gameBoard = document.getElementById('game-board');
            if (gameBoard) {
                gameBoard.innerHTML = '<div class="game-closed">游戏已关闭</div>';
            }
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

                // 检查是否是游戏胜利消息
                if (data.type === 'game_won' && data.sessionId === sessionId) {
                    log('收到游戏胜利通知: ' + data.message);
                    gameIsWon = true;
                    updateGameBoard(data.matrix); // 更新到最终胜利状态
                    updateGameStats(data.steps, data.gameTime, data.minSteps !== undefined ? data.minSteps : -1); // 更新最终统计数据
                    updateLastUpdateTime(data.timestamp);
                    updateConnectionStatus(data.message); // 更新状态为“游戏已胜利”
                    if (socket && socket.readyState === WebSocket.OPEN) {
                        socket.close(); // 服务器也会关闭，但客户端可以主动关闭
                    }
                    return;
                }
                
                // 检查是否是服务器关闭消息
                if (data.type === 'server_shutdown') {
                    if (!gameIsWon) { // 只有在游戏未胜利时才处理为标准关闭
                        serverShutdown = true;
                        log('收到服务器关闭通知: ' + data.message);
                        const gameBoard = document.getElementById('game-board');
                        if (gameBoard) {
                            gameBoard.innerHTML = '<div class="game-closed">游戏已关闭</div>';
                        }
                        if (socket && socket.readyState === WebSocket.OPEN) {
                            socket.close();
                        }
                    } else {
                        // 如果游戏已胜利，服务器关闭是预期的，保持胜利状态
                        log('服务器正在关闭，游戏已胜利，状态保持不变。');
                         if (socket && socket.readyState === WebSocket.OPEN) {
                            socket.close();
                        }
                    }
                    return;
                }
                
                if (data.sessionId === sessionId && !gameIsWon) { // 只有在游戏未胜利时才更新棋盘
                    updateGameBoard(data.matrix);
                    updateGameStats(data.steps, data.gameTime, data.minSteps !== undefined ? data.minSteps : -1);
                    updateLastUpdateTime(data.timestamp);
                }
            } catch (e) {
                log('解析数据错误: ' + e.message, true);
            }
        };
    
        socket.onclose = function(event) {
            if (gameIsWon) {
                log('游戏已胜利，WebSocket连接已按预期关闭。');
                // updateConnectionStatus 会确保显示 "游戏已胜利"
                // 不进行重连或更改棋盘
                return;
            }

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
                if (!gameIsWon) { // 只有在游戏未胜利时才显示此消息
                    const gameBoard = document.getElementById('game-board');
                    if (gameBoard) {
                        gameBoard.innerHTML = '<div class="game-closed">游戏可能已关闭<br>请关闭此页面</div>';
                    }
                }
            }
        };
    
        socket.onerror = function(error) {
            log('WebSocket错误: ' + error, true);
            if (!serverShutdown && reconnectAttempts < MAX_RECONNECT_ATTEMPTS && !gameIsWon) {
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
    if (serverShutdown || reconnectAttempts > MAX_RECONNECT_ATTEMPTS || gameIsWon) {
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
                if (data.type === 'game_won' && data.sessionId === sessionId) {
                    log('收到游戏胜利通知 (备用连接): ' + data.message);
                    gameIsWon = true;
                    updateGameBoard(data.matrix);
                    updateGameStats(data.steps, data.gameTime, data.minSteps !== undefined ? data.minSteps : -1);
                    updateLastUpdateTime(data.timestamp);
                    updateConnectionStatus(data.message);
                     if (socket && socket.readyState === WebSocket.OPEN) {
                        socket.close();
                    }
                    return;
                }

                if (data.type === 'server_shutdown') {
                    if (!gameIsWon) {
                        serverShutdown = true;
                        log('收到服务器关闭通知 (备用连接): ' + data.message);
                        const gameBoard = document.getElementById('game-board');
                        if (gameBoard) {
                            gameBoard.innerHTML = '<div class="game-closed">游戏已关闭</div>';
                        }
                         if (socket && socket.readyState === WebSocket.OPEN) {
                            socket.close();
                        }
                    } else {
                        log('服务器正在关闭 (备用连接)，游戏已胜利，状态保持不变。');
                         if (socket && socket.readyState === WebSocket.OPEN) {
                            socket.close();
                        }
                    }
                    return;
                }
                
                if (data.sessionId === sessionId && !gameIsWon) {
                    updateGameBoard(data.matrix);
                    updateGameStats(data.steps, data.gameTime, data.minSteps !== undefined ? data.minSteps : -1);
                    updateLastUpdateTime(data.timestamp);
                }
            } catch (e) { log('解析数据错误: ' + e.message, true); }
        };
        socket.onclose = function(event) {
            if (gameIsWon) {
                log('游戏已胜利 (备用连接)，WebSocket连接已按预期关闭。');
                return;
            }
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
    if (!matrix || matrix.length === 0) return;

    const board = document.getElementById('game-board');
    board.innerHTML = ''; // 清空棋盘

    const rows = matrix.length;
    const cols = matrix[0].length;
    const processedCells = Array(rows).fill(null).map(() => Array(cols).fill(false));

    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            if (processedCells[r][c]) {
                continue; // 这个单元格已经被某个棋子覆盖
            }

            const pieceId = matrix[r][c];
            const cell = document.createElement('div');
            cell.className = 'cell'; // 基本样式
            cell.setAttribute('data-row', r);
            cell.setAttribute('data-col', c);

            cell.style.gridRowStart = r + 1;
            cell.style.gridColumnStart = c + 1;

            if (pieceId > 0) { // 是一个棋子
                let blockWidth = 1;
                let blockHeight = 1;

                // 根据 pieceId 确定棋子尺寸
                // 这是基于华容道标准棋子类型的常见假设
                // pieceId 1: 1x1 (兵)
                // pieceId 2: 2x1 (横将/横块)
                // pieceId 3: 1x2 (竖将/竖块)
                // pieceId 4: 2x2 (曹操/大方块)
                switch (pieceId) {
                    case 1: // 1x1
                        blockWidth = 1;
                        blockHeight = 1;
                        break;
                    case 2: // 2x1 水平
                        blockWidth = 2;
                        blockHeight = 1;
                        break;
                    case 3: // 1x2 垂直
                        blockWidth = 1;
                        blockHeight = 2;
                        break;
                    case 4: // 2x2
                        blockWidth = 2;
                        blockHeight = 2;
                        break;
                    default:
                        // 对于未知 pieceId，默认为 1x1
                        blockWidth = 1;
                        blockHeight = 1;
                        console.warn(`Unknown pieceId ${pieceId} at (${r},${c}), defaulting to 1x1.`);
                        break;
                }
                
                // 边界检查，以防棋子数据超出棋盘范围
                if (r + blockHeight > rows) blockHeight = rows - r;
                if (c + blockWidth > cols) blockWidth = cols - c;


                cell.style.gridRowEnd = `span ${blockHeight}`;
                cell.style.gridColumnEnd = `span ${blockWidth}`;
                
                cell.classList.add('piece', `piece-${pieceId}`);
                cell.textContent = getPieceLabel(pieceId);

                // 标记此棋子覆盖的所有单元格为已处理
                for (let i = 0; i < blockHeight; i++) {
                    for (let j = 0; j < blockWidth; j++) {
                        if (r + i < rows && c + j < cols) {
                            processedCells[r + i][c + j] = true;
                        }
                    }
                }
            } else { // 是空格
                cell.style.gridRowEnd = 'span 1';
                cell.style.gridColumnEnd = 'span 1';
                cell.classList.add('empty');
                processedCells[r][c] = true;
            }
            board.appendChild(cell);
        }
    }

    // 设置棋盘网格的行列定义
    board.style.gridTemplateRows = `repeat(${rows}, 1fr)`;
    board.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;
}

// 更新游戏统计信息
function updateGameStats(steps, gameTimeMillis, minSteps) {
    const stepsElement = document.getElementById('game-steps');
    const timeElement = document.getElementById('game-time');
    const minStepsElement = document.getElementById('game-min-steps');

    if (stepsElement) {
        stepsElement.textContent = steps !== undefined ? steps : '0';
    }
    if (timeElement) {
        timeElement.textContent = formatGameTime(gameTimeMillis !== undefined ? gameTimeMillis : 0);
    }
    if (minStepsElement) {
        if (minSteps !== undefined && minSteps >= 0) {
            minStepsElement.textContent = minSteps;
        } else {
            minStepsElement.textContent = '--';
        }
    }
}

// 格式化游戏时间 (毫秒 -> MM:SS.ss)
function formatGameTime(millis) {
    if (millis === undefined || millis === null) return '00:00.00';
    let totalSeconds = Math.floor(millis / 1000);
    let centiseconds = Math.floor((millis % 1000) / 10); // 取厘秒 (两位)
    let minutes = Math.floor(totalSeconds / 60);
    let seconds = totalSeconds % 60;

    const pad = (num, size = 2) => num.toString().padStart(size, '0');

    return `${pad(minutes)}:${pad(seconds)}.${pad(centiseconds)}`;
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
