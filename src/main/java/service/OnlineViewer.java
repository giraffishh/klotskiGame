package service;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Scanner;
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
public class OnlineViewer {
    private static OnlineViewer instance;
    private int httpPort = 8080;
    private int wsPort = 8081;
    private HttpServer httpServer;
    private GameWebSocketServer wsServer;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private String localIpAddress;
    
    // 存储会话ID到游戏模型的映射
    private final Map<String, MapModel> sessionModels = new HashMap<>();
    
    // 单例访问
    public static OnlineViewer getInstance() {
        if (instance == null) {
            instance = new OnlineViewer();
        }
        return instance;
    }
    
    private OnlineViewer() {
        initServer();
    }
    
    /**
     * 初始化服务器
     */
    private void initServer() {
        try {
            // 获取本机IP地址
            localIpAddress = getLocalIpAddress();
            System.out.println("本机IP地址: " + localIpAddress);
            
            // 查找可用端口并启动服务器
            if (setupServer()) {
                System.out.println("=== 华容道网页查看服务已启动 ===");
                System.out.println("HTTP服务 (本地): http://localhost:" + httpPort);
                System.out.println("HTTP服务 (网络): http://" + localIpAddress + ":" + httpPort);
                System.out.println("WebSocket服务: 端口 " + wsPort);
                System.out.println("==============================");
            } else {
                System.err.println("无法启动网页服务 - 请检查网络连接和防火墙设置");
            }
        } catch (Exception e) {
            System.err.println("初始化网页服务时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 设置服务器
     */
    private boolean setupServer() {
        int attempts = 0;
        int maxAttempts = 15;
        
        while (attempts < maxAttempts) {
            try {
                httpPort = findAvailablePort(8000, 9000);
                wsPort = findAvailablePort(httpPort + 1, 9001);
                
                // 启动HTTP服务器
                httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
                httpServer.createContext("/", new MainHandler());
                httpServer.createContext("/status", new StatusHandler());
                httpServer.createContext("/template", new TemplateHandler());
                httpServer.setExecutor(Executors.newCachedThreadPool());
                httpServer.start();
                
                // 启动WebSocket服务器
                wsServer = new GameWebSocketServer(wsPort);
                wsServer.start();
                
                return true;
            } catch (IOException e) {
                attempts++;
                cleanupPartialServers();
                System.err.println("尝试 " + attempts + "/" + maxAttempts + " 失败: " + e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * 清理可能已部分创建的服务器
     */
    private void cleanupPartialServers() {
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
        
        return minPort; // 如果所有端口都不可用，返回最小端口
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":")) {
                        continue; // 跳过IPv6地址
                    }
                    return addr.getHostAddress();
                }
            }
            return "127.0.0.1";
        } catch (Exception e) {
            System.err.println("获取本机IP地址时出错: " + e.getMessage());
            return "127.0.0.1";
        }
    }
    
    /**
     * 创建新的游戏会话并返回唯一URL
     */
    public String createGameSession(MapModel model) {
        if (httpServer == null) {
            return "网页服务未启动，无法创建会话";
        }
        
        try {
            String sessionId = generateSessionId();
            sessionModels.put(sessionId, model);
            
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
            sendJsonResponse(exchange, response);
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
            String sessionId = extractSessionId(query);
            
            System.out.println("收到HTTP请求: " + uri);
            
            String response;
            if (sessionId != null && sessionModels.containsKey(sessionId)) {
                response = loadTemplate("game_view.html")
                    .replace("{{SESSION_ID}}", sessionId)
                    .replace("{{WS_PORT}}", String.valueOf(wsPort))
                    .replace("{{LOCAL_IP}}", localIpAddress);
            } else {
                response = loadTemplate("index.html")
                    .replace("{{HTTP_PORT}}", String.valueOf(httpPort))
                    .replace("{{WS_PORT}}", String.valueOf(wsPort));
            }
            
            sendHtmlResponse(exchange, response);
        }
    }
    
    /**
     * 从查询字符串中提取会话ID
     */
    private String extractSessionId(String query) {
        if (query != null && query.contains("session=")) {
            String sessionId = query.split("session=")[1];
            if (sessionId.contains("&")) {
                sessionId = sessionId.split("&")[0];
            }
            return sessionId;
        }
        return null;
    }
    
    /**
     * 处理模板资源请求
     */
    private class TemplateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            
            System.out.println("收到模板请求: " + path);
            
            if (path.equals("/template/game.js")) {
                String js = loadTemplate("game.js");
                sendResponse(exchange, "application/javascript", js);
            } 
            else if (path.equals("/template/styles.css")) {
                String css = loadTemplate("styles.css");
                sendResponse(exchange, "text/css", css);
            }
            else {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
            }
        }
    }
    
    /**
     * 加载模板文件
     */
    private String loadTemplate(String templateName) {
        try (InputStream is = getClass().getResourceAsStream("/templates/" + templateName);
             Scanner scanner = new Scanner(is, "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            System.err.println("加载模板 " + templateName + " 失败: " + e.getMessage());
            return "<!-- 模板加载失败 -->";
        }
    }
    
    /**
     * 发送HTML响应
     */
    private void sendHtmlResponse(HttpExchange exchange, String content) throws IOException {
        sendResponse(exchange, "text/html; charset=utf-8", content);
    }
    
    /**
     * 发送JSON响应
     */
    private void sendJsonResponse(HttpExchange exchange, String content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendResponseBytes(exchange, content.getBytes());
    }
    
    /**
     * 发送通用响应
     */
    private void sendResponse(HttpExchange exchange, String contentType, String content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        sendResponseBytes(exchange, content.getBytes("UTF-8"));
    }
    
    /**
     * 发送响应字节
     */
    private void sendResponseBytes(HttpExchange exchange, byte[] bytes) throws IOException {
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * WebSocket服务器实现
     */
    private class GameWebSocketServer extends WebSocketServer {
        public GameWebSocketServer(int port) {
            super(new InetSocketAddress(port));
            setConnectionLostTimeout(60); // 60秒超时
        }
        
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("WebSocket新连接: " + conn.getRemoteSocketAddress());
            
            String sessionId = extractSessionId(handshake.getResourceDescriptor());
            sendInitialGameState(conn, sessionId);
        }
        
        private void sendInitialGameState(WebSocket conn, String sessionId) {
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
                            " 代码: " + code + " 原因: " + reason);
        }
        
        @Override
        public void onMessage(WebSocket conn, String message) {
            System.out.println("收到WebSocket消息: " + message);
        }
        
        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("WebSocket错误: " + (conn != null ? conn.getRemoteSocketAddress() : "未知连接"));
            ex.printStackTrace();
        }
        
        @Override
        public void onStart() {
            System.out.println("WebSocket服务器已启动，监听端口: " + getPort());
        }
    }
}