package view.victory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 撒花效果面板，用于在胜利界面上显示撒花动画
 */
public class ConfettiPanel extends JPanel {
    private final List<Confetti> confettiList = new CopyOnWriteArrayList<>();
    private final List<Confetti> confettiPool = new ArrayList<>();
    private final Random random = new Random();

    private static final int MAX_CONFETTI = 180;
    private int totalConfettiGenerated = 0;
    private static final int MAX_TOTAL_CONFETTI = 180;
    private static final int CONFETTI_BATCH_SIZE = 7;
    private static final int GENERATION_INTERVAL = 100;

    private javax.swing.Timer animationTimer; // 用于动画更新和重绘
    private javax.swing.Timer generationTimer; // 保留用于生成纸片
    private long lastUpdateTimeNanos; // 用于计算时间差
    private static final int TARGET_FPS = 60;
    private static final int ANIMATION_DELAY_MS = 1000 / TARGET_FPS; // Timer 延迟（毫秒）

    private static final int CONFETTI_SIZE = 12;

    private int currentSectionIndex = 0;
    private static final int HORIZONTAL_SECTIONS = 8; // 减少分区

    private final Color[] colors = {
            new Color(255, 0, 0, getRandomAlpha()),      // 红色
            new Color(255, 165, 0, getRandomAlpha()),    // 橙色
            new Color(255, 255, 0, getRandomAlpha()),    // 黄色
            new Color(0, 255, 0, getRandomAlpha()),      // 绿色
            new Color(0, 191, 255, getRandomAlpha()),    // 天蓝色
            new Color(0, 0, 255, getRandomAlpha()),      // 蓝色
            new Color(138, 43, 226, getRandomAlpha()),   // 紫色
            new Color(255, 20, 147, getRandomAlpha()),   // 粉红色
            new Color(64, 224, 208, getRandomAlpha()),   // 青色
            new Color(255, 215, 0, getRandomAlpha())     // 金色
    };

    // 使用硬件加速渲染
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    public ConfettiPanel() {
        setOpaque(false);
        setVisible(false);
        setDoubleBuffered(true); // 确保双缓冲开启

        // 预先创建纸片对象池
        for (int i = 0; i < MAX_CONFETTI; i++) {
            confettiPool.add(new Confetti(0, 0, CONFETTI_SIZE, Color.WHITE, 0, random));
        }

        // 创建纸片生成计时器 (EDT)
        generationTimer = new Timer(GENERATION_INTERVAL, e -> {
            if (totalConfettiGenerated < MAX_TOTAL_CONFETTI) {
                generateConfettiBatch();
            } else {
                generationTimer.stop();
            }
        });

        // 创建动画计时器 (EDT)
        animationTimer = new Timer(ANIMATION_DELAY_MS, e -> animationTick());
        animationTimer.setInitialDelay(0); // 可以立即开始（如果需要）
    }

    // 新增：动画计时器的处理方法
    private void animationTick() {
        long now = System.nanoTime();
        // 首次调用或暂停后恢复时，重置时间戳
        if (lastUpdateTimeNanos == 0) {
            lastUpdateTimeNanos = now;
            return; // 跳过第一次更新，避免 deltaTime 过大
        }

        long elapsedNanos = now - lastUpdateTimeNanos;
        lastUpdateTimeNanos = now;

        // 计算实际经过的时间（秒），并限制最大值防止跳跃过大
        double deltaTimeSeconds = Math.min(elapsedNanos / 1_000_000_000.0, 0.1);

        updateConfetti(deltaTimeSeconds); // 更新纸片状态
        repaint(); // 直接调用 repaint，因为在 EDT 上
    }

    // 获取随机透明度 (70-90% 不透明度)
    private int getRandomAlpha() {
        return 180 + random.nextInt(50);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width > 0 && height > 0) {
            // 使用TRANSLUCENT类型支持更好的透明度渲染
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc != null) {
                // 尝试使用硬件加速创建缓冲区
                buffer = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            } else {
                buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }

            bufferGraphics = buffer.createGraphics();
            setupGraphics(bufferGraphics);
        }
    }

    private void setupGraphics(Graphics2D g2d) {
        // Explicitly turn off anti-aliasing for potential performance gain
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    /**
     * 生成一批新纸片
     */
    private void generateConfettiBatch() {
        int batchSize = Math.min(CONFETTI_BATCH_SIZE, MAX_TOTAL_CONFETTI - totalConfettiGenerated);
        for (int i = 0; i < batchSize; i++) {
            if (!confettiPool.isEmpty()) {
                Confetti c = confettiPool.remove(confettiPool.size() - 1);
                resetConfetti(c);
                confettiList.add(c);
                totalConfettiGenerated++;

                // 移动到下一个水平区域
                currentSectionIndex = (currentSectionIndex + 1) % HORIZONTAL_SECTIONS;
            }
        }
    }

    /**
     * 重置纸片的属性而不创建新对象
     */
    private void resetConfetti(Confetti c) {
        int width = getWidth() > 0 ? getWidth() : 450;

        // 计算当前纸片应该落在的水平区域
        int sectionWidth = width / HORIZONTAL_SECTIONS;
        int minX = currentSectionIndex * sectionWidth;

        // 在区域内随机选择位置，保留一点随机性
        int x = minX + random.nextInt(sectionWidth);

        // 在屏幕上方添加随机高度，使纸片下落时间有所不同
        int y = random.nextInt(30) - 50;

        Color color = colors[random.nextInt(colors.length)];
        double speed = 100.0 + random.nextDouble() * 80.0;
        c.reset(x, y, color, speed, random);
    }

    private void updateConfetti(double deltaTimeSeconds) {
        int height = getHeight() > 0 ? getHeight() : 350;
        List<Confetti> toRemove = null;

        if (deltaTimeSeconds > 0.1) {
            deltaTimeSeconds = 0.1; // Cap delta time
        }

        for (Confetti c : confettiList) {
            c.update(deltaTimeSeconds);
            if (c.y > height) {
                if (toRemove == null) {
                    toRemove = new ArrayList<>(8);
                }
                toRemove.add(c);
                confettiPool.add(c);
            }
        }

        if (toRemove != null && !toRemove.isEmpty()) {
            confettiList.removeAll(toRemove);
        }
    }

    public void startAnimation() {
        setVisible(true);
        totalConfettiGenerated = 0;
        confettiList.clear();
        currentSectionIndex = 0;

        // 确保对象池足够
        for (int i = confettiPool.size(); i < MAX_CONFETTI; i++) {
            confettiPool.add(new Confetti(0, 0, CONFETTI_SIZE, Color.WHITE, 0, random));
        }

        stopAnimation(); // 先停止可能正在运行的计时器

        lastUpdateTimeNanos = 0; // 重置上次更新时间
        animationTimer.start(); // 启动动画计时器
        generationTimer.start(); // 启动生成计时器
    }

    public void stopAnimation() {
        // 停止两个计时器
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (generationTimer.isRunning()) {
            generationTimer.stop();
        }
        lastUpdateTimeNanos = 0; // 重置时间戳
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc != null) {
                buffer = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            } else {
                buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }

            bufferGraphics = buffer.createGraphics();
            setupGraphics(bufferGraphics);
        }

        bufferGraphics.setComposite(AlphaComposite.Clear);
        bufferGraphics.fillRect(0, 0, getWidth(), getHeight());
        bufferGraphics.setComposite(AlphaComposite.SrcOver);

        Object[] currentConfettiArray = confettiList.toArray();

        for (Object obj : currentConfettiArray) {
            Confetti c = (Confetti) obj;
            if (c != null) {
                c.draw(bufferGraphics);
            }
        }

        g.drawImage(buffer, 0, 0, null);
    }
}
