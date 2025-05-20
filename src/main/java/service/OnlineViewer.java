package service;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font; // 新增导入
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
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

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

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

        // 添加JVM关闭钩子，确保程序退出时关闭所有连接
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("检测到程序退出，正在关闭网络服务...");
            stop();
        }));
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
                System.out.println("可以使用浏览器访问以上地址查看游戏状态");
                System.out.println("==============================");
            } else {
                System.err.println("无法启动网页服务 - 请检查网络连接和防火墙设置");
                System.err.println("请检查HTTP端口 " + httpPort + " 是否被其他应用占用");
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
                // 固定HTTP端口为8080
                httpPort = 8080;
                System.out.println("尝试使用固定HTTP端口 " + httpPort);

                // 仅为WebSocket查找可用端口，从8081开始尝试
                wsPort = findAvailablePort(8081, 9001);
                System.out.println("找到可用的WebSocket端口 " + wsPort);

                // 启动HTTP服务器
                System.out.println("开始创建HTTP服务器，绑定端口 " + httpPort + "...");
                httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
                httpServer.createContext("/", new MainHandler());
                httpServer.createContext("/status", new StatusHandler());
                httpServer.createContext("/template", new TemplateHandler());
                httpServer.setExecutor(Executors.newCachedThreadPool());
                httpServer.start();
                System.out.println("HTTP服务器创建成功，端口 " + httpPort + " 监听中");

                // 启动WebSocket服务器
                wsServer = new GameWebSocketServer(wsPort);
                wsServer.start();
                System.out.println("WebSocket服务器已启动，监听端口: " + wsPort);

                return true;
            } catch (IOException e) {
                attempts++;
                cleanupPartialServers();

                // 如果错误是8080端口不可用，直接报错并退出循环
                if (e.getMessage() != null && e.getMessage().contains("8080")) {
                    System.err.println("错误: HTTP端口 " + httpPort + " 已被占用，无法启动服务");
                    System.err.println("请检查是否有其他应用正在使用此端口，或者尝试关闭这些应用后重启游戏");
                    System.err.println("技术错误信息: " + e.getMessage());
                    return false;
                }

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
     * 检查服务是否正在运行
     *
     * @return 如果HTTP和WebSocket服务都在运行，则返回true
     */
    public boolean isRunning() {
        return httpServer != null && wsServer != null;
    }

    /**
     * 如果服务已停止，则重新启动服务
     */
    public void ensureRunning() {
        if (!isRunning()) {
            System.out.println("网页服务已停止，正在重新启动...");
            initServer();
        }
    }

    /**
     * 创建新的游戏会话并返回唯一URL，同时显示包含二维码的弹窗
     */
    public String createGameSession(MapModel model) {
        // 确保服务处于运行状态
        ensureRunning();

        if (httpServer == null) {
            return "网页服务未启动，无法创建会话";
        }

        try {
            String sessionId = generateSessionId();
            sessionModels.put(sessionId, model);

            String lanUrl = "http://" + localIpAddress + ":" + httpPort + "/?session=" + sessionId;
            String publicUrl = "http://3.tcp.cpolar.top:10026/?session=" + sessionId; // 根据实际公网映射URL调整

            StringBuilder urls = new StringBuilder();
            urls.append("本地访问: http://localhost:").append(httpPort).append("/?session=").append(sessionId).append("\n");
            urls.append("局域网访问: ").append(lanUrl).append("\n");
            urls.append("公网访问 (Cpolar): ").append(publicUrl);

            // 生成二维码并显示弹窗
            try {
                BufferedImage lanQRCode = generateQRCodeImage(lanUrl, 200, 200);
                BufferedImage publicQRCode = generateQRCodeImage(publicUrl, 200, 200);

                // 在Swing事件调度线程中显示弹窗
                SwingUtilities.invokeLater(()
                        -> displayQRCodePopup(lanQRCode, lanUrl, publicQRCode, publicUrl)
                );

            } catch (WriterException e) {
                System.err.println("生成二维码失败: " + e.getMessage());
                // 即使二维码生成失败，也继续返回URL字符串
            }

            return urls.toString();
        } catch (Exception e) {
            System.err.println("创建游戏会话时出错: " + e.getMessage());
            return "创建会话时出错: " + e.getMessage();
        }
    }

    /**
     * 生成二维码图像
     *
     * @param text 要编码的文本 (URL)
     * @param width 二维码宽度
     * @param height 二维码高度
     * @return BufferedImage 类型的二维码图像
     * @throws WriterException 如果生成失败
     */
    private static BufferedImage generateQRCodeImage(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // 调整边距

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * 显示包含二维码和URL的弹窗
     *
     * @param lanQRCode 局域网二维码图像
     * @param lanUrl 局域网URL
     * @param publicQRCode 公网二维码图像
     * @param publicUrl 公网URL
     */
    private void displayQRCodePopup(BufferedImage lanQRCode, String lanUrl, BufferedImage publicQRCode, String publicUrl) {
        // 将第三个参数 'true' (模态) 改为 'false' (非模态)
        JDialog dialog = new JDialog((java.awt.Frame) null, "游戏访问链接二维码", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10)); // 主布局，带间距

        // 将 GridLayout(1, 2, ...) 改为 GridLayout(2, 1, 0, 15) 使其垂直排列，并调整垂直间距
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 给主面板添加边距

        // 局域网二维码面板
        JPanel lanPanel = new JPanel(new BorderLayout(5, 5));
        JLabel lanTitleLabel = new JLabel("局域网访问 (LAN)", SwingConstants.CENTER);
        lanTitleLabel.setFont(new Font(lanTitleLabel.getFont().getName(), Font.BOLD, 16)); // 加粗并设置字体大小
        JLabel lanImageLabel = new JLabel(new ImageIcon(lanQRCode));
        JLabel lanUrlOnlyLabel = new JLabel(lanUrl, SwingConstants.CENTER); // 只显示URL
        lanUrlOnlyLabel.setFont(new Font(lanUrlOnlyLabel.getFont().getName(), Font.PLAIN, 12));

        lanPanel.add(lanTitleLabel, BorderLayout.NORTH);
        lanPanel.add(lanImageLabel, BorderLayout.CENTER);
        lanPanel.add(lanUrlOnlyLabel, BorderLayout.SOUTH);
        mainPanel.add(lanPanel);

        // 公网二维码面板
        JPanel publicPanel = new JPanel(new BorderLayout(5, 5));
        JLabel publicTitleLabel = new JLabel("公网访问 (Cpolar没给钱很慢)", SwingConstants.CENTER);
        publicTitleLabel.setFont(new Font(publicTitleLabel.getFont().getName(), Font.BOLD, 16)); // 加粗并设置字体大小
        JLabel publicImageLabel = new JLabel(new ImageIcon(publicQRCode));
        JLabel publicUrlOnlyLabel = new JLabel(publicUrl, SwingConstants.CENTER); // 只显示URL
        publicUrlOnlyLabel.setFont(new Font(publicUrlOnlyLabel.getFont().getName(), Font.PLAIN, 12));

        publicPanel.add(publicTitleLabel, BorderLayout.NORTH);
        publicPanel.add(publicImageLabel, BorderLayout.CENTER);
        publicPanel.add(publicUrlOnlyLabel, BorderLayout.SOUTH);
        mainPanel.add(publicPanel);

        dialog.add(mainPanel, BorderLayout.CENTER);

        // 添加一个总标题或说明
        JLabel overallTitleLabel = new JLabel("请扫描二维码或在浏览器中打开链接访问游戏", SwingConstants.CENTER);
        overallTitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        dialog.add(overallTitleLabel, BorderLayout.NORTH);

        dialog.pack();
        // 调整最小尺寸以适应垂直布局
        dialog.setMinimumSize(new Dimension(380, 700));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * 更新特定会话的游戏模型和统计数据
     *
     * @param sessionId 会话ID
     * @param model 游戏模型
     * @param steps 当前步数
     * @param gameTime 当前游戏时间（毫秒）
     * @param minSteps 最少步数
     */
    public void updateGameSession(String sessionId, MapModel model, int steps, long gameTime, int minSteps) {
        // 确保服务处于运行状态
        ensureRunning();

        if (wsServer == null) {
            System.err.println("WebSocket服务未启动，无法更新会话");
            return;
        }

        try {
            sessionModels.put(sessionId, model); // 仍然需要存储模型以备新连接时使用
            broadcastUpdate(sessionId, model, steps, gameTime, minSteps);
        } catch (Exception e) {
            System.err.println("更新游戏会话时出错: " + e.getMessage());
        }
    }

    /**
     * 通过WebSocket广播游戏更新
     *
     * @param sessionId 会话ID
     * @param model 游戏模型
     * @param steps 当前步数
     * @param gameTime 当前游戏时间（毫秒）
     * @param minSteps 最少步数
     */
    private void broadcastUpdate(String sessionId, MapModel model, int steps, long gameTime, int minSteps) {
        if (wsServer == null || wsServer.getConnections().isEmpty()) {
            return; // 没有活动连接，不需要广播
        }

        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("sessionId", sessionId);
            updateData.put("matrix", model.getMatrix());
            updateData.put("timestamp", System.currentTimeMillis());
            updateData.put("steps", steps);
            updateData.put("gameTime", gameTime);
            updateData.put("minSteps", minSteps);

            String jsonUpdate = jsonMapper.writeValueAsString(updateData);
            wsServer.broadcast(jsonUpdate);
        } catch (Exception e) {
            System.err.println("广播更新时出错: " + e.getMessage());
        }
    }

    /**
     * 广播游戏胜利消息给特定会话的客户端
     *
     * @param sessionId 会话ID
     * @param finalModel 最终的游戏模型
     * @param finalSteps 最终步数
     * @param finalGameTime 最终游戏时间
     * @param finalMinSteps 最终最少步数 (通常是0或与当前状态一致)
     */
    public void broadcastGameWon(String sessionId, MapModel finalModel, int finalSteps, long finalGameTime, int finalMinSteps) {
        if (wsServer == null || wsServer.getConnections().isEmpty() || sessionId == null || finalModel == null) {
            System.err.println("无法广播游戏胜利消息：WebSocket服务未运行，或会话ID/模型为空。");
            return;
        }

        try {
            Map<String, Object> gameWonData = new HashMap<>();
            gameWonData.put("type", "game_won");
            gameWonData.put("sessionId", sessionId);
            gameWonData.put("matrix", finalModel.getMatrix()); // 最终棋盘状态
            gameWonData.put("timestamp", System.currentTimeMillis());
            gameWonData.put("message", "游戏已胜利!");
            gameWonData.put("steps", finalSteps);
            gameWonData.put("gameTime", finalGameTime);
            gameWonData.put("minSteps", finalMinSteps);

            String jsonMessage = jsonMapper.writeValueAsString(gameWonData);

            wsServer.broadcast(jsonMessage);
            System.out.println("已向会话 " + sessionId + " 广播游戏胜利消息 (包含最终统计数据)。");

        } catch (Exception e) {
            System.err.println("广播游戏胜利消息时出错: " + e.getMessage());
        }
    }

    /**
     * 生成唯一会话ID
     */
    private String generateSessionId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        return sdf.format(new Date()) + "_" + (int) (Math.random() * 10000);
    }

    /**
     * 停止服务
     */
    public void stop() {
        if (wsServer != null) {
            try {
                // 发送关闭消息给所有连接的客户端
                Map<String, Object> closeData = new HashMap<>();
                closeData.put("type", "server_shutdown");
                closeData.put("message", "游戏已关闭");
                closeData.put("timestamp", System.currentTimeMillis());

                String closeMessage = jsonMapper.writeValueAsString(closeData);
                wsServer.broadcast(closeMessage);

                // 给客户端一点时间处理关闭消息
                Thread.sleep(200);

                // 关闭WebSocket服务器
                wsServer.stop();
                wsServer = null;
                System.out.println("WebSocket服务已停止");
            } catch (Exception e) {
                System.err.println("停止WebSocket服务器时出错: " + e.getMessage());
            }
        }

        if (httpServer != null) {
            System.out.println("正在停止HTTP服务，端口: " + httpPort);
            httpServer.stop(0);
            httpServer = null;
            System.out.println("HTTP服务已停止");
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
            } else if (path.equals("/template/styles.css")) {
                String css = loadTemplate("styles.css");
                sendResponse(exchange, "text/css", css);
            } else {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
            }
        }
    }

    /**
     * 加载模板文件
     */
    private String loadTemplate(String templateName) {
        try (InputStream is = getClass().getResourceAsStream("/templates/" + templateName); Scanner scanner = new Scanner(is, "UTF-8")) {
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
                    // 发送默认的初始统计数据
                    initialData.put("steps", 0);
                    initialData.put("gameTime", 0L);
                    initialData.put("minSteps", -1); // -1 表示未知或不适用

                    String jsonData = jsonMapper.writeValueAsString(initialData);
                    conn.send(jsonData);
                    System.out.println("已发送初始游戏数据 (包含默认统计) 到WebSocket客户端 for session: " + sessionId);
                } catch (Exception e) {
                    System.err.println("发送初始状态时出错: " + e.getMessage());
                }
            } else {
                System.out.println("WebSocket连接的会话ID无效或不存在: " + sessionId + ". 未发送初始数据.");
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("WebSocket连接关闭: " + conn.getRemoteSocketAddress()
                    + " 代码: " + code + " 原因: " + reason);
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
