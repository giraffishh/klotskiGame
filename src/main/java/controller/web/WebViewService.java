package controller.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.MapModel;

/**
 * 提供本地网页视图服务，将华容道游戏布局实时显示在网页上
 */
public class WebViewService {
    private static WebViewService instance;
    private int httpPort = 8080; // 改为实例变量，允许端口配置
    private int wsPort = 8081;   // 改为实例变量，允许端口配置
    private HttpServer httpServer;
    private GameWebSocketServer wsServer;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private String localIpAddress; // 存储本机IP地址
    
    // 存储会话ID到游戏模型的映射
    private final Map<String, MapModel> sessionModels = new HashMap<>();
    
    // 单例访问
    public static WebViewService getInstance() {
        if (instance == null) {
            instance = new WebViewService();
        }
        return instance;
    }
    
    private WebViewService() {
        try {
            // 获取本机IP地址
            localIpAddress = getLocalIpAddress();
            System.out.println("本机IP地址: " + localIpAddress);
            
            // 使用随机端口初始化服务器
            boolean serverStarted = false;
            int maxAttempts = 15; // 最多尝试15次
            int attempt = 0;
            
            while (!serverStarted && attempt < maxAttempts) {
                httpPort = findAvailablePort(8000, 9000);
                wsPort = findAvailablePort(httpPort + 1, 9001);
                
                try {
                    System.out.println("尝试启动服务器，HTTP端口: " + httpPort + ", WebSocket端口: " + wsPort);
                    
                    // 启动HTTP服务器
                    httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
                    httpServer.createContext("/", new MainHandler());
                    httpServer.createContext("/status", new StatusHandler()); // 添加状态检查API
                    httpServer.createContext("/template", new TemplateHandler());
                    httpServer.setExecutor(Executors.newCachedThreadPool());
                    httpServer.start();
                    
                    // 启动WebSocket服务器
                    wsServer = new GameWebSocketServer(wsPort);
                    wsServer.start();
                    
                    serverStarted = true;
                    System.out.println("=== 华容道网页查看服务已启动 ===");
                    System.out.println("HTTP服务 (本地): http://localhost:" + httpPort);
                    System.out.println("HTTP服务 (网络): http://" + localIpAddress + ":" + httpPort);
                    System.out.println("WebSocket服务: 端口 " + wsPort);
                    System.out.println("状态检查API: http://localhost:" + httpPort + "/status");
                    System.out.println("==============================");
                } catch (IOException e) {
                    attempt++;
                    System.err.println("无法启动服务，原因: " + e.getMessage());
                    // 关闭可能已部分创建的服务器
                    if (httpServer != null) {
                        httpServer.stop(0);
                        httpServer = null;
                    }
                    if (wsServer != null) {
                        try {
                            wsServer.stop();
                            wsServer = null;
                        } catch (InterruptedException ex) {
                            System.err.println("停止WebSocket服务器时出错: " + ex.getMessage());
                        }
                    }
                }
            }
            
            if (!serverStarted) {
                System.err.println("无法启动网页服务 - 请检查网络连接和防火墙设置");
            }
        } catch (Exception e) {
            System.err.println("初始化网页服务时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查指定端口是否可用
     */
    private boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false; // 端口已被占用
        } catch (IOException e) {
            return true; // 端口可用
        }
    }
    
    /**
     * 在指定范围内找一个可用端口
     */
    private int findAvailablePort(int minPort, int maxPort) {
        Random random = new Random();
        int range = maxPort - minPort;
        int[] ports = new int[range];
        
        // 创建随机顺序的端口数组
        for (int i = 0; i < range; i++) {
            ports[i] = minPort + i;
        }
        
        // 洗牌算法
        for (int i = range - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = ports[index];
            ports[index] = ports[i];
            ports[i] = temp;
        }
        
        // 按随机顺序尝试端口
        for (int port : ports) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        
        return minPort; // 如果所有端口都不可用，返回最小端口（可能会失败）
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalIpAddress() {
        try {
            // 优先获取非回环且已启用的IPv4地址
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 过滤掉回环接口、虚拟接口以及未开启的接口
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // 过滤IPv6地址
                    if (addr.getHostAddress().contains(":")) {
                        continue;
                    }
                    return addr.getHostAddress();
                }
            }
            
            // 如果没有找到合适的地址，使用localhost地址
            return "127.0.0.1";
        } catch (Exception e) {
            System.err.println("获取本机IP地址时出错: " + e.getMessage());
            return "127.0.0.1";
        }
    }
    
    /**
     * 创建新的游戏会话并返回唯一URL
     * @param model 游戏模型
     * @return 唯一会话URL
     */
    public String createGameSession(MapModel model) {
        if (httpServer == null) {
            return "网页服务未启动，无法创建会话";
        }
        
        try {
            // 使用时间戳创建唯一会话ID
            String sessionId = generateSessionId();
            sessionModels.put(sessionId, model);
            
            // 返回多个可能的URL
            StringBuilder urls = new StringBuilder();
            urls.append("本地访问: http://localhost:").append(httpPort).append("/?session=").append(sessionId).append("\n");
            urls.append("网络访问: http://").append(localIpAddress).append(":").append(httpPort).append("/?session=").append(sessionId);
            
            return urls.toString();
        } catch (Exception e) {
            System.err.println("创建游戏会话时出错: " + e.getMessage());
            return "创建会话时出错: " + e.getMessage();
        }
    }
    
    /**
     * 更新特定会话的游戏模型
     * @param sessionId 会话ID
     * @param model 游戏模型
     */
    public void updateGameSession(String sessionId, MapModel model) {
        if (wsServer == null) {
            System.err.println("WebSocket服务未启动，无法更新会话");
            return;
        }
        
        try {
            sessionModels.put(sessionId, model);
            broadcastUpdate(sessionId, model);
        } catch (Exception e) {
            System.err.println("更新游戏会话时出错: " + e.getMessage());
        }
    }
    
    /**
     * 通过WebSocket广播游戏更新
     */
    private void broadcastUpdate(String sessionId, MapModel model) {
        if (wsServer == null || wsServer.getConnections().isEmpty()) {
            return; // 没有活动连接，不需要广播
        }
        
        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("sessionId", sessionId);
            updateData.put("matrix", model.getMatrix());
            updateData.put("timestamp", System.currentTimeMillis());
            
            String jsonUpdate = jsonMapper.writeValueAsString(updateData);
            wsServer.broadcast(jsonUpdate);
        } catch (Exception e) {
            System.err.println("广播更新时出错: " + e.getMessage());
        }
    }
    
    /**
     * 生成唯一会话ID
     */
    private String generateSessionId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        return sdf.format(new Date()) + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * 停止服务
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            System.out.println("HTTP服务已停止");
        }
        if (wsServer != null) {
            try {
                wsServer.stop();
                wsServer = null;
                System.out.println("WebSocket服务已停止");
            } catch (InterruptedException e) {
                System.err.println("停止WebSocket服务器时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理状态检查API请求
     */
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"running\",\"httpPort\":" + httpPort + ",\"wsPort\":" + wsPort + "}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    /**
     * 处理主页请求
     */
    private class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String sessionId = null;
            
            // 记录访问日志
            System.out.println("收到HTTP请求: " + uri);
            
            // 解析查询参数获取会话ID
            if (query != null && query.contains("session=")) {
                sessionId = query.split("session=")[1];
                if (sessionId.contains("&")) {
                    sessionId = sessionId.split("&")[0];
                }
            }
            
            String response;
            if (sessionId != null && sessionModels.containsKey(sessionId)) {
                // 返回有会话ID的游戏视图
                response = getGameViewHtml(sessionId);
            } else {
                // 返回无会话的主页
                response = getIndexHtml();
            }
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
    
    /**
     * 处理模板资源请求
     */
    private class TemplateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            
            // 记录访问日志
            System.out.println("收到模板请求: " + path);
            
            // 只处理特定的模板请求
            if (path.equals("/template/game.js")) {
                String js = getGameJs();
                exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=utf-8");
                byte[] responseBytes = js.getBytes("UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            } 
            else if (path.equals("/template/styles.css")) {
                String css = getGameCss();
                exchange.getResponseHeaders().set("Content-Type", "text/css; charset=utf-8");
                byte[] responseBytes = css.getBytes("UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
            else {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
            }
        }
    }
    
    /**
     * WebSocket服务器实现
     */
    private class GameWebSocketServer extends WebSocketServer {
        
        public GameWebSocketServer(int port) {
            super(new InetSocketAddress(port));
            // 增加连接超时设置，提高稳定性
            setConnectionLostTimeout(60); // 60秒超时
        }
        
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("WebSocket新连接: " + conn.getRemoteSocketAddress());
            
            // 获取查询参数
            String query = handshake.getResourceDescriptor();
            String sessionId = null;
            
            if (query != null && query.contains("?session=")) {
                sessionId = query.split("session=")[1];
                if (sessionId.contains("&")) {
                    sessionId = sessionId.split("&")[0];
                }
                System.out.println("WebSocket连接请求会话ID: " + sessionId);
            }
            
            // 如果是有效会话，则发送初始状态
            if (sessionId != null && sessionModels.containsKey(sessionId)) {
                try {
                    MapModel model = sessionModels.get(sessionId);
                    Map<String, Object> initialData = new HashMap<>();
                    initialData.put("sessionId", sessionId);
                    initialData.put("matrix", model.getMatrix());
                    initialData.put("timestamp", System.currentTimeMillis());
                    
                    String jsonData = jsonMapper.writeValueAsString(initialData);
                    conn.send(jsonData);
                    System.out.println("已发送初始游戏数据到WebSocket客户端");
                } catch (Exception e) {
                    System.err.println("发送初始状态时出错: " + e.getMessage());
                }
            } else {
                System.out.println("WebSocket连接的会话ID无效或不存在: " + sessionId);
            }
        }
        
        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("WebSocket连接关闭: " + conn.getRemoteSocketAddress() + 
                            " 代码: " + code + " 原因: " + reason + " 远程关闭: " + remote);
        }
        
        @Override
        public void onMessage(WebSocket conn, String message) {
            // 客户端消息处理（目前只接收不处理）
            System.out.println("收到WebSocket消息: " + message);
        }
        
        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("WebSocket错误: " + (conn != null ? conn.getRemoteSocketAddress() : "未知连接"));
            System.err.println("错误详情: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        @Override
        public void onStart() {
            System.out.println("WebSocket服务器已启动，监听端口: " + getPort());
        }
    }
    
    // HTML模板方法
    private String getIndexHtml() {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>华容道游戏查看器</title>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; margin: 20px; text-align: center; }\n" +
               "        h1 { color: #333; }\n" +
               "        .message { margin: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 5px; }\n" +
               "        .footer { margin-top: 50px; font-size: 12px; color: #888; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <h1>华容道游戏查看器</h1>\n" +
               "    <div class=\"message\">\n" +
               "        <p>请从游戏客户端访问特定会话链接。</p>\n" +
               "        <p>每次开始新游戏时，会自动生成会话链接。</p>\n" +
               "        <p>服务器状态: <a href=\"/status\" target=\"_blank\">检查状态</a></p>\n" +
               "    </div>\n" +
               "    <div class=\"footer\">\n" +
               "        <p>服务器端口: HTTP " + httpPort + " / WS " + wsPort + "</p>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }
    
    private String getGameViewHtml(String sessionId) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>华容道游戏 - 会话 " + sessionId + "</title>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <link rel=\"stylesheet\" href=\"/template/styles.css\">\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"container\">\n" +
               "        <h1>华容道游戏查看器</h1>\n" +
               "        <div class=\"session-info\">会话ID: <span id=\"session-id\">" + sessionId + "</span></div>\n" +
               "        <div class=\"connection-status\">连接状态: <span id=\"connection-status\">正在连接...</span></div>\n" +
               "        <div class=\"last-update\">最后更新: <span id=\"last-update\">加载中...</span></div>\n" +
               "        <div class=\"game-board\" id=\"game-board\">\n" +
               "            <div class=\"loading\">加载游戏数据中...</div>\n" +
               "        </div>\n" +
               "        <div class=\"debug-info\">\n" +
               "            <button id=\"toggle-debug\">显示调试信息</button>\n" +
               "            <div id=\"debug-panel\" style=\"display:none; margin-top:10px; text-align:left; padding:10px; background:#f8f8f8; border-radius:5px;\">\n" +
               "                <p>WebSocket端口: <span id=\"ws-port\">" + wsPort + "</span></p>\n" +
               "                <p>连接URL: <span id=\"ws-url\">ws://" + localIpAddress + ":" + wsPort + "?session=" + sessionId + "</span></p>\n" +
               "                <div id=\"log-container\" style=\"max-height:200px; overflow-y:auto; background:#eee; padding:5px;\"></div>\n" +
               "            </div>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "    <script>\n" +
               "        // 传递会话ID给JavaScript\n" +
               "        const sessionId = \"" + sessionId + "\";\n" +
               "        const wsPort = " + wsPort + ";\n" +
               "        const localIpAddress = \"" + localIpAddress + "\";\n" +
               "    </script>\n" +
               "    <script src=\"/template/game.js\"></script>\n" +
               "</body>\n" +
               "</html>";
    }
    
    private String getGameJs() {
        return "// 华容道游戏查看器JavaScript\n" +
               "let socket;\n" +
               "let lastUpdateTime = null;\n" +
               "let reconnectAttempts = 0;\n" +
               "let debugLog = [];\n" +
               "\n" +
               "// 调试日志函数\n" +
               "function log(message, isError = false) {\n" +
               "    const timestamp = new Date().toLocaleTimeString();\n" +
               "    const logMessage = `${timestamp}: ${message}`;\n" +
               "    console.log(logMessage);\n" +
               "    \n" +
               "    // 添加到调试日志数组\n" +
               "    debugLog.push(logMessage);\n" +
               "    if (debugLog.length > 50) debugLog.shift(); // 保持日志不超过50条\n" +
               "    \n" +
               "    // 更新UI中的日志显示\n" +
               "    const logContainer = document.getElementById('log-container');\n" +
               "    if (logContainer) {\n" +
               "        const logEntry = document.createElement('div');\n" +
               "        logEntry.textContent = logMessage;\n" +
               "        if (isError) logEntry.style.color = 'red';\n" +
               "        logContainer.appendChild(logEntry);\n" +
               "        logContainer.scrollTop = logContainer.scrollHeight; // 自动滚动到底部\n" +
               "    }\n" +
               "    \n" +
               "    // 更新连接状态\n" +
               "    updateConnectionStatus(message, isError);\n" +
               "}\n" +
               "\n" +
               "// 更新连接状态显示\n" +
               "function updateConnectionStatus(message, isError) {\n" +
               "    const statusElement = document.getElementById('connection-status');\n" +
               "    if (!statusElement) return;\n" +
               "    \n" +
               "    if (message.includes('连接已建立')) {\n" +
               "        statusElement.textContent = '已连接';\n" +
               "        statusElement.style.color = 'green';\n" +
               "    } else if (message.includes('连接断开') || message.includes('错误') || isError) {\n" +
               "        statusElement.textContent = '已断开，正在重连...';\n" +
               "        statusElement.style.color = 'red';\n" +
               "    } else if (message.includes('尝试连接')) {\n" +
               "        statusElement.textContent = '正在连接...';\n" +
               "        statusElement.style.color = 'orange';\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "// 连接WebSocket\n" +
               "function connectWebSocket() {\n" +
               "    try {\n" +
               "        // 尝试使用动态主机名\n" +
               "        const host = window.location.hostname;\n" +
               "        const wsUrl = `ws://${host}:${wsPort}?session=${sessionId}`;\n" +
               "        log(`尝试连接WebSocket: ${wsUrl}`);\n" +
               "        \n" +
               "        socket = new WebSocket(wsUrl);\n" +
               "    \n" +
               "        socket.onopen = function(e) {\n" +
               "            log('WebSocket连接已建立');\n" +
               "            reconnectAttempts = 0; // 重置重连计数\n" +
               "        };\n" +
               "    \n" +
               "        socket.onmessage = function(event) {\n" +
               "            try {\n" +
               "                const data = JSON.parse(event.data);\n" +
               "                if (data.sessionId === sessionId) {\n" +
               "                    updateGameBoard(data.matrix);\n" +
               "                    updateLastUpdateTime(data.timestamp);\n" +
               "                }\n" +
               "            } catch (e) {\n" +
               "                log('解析数据错误: ' + e.message, true);\n" +
               "            }\n" +
               "        };\n" +
               "    \n" +
               "        socket.onclose = function(event) {\n" +
               "            if (event.wasClean) {\n" +
               "                log(`连接已关闭，代码=${event.code} 原因=${event.reason}`);\n" +
               "            } else {\n" +
               "                log('连接断开');\n" +
               "            }\n" +
               "            // 尝试重新连接，使用指数退避策略\n" +
               "            reconnectAttempts++;\n" +
               "            const delay = Math.min(30000, 1000 * Math.pow(1.5, reconnectAttempts)); // 最多30秒\n" +
               "            log(`将在 ${delay}ms 后尝试重新连接，这是第 ${reconnectAttempts} 次尝试`);\n" +
               "            setTimeout(connectWebSocket, delay);\n" +
               "        };\n" +
               "    \n" +
               "        socket.onerror = function(error) {\n" +
               "            log('WebSocket错误: ' + error, true);\n" +
               "            log('尝试备用连接方式...', true);\n" +
               "            tryAlternativeConnection();\n" +
               "        };\n" +
               "    } catch (e) {\n" +
               "        log('创建WebSocket连接时出错: ' + e.message, true);\n" +
               "        log('尝试备用连接方式...', true);\n" +
               "        tryAlternativeConnection();\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "// 尝试备用连接方式\n" +
               "function tryAlternativeConnection() {\n" +
               "    try {\n" +
               "        // 如果使用主机名连接失败，尝试使用IP地址\n" +
               "        const wsUrl = `ws://${localIpAddress}:${wsPort}?session=${sessionId}`;\n" +
               "        log(`尝试备用连接: ${wsUrl}`);\n" +
               "        \n" +
               "        socket = new WebSocket(wsUrl);\n" +
               "        // 设置相同的事件处理程序\n" +
               "        socket.onopen = function(e) { log('备用连接已建立'); reconnectAttempts = 0; };\n" +
               "        socket.onmessage = function(event) {\n" +
               "            try {\n" +
               "                const data = JSON.parse(event.data);\n" +
               "                if (data.sessionId === sessionId) {\n" +
               "                    updateGameBoard(data.matrix);\n" +
               "                    updateLastUpdateTime(data.timestamp);\n" +
               "                }\n" +
               "            } catch (e) { log('解析数据错误: ' + e.message, true); }\n" +
               "        };\n" +
               "        socket.onclose = function(event) {\n" +
               "            log('备用连接断开');\n" +
               "            setTimeout(connectWebSocket, 3000); // 在失败后回到主连接方式\n" +
               "        };\n" +
               "        socket.onerror = function(error) { log('备用连接错误: ' + error, true); };\n" +
               "    } catch (e) {\n" +
               "        log('创建备用连接时出错: ' + e.message, true);\n" +
               "        setTimeout(connectWebSocket, 3000); // 在失败后回到主连接方式\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "// 更新游戏板\n" +
               "function updateGameBoard(matrix) {\n" +
               "    if (!matrix) return;\n" +
               "    \n" +
               "    const board = document.getElementById('game-board');\n" +
               "    board.innerHTML = '';\n" +
               "    \n" +
               "    // 创建棋盘格子\n" +
               "    for (let r = 0; r < matrix.length; r++) {\n" +
               "        for (let c = 0; c < matrix[r].length; c++) {\n" +
               "            const cell = document.createElement('div');\n" +
               "            cell.className = 'cell';\n" +
               "            cell.setAttribute('data-row', r);\n" +
               "            cell.setAttribute('data-col', c);\n" +
               "            \n" +
               "            // 设置不同方块的类型\n" +
               "            const pieceId = matrix[r][c];\n" +
               "            if (pieceId > 0) {\n" +
               "                cell.classList.add('piece');\n" +
               "                cell.classList.add(`piece-${pieceId}`);\n" +
               "                cell.textContent = getPieceLabel(pieceId);\n" +
               "            } else {\n" +
               "                cell.classList.add('empty');\n" +
               "            }\n" +
               "            \n" +
               "            board.appendChild(cell);\n" +
               "        }\n" +
               "    }\n" +
               "    \n" +
               "    // 设置棋盘样式，基于矩阵尺寸\n" +
               "    board.style.gridTemplateRows = `repeat(${matrix.length}, 1fr)`;\n" +
               "    board.style.gridTemplateColumns = `repeat(${matrix[0].length}, 1fr)`;\n" +
               "}\n" +
               "\n" +
               "// 获取方块标签\n" +
               "function getPieceLabel(pieceId) {\n" +
               "    switch(pieceId) {\n" +
               "        case 1: return '兵';\n" +
               "        case 2: return '横';\n" +
               "        case 3: return '竖';\n" +
               "        case 4: return '曹';\n" +
               "        default: return '';\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "// 更新最后更新时间\n" +
               "function updateLastUpdateTime(timestamp) {\n" +
               "    lastUpdateTime = new Date(timestamp);\n" +
               "    const element = document.getElementById('last-update');\n" +
               "    element.textContent = lastUpdateTime.toLocaleTimeString();\n" +
               "}\n" +
               "\n" +
               "// 页面加载完成后初始化\n" +
               "document.addEventListener('DOMContentLoaded', function() {\n" +
               "    // 注册调试按钮事件\n" +
               "    const toggleButton = document.getElementById('toggle-debug');\n" +
               "    const debugPanel = document.getElementById('debug-panel');\n" +
               "    if (toggleButton && debugPanel) {\n" +
               "        toggleButton.addEventListener('click', function() {\n" +
               "            if (debugPanel.style.display === 'none') {\n" +
               "                debugPanel.style.display = 'block';\n" +
               "                toggleButton.textContent = '隐藏调试信息';\n" +
               "            } else {\n" +
               "                debugPanel.style.display = 'none';\n" +
               "                toggleButton.textContent = '显示调试信息';\n" +
               "            }\n" +
               "        });\n" +
               "    }\n" +
               "    \n" +
               "    // 开始连接\n" +
               "    connectWebSocket();\n" +
               "});\n";
    }
    
    private String getGameCss() {
        return "/* 华容道游戏查看器样式 */\n" +
               "body {\n" +
               "    font-family: Arial, sans-serif;\n" +
               "    margin: 0;\n" +
               "    padding: 20px;\n" +
               "    background-color: #f5f5f5;\n" +
               "}\n" +
               "\n" +
               ".container {\n" +
               "    max-width: 800px;\n" +
               "    margin: 0 auto;\n" +
               "    text-align: center;\n" +
               "}\n" +
               "\n" +
               "h1 {\n" +
               "    color: #333;\n" +
               "    margin-bottom: 20px;\n" +
               "}\n" +
               "\n" +
               ".session-info {\n" +
               "    font-size: 14px;\n" +
               "    color: #666;\n" +
               "    margin-bottom: 10px;\n" +
               "}\n" +
               "\n" +
               ".last-update {\n" +
               "    font-size: 14px;\n" +
               "    color: #666;\n" +
               "    margin-bottom: 20px;\n" +
               "}\n" +
               "\n" +
               ".game-board {\n" +
               "    display: grid;\n" +
               "    grid-gap: 5px;\n" +
               "    background-color: #4a4a4a;\n" +
               "    padding: 5px;\n" +
               "    border-radius: 5px;\n" +
               "    max-width: 600px;\n" +
               "    margin: 0 auto;\n" +
               "    aspect-ratio: 4/5;\n" +
               "}\n" +
               "\n" +
               ".cell {\n" +
               "    border-radius: 4px;\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    justify-content: center;\n" +
               "    font-weight: bold;\n" +
               "    font-size: 24px;\n" +
               "}\n" +
               "\n" +
               ".empty {\n" +
               "    background-color: #222;\n" +
               "}\n" +
               "\n" +
               ".piece {\n" +
               "    background-color: #e74c3c;\n" +
               "    box-shadow: 0 3px 5px rgba(0,0,0,0.2);\n" +
               "    color: white;\n" +
               "}\n" +
               "\n" +
               ".piece-1 {\n" +
               "    background-color: #f39c12;\n" +
               "}\n" +
               "\n" +
               ".piece-2 {\n" +
               "    background-color: #9b59b6;\n" +
               "}\n" +
               "\n" +
               ".piece-3 {\n" +
               "    background-color: #3498db;\n" +
               "}\n" +
               "\n" +
               ".piece-4 {\n" +
               "    background-color: #2ecc71;\n" +
               "}\n" +
               "\n" +
               ".loading {\n" +
               "    grid-column: span 4;\n" +
               "    grid-row: span 5;\n" +
               "    display: flex;\n" +
               "    align-items: center;\n" +
               "    justify-content: center;\n" +
               "    color: white;\n" +
               "    font-size: 18px;\n" +
               "}\n";
    }
}
