package view.victory;

import view.util.FontManager;
import view.util.FrameUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 游戏胜利界面，显示胜利消息和提供不同的后续操作按钮
 */
public class VictoryFrame extends JDialog implements VictoryView {

    private final JLabel messageLabel;
    private final JLabel stepsLabel;
    private final JButton homeButton;
    private final JButton levelSelectButton;
    private final JButton restartButton;
    private final JButton nextLevelButton;

    // 撒花效果面板
    private ConfettiPanel confettiPanel;

    /**
     * 构造方法，初始化胜利界面的UI组件
     * @param parent 父窗口引用
     */
    public VictoryFrame(JFrame parent) {
        super(parent, "Victory!", true); // 创建模态对话框

        // 设置对话框基本属性
        setSize(450, 350);
        setLocationRelativeTo(parent); // 居中显示
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // 创建主面板
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 20, 20, 20, 20);
        setContentPane(mainPanel);

        // 创建顶部胜利消息面板
        JPanel headerPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 0, 0, 15, 0);
        headerPanel.setOpaque(true);

        // 创建胜利消息标签
        messageLabel = FrameUtil.createTitleLabel("Victory!", JLabel.CENTER);
        messageLabel.setFont(FontManager.getTitleFont(32));
        messageLabel.setForeground(FrameUtil.ACCENT_COLOR);
        headerPanel.add(messageLabel, BorderLayout.CENTER);

        // 添加副标题
        JLabel subtitleLabel = FrameUtil.createTitleLabel("You completed the puzzle!", JLabel.CENTER);
        subtitleLabel.setFont(FontManager.getRegularFont(18));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央步数显示面板
        JPanel stepsPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 10, 0, 20, 0);
        stepsPanel.setOpaque(true);

        // 创建步数标签
        stepsLabel = FrameUtil.createTitleLabel("Steps: 0", JLabel.CENTER);
        stepsLabel.setFont(FontManager.getTitleFont(22));
        stepsLabel.setForeground(FrameUtil.PRIMARY_COLOR);
        stepsPanel.add(stepsLabel, BorderLayout.CENTER);

        mainPanel.add(stepsPanel, BorderLayout.CENTER);

        // 创建按钮
        homeButton = FrameUtil.createStyledButton("Back to Home", true);
        levelSelectButton = FrameUtil.createStyledButton("Level Select", true);
        restartButton = FrameUtil.createStyledButton("Play Again", true);
        nextLevelButton = FrameUtil.createStyledButton("Next Level", true);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        FrameUtil.setPadding(buttonPanel, 0, 10, 10, 10);
        buttonPanel.setOpaque(true);

        // 将按钮添加到按钮面板
        buttonPanel.add(homeButton);
        buttonPanel.add(levelSelectButton);
        buttonPanel.add(restartButton);
        buttonPanel.add(nextLevelButton);

        // 将按钮面板添加到主面板
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 初始化撒花效果面板
        confettiPanel = new ConfettiPanel();
        JRootPane rootPane = getRootPane();
        rootPane.setGlassPane(confettiPanel);
        confettiPanel.setVisible(true);
    }

    @Override
    public void showVictory(String victoryMessage) {
        messageLabel.setText(victoryMessage);
        confettiPanel.startAnimation();
        setVisible(true);
    }

    @Override
    public void showVictory(String victoryMessage, int steps) {
        messageLabel.setText(victoryMessage);
        stepsLabel.setText("Steps: " + steps);
        confettiPanel.startAnimation();
        setVisible(true);
    }

    @Override
    public void hideVictory() {
        confettiPanel.stopAnimation();
        setVisible(false);
        dispose();
    }

    @Override
    public void setHomeListener(ActionListener listener) {
        homeButton.addActionListener(listener);
    }

    @Override
    public void setLevelSelectListener(ActionListener listener) {
        levelSelectButton.addActionListener(listener);
    }

    @Override
    public void setRestartListener(ActionListener listener) {
        restartButton.addActionListener(listener);
    }

    @Override
    public void setNextLevelListener(ActionListener listener) {
        nextLevelButton.addActionListener(listener);
    }

    /**
     * 撒花效果面板，用于在胜利界面上显示撒花动画
     */
    private class ConfettiPanel extends JPanel {
        private final List<Confetti> confettiList = new ArrayList<>();
        private final Timer animationTimer;
        private Timer durationTimer;
        private Timer fadeOutTimer;
        private boolean isFadingOut = false;
        private float fadeAlpha = 1.0f;
        private final Random random = new Random();

        // 撒花颜色数组，改回彩色系列带透明度
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

        public ConfettiPanel() {
            setOpaque(false);
            setVisible(false);

            // 初始化350个彩色纸片，增加数量使效果更丰富
            for (int i = 0; i < 350; i++) {
                confettiList.add(createRandomConfetti());
            }

            // 创建动画计时器，每20毫秒更新一次
            animationTimer = new Timer(20, e -> {
                updateConfetti();
                repaint(0, 0, getWidth(), getHeight()); // 限制重绘区域
            });

            // 创建持续时间计时器，5秒后开始淡出
            durationTimer = new Timer(5000, e -> {
                startFadeOut();
                durationTimer.stop();
            });
            durationTimer.setRepeats(false);

            // 创建淡出计时器
            fadeOutTimer = new Timer(50, e -> {
                updateFadeOut();
                repaint(0, 0, getWidth(), getHeight());
            });
            fadeOutTimer.setRepeats(true);
        }

        // 获取随机透明度 (70-90% 不透明度)
        private int getRandomAlpha() {
            return 180 + random.nextInt(50);
        }

        /**
         * 创建一个随机位置、颜色和大小的彩色纸片
         */
        private Confetti createRandomConfetti() {
            int width = getWidth() > 0 ? getWidth() : 450;
            int x = random.nextInt(width);
            int y = random.nextInt(50) - 100; // 从屏幕上方开始
            int size = random.nextInt(8) + 5; // 5-12的随机大小
            Color color = colors[random.nextInt(colors.length)];
            double speed = 3.0 + random.nextDouble() * 5.0; // 3-8的随机速度，比原来更快
            return new Confetti(x, y, size, color, speed);
        }

        /**
         * 更新所有纸片的位置
         */
        private void updateConfetti() {
            int height = getHeight() > 0 ? getHeight() : 350;

            for (int i = 0; i < confettiList.size(); i++) {
                Confetti c = confettiList.get(i);
                c.update();

                // 如果纸片落到屏幕底部，重新创建一个从顶部下落的纸片
                if (c.y > height) {
                    confettiList.set(i, createRandomConfetti());
                }
            }
        }

        /**
         * 开始淡出效果
         */
        private void startFadeOut() {
            isFadingOut = true;
            fadeAlpha = 1.0f;
            fadeOutTimer.start();
        }

        /**
         * 更新淡出效果
         */
        private void updateFadeOut() {
            // 每次更新减少透明度
            fadeAlpha -= 0.05f;

            // 当透明度为0时，停止淡出动画
            if (fadeAlpha <= 0) {
                fadeAlpha = 0;
                fadeOutTimer.stop();
                animationTimer.stop();
                confettiList.clear();  // 清空所有碎片
                repaint();  // 确保视图更新
            }
        }

        /**
         * 启动撒花动画
         */
        public void startAnimation() {
            setVisible(true);
            isFadingOut = false;
            fadeAlpha = 1.0f;

            // 停止可能正在运行的计时器
            if (fadeOutTimer.isRunning()) {
                fadeOutTimer.stop();
            }

            // 重新填充碎片列表（如果为空）
            if (confettiList.isEmpty()) {
                for (int i = 0; i < 350; i++) {
                    confettiList.add(createRandomConfetti());
                }
            }

            // 启动动画计时器和持续时间计时器
            if (!animationTimer.isRunning()) {
                animationTimer.start();
                durationTimer.restart();
            }
        }

        /**
         * 停止撒花动画
         */
        public void stopAnimation() {
            if (animationTimer.isRunning()) {
                animationTimer.stop();
            }
            if (durationTimer.isRunning()) {
                durationTimer.stop();
            }
            if (fadeOutTimer.isRunning()) {
                fadeOutTimer.stop();
            }
            confettiList.clear();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // 启用高质量渲染
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 如果在淡出阶段，应用全局透明度
            if (isFadingOut) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
            }

            // 绘制所有彩色纸片
            for (Confetti c : confettiList) {
                c.draw(g2d);
            }
        }
    }

    /**
     * 表示单个彩色纸片的类
     */
    private class Confetti {
        private double x, y;
        private final int size;
        private final Color color;
        private final double speed;
        private double rotation = 0;
        private final double rotationSpeed;
        private double horizontalMovement = 0;

        public Confetti(double x, double y, int size, Color color, double speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.speed = speed;
            this.rotationSpeed = 0.08 + Math.random() * 0.12; // 稍微快一点的旋转
            this.horizontalMovement = Math.random() * Math.PI * 2;
        }

        /**
         * 更新纸片的位置和旋转角度
         */
        public void update() {
            // 纸片下落
            y += speed;

            // 纸片水平摆动
            x += Math.sin(horizontalMovement) * 1.0; // 稍微大一点的摆动幅度
            horizontalMovement += 0.06;

            // 纸片旋转
            rotation += rotationSpeed;
        }

        /**
         * 绘制纸片
         */
        public void draw(Graphics2D g2d) {
            AffineTransform oldTransform = g2d.getTransform();

            // 应用旋转和平移
            g2d.translate(x, y);
            g2d.rotate(rotation);

            // 绘制彩色纸片
            g2d.setColor(color);
            if (Math.random() > 0.5) { // 50%概率为圆形，增加形状多样性
                g2d.fillOval(-size / 2, -size / 2, size, size);
            } else {
                g2d.fillRect(-size / 2, -size / 2, size, size);
            }

            // 恢复原来的变换
            g2d.setTransform(oldTransform);
        }
    }
}