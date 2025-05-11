package view.level;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import controller.core.LevelSelectController;
import model.MapModel;
import view.util.FontManager;
import view.util.FrameUtil;

/**
 * 关卡选择界面 显示所有可用的华容道关卡布局供玩家选择
 */
public class LevelSelectFrame extends JFrame implements LevelSelectView {

    private JPanel levelPanel;
    private LevelSelectController controller;
    private final Color NORMAL_CARD_BG = Color.WHITE;
    private final Color HOVER_CARD_BG = new Color(240, 248, 255); // 非常淡的蓝色

    // 游戏模式颜色
    private final Color PRACTICE_MODE_COLOR = new Color(84, 173, 255, 180); // 练习模式 - 蓝色
    private final Color SPEED_MODE_COLOR = new Color(255, 128, 0, 180);     // 竞速模式 - 橙色

    // 定义卡片悬停边框颜色 - 使用主色调
    private final Color CARD_BORDER_COLOR = FrameUtil.PRIMARY_COLOR;

    /**
     * 创建关卡选择窗口
     *
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public LevelSelectFrame(int width, int height) {
        this.setTitle("Klotski - Level Selection");
        this.setSize(width, height);
        this.setLocationRelativeTo(null); // 居中显示
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // 添加窗口关闭监听器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 当窗口关闭时返回主页
                if (controller != null) {
                    controller.returnToHome();
                }
            }
        });

        // 初始化UI组件
        initializeUI();
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 使用BorderLayout作为主布局
        this.setLayout(new BorderLayout());

        // 创建主面板，使用BorderLayout，减小水平内边距以便放置更多卡片
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 10, 30, 10, 30);

        // 创建顶部标题面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Select Level", JLabel.CENTER);
        titleLabel.setFont(FontManager.getTitleFont(24)); // 增大标题字体
        // 减小标题上下边距，特别是上边距
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // 添加标题面板到主面板顶部
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 创建中央内容面板 - 使用GridLayout显示关卡，改为每行3张卡片
        levelPanel = new JPanel();
        levelPanel.setLayout(new GridLayout(0, 3, 12, 20)); // 减小卡片间距

        // 将关卡面板放入滚动面板
        JScrollPane scrollPane = new JScrollPane(levelPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(20); // 增大滚动速度

        // 为滚动面板创建包装面板，减小上下边距
        JPanel scrollPaneWrapper = new JPanel(new BorderLayout());
        scrollPaneWrapper.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        scrollPaneWrapper.add(scrollPane, BorderLayout.CENTER);

        // 添加鼠标监听器，当鼠标离开滚动区域时清除所有卡片高亮
        scrollPaneWrapper.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                // 仅当鼠标真正离开区域时才清除高亮效果
                Point p = e.getPoint();
                if (p.x < 0 || p.y < 0 || p.x >= scrollPaneWrapper.getWidth() || p.y >= scrollPaneWrapper.getHeight()) {
                    clearAllCardHighlights();
                }
            }
        });

        // 添加包装后的滚动面板到主面板中央
        mainPanel.add(scrollPaneWrapper, BorderLayout.CENTER);

        // 创建底部按钮面板，减小与中间内容的距离
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        // 减小按钮面板上边距
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JButton backButton = FrameUtil.createStyledButton("Home", false);
        // 增大按钮尺寸
        backButton.setPreferredSize(new Dimension(120, 40));
        backButton.setFont(FontManager.getButtonFont().deriveFont(16f));

        // 添加按钮到按钮面板
        buttonPanel.add(backButton);

        // 添加按钮面板到主面板底部
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加主面板到窗口
        this.add(mainPanel);

        // 添加按钮事件监听器
        backButton.addActionListener(e -> {
            if (controller != null) {
                controller.returnToHome();
            }
        });
    }

    /**
     * 清除所有卡片的高亮效果
     */
    private void clearAllCardHighlights() {
        for (Component comp : levelPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel wrapperPanel = (JPanel) comp;

                // 找到ModeOverlayPanel并隐藏它
                for (Component child : wrapperPanel.getComponents()) {
                    if (child instanceof ModeOverlayPanel) {
                        ((ModeOverlayPanel) child).setVisible(false);
                        ((ModeOverlayPanel) child).setHoverSection(0);
                    } else if (child instanceof JPanel) {
                        // 找到卡片面板并重置样式
                        JPanel card = (JPanel) child;
                        card.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR, 2));

                        // 只重置卡片本身和直接子面板的背景色，不递归处理预览面板
                        card.setBackground(NORMAL_CARD_BG);

                        // 直接获取并重置特定组件，而不是递归处理所有子组件
                        for (Component cardChild : card.getComponents()) {
                            if (cardChild instanceof JPanel) {
                                JPanel contentPanel = (JPanel) cardChild;
                                contentPanel.setBackground(NORMAL_CARD_BG);

                                // 找出内容面板中的信息面板和预览容器
                                for (Component contentChild : contentPanel.getComponents()) {
                                    if (contentChild instanceof JPanel) {
                                        // 重置信息面板和预览容器的背景色，但不修改预览面板本身
                                        if (!(((JPanel) contentChild).getComponentCount() > 0
                                                && ((JPanel) contentChild).getComponent(0) instanceof JPanel
                                                && ((JPanel) ((JPanel) contentChild).getComponent(0)).getBackground().equals(FrameUtil.SECONDARY_COLOR))) {
                                            ((JPanel) contentChild).setBackground(NORMAL_CARD_BG);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 填充关卡列表
     */
    private void populateLevelList() {
        if (controller == null) {
            return;
        }

        // 清空当前面板
        levelPanel.removeAll();

        // 获取所有关卡
        List<LevelSelectController.LevelData> levels = controller.getLevels();

        // 为每个关卡创建卡片
        for (int i = 0; i < levels.size(); i++) {
            LevelSelectController.LevelData level = levels.get(i);

            // 创建关卡卡片面板
            JPanel levelCard = createLevelCard(level, i);
            levelPanel.add(levelCard);
        }

        // 重新验证和重绘面板
        levelPanel.revalidate();
        levelPanel.repaint();
    }

    /**
     * 创建单个关卡卡片
     *
     * @param level 关卡数据
     * @param index 关卡索引
     * @return 关卡卡片面板
     */
    private JPanel createLevelCard(LevelSelectController.LevelData level, int index) {
        // 创建卡片主面板，使用BorderLayout
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR, 2));
        card.setBackground(NORMAL_CARD_BG);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 保持卡片尺寸以适应更大的预览图
        card.setPreferredSize(new Dimension(190, 290));

        // 创建内容面板，减小内边距以增加预览图占比
        JPanel contentPanel = new JPanel(new BorderLayout(0, 8));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3)); // 减小左右内边距
        contentPanel.setBackground(NORMAL_CARD_BG);

        // 预览图面板
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(FrameUtil.SECONDARY_COLOR);

        // 卡片可用宽度（考虑边框和内边距）
        int availableWidth = 180;

        // 尝试加载预览图片
        JLabel previewLabel;
        try {
            String imagePath = "/images/levelPreview/level" + (index + 1) + ".png";
            ImageIcon originalIcon = new ImageIcon(getClass().getResource(imagePath));

            // 确认图片已正确加载
            if (originalIcon.getIconWidth() > 0) {
                // 获取原始图片尺寸
                int originalWidth = originalIcon.getIconWidth();
                int originalHeight = originalIcon.getIconHeight();

                // 计算缩放比例，确保图片完全适应可用宽度
                double scaleFactor = (double) availableWidth / originalWidth;

                // 按比例计算高度
                int scaledHeight = (int) (originalHeight * scaleFactor);

                // 缩放图片保持原始比例
                Image img = originalIcon.getImage();
                Image resizedImg = img.getScaledInstance(availableWidth, scaledHeight, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(resizedImg);

                previewLabel = new JLabel(scaledIcon);
                previewLabel.setHorizontalAlignment(JLabel.CENTER);

                // 调整预览面板大小适应缩放后的图片
                previewPanel.setPreferredSize(new Dimension(availableWidth, scaledHeight));
            } else {
                throw new Exception("Image loading failed");
            }
        } catch (Exception e) {
            // 图片加载失败，显示默认文本
            previewLabel = new JLabel("Level Preview", JLabel.CENTER);
            previewLabel.setFont(FontManager.getRegularFont(14));
            previewLabel.setForeground(FrameUtil.TEXT_COLOR);
            previewPanel.setPreferredSize(new Dimension(availableWidth, 200));
            System.out.println("Failed to load preview image for level " + (index + 1) + ": " + e.getMessage());
        }

        // 将预览标签添加到预览面板
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        // 使用FlowLayout容器包装预览面板，以确保居中对齐
        JPanel previewContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        previewContainer.setBackground(NORMAL_CARD_BG);
        previewContainer.add(previewPanel);

        // 信息面板 - 移到预览图下方
        JPanel infoPanel = new JPanel(new BorderLayout(0, 2));
        infoPanel.setBackground(NORMAL_CARD_BG);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        // 关卡标题
        JLabel nameLabel = new JLabel(level.getName(), JLabel.CENTER);
        nameLabel.setFont(FontManager.getTitleFont(18)); // 增大字体从15到18
        nameLabel.setForeground(FrameUtil.TEXT_COLOR);

        // 关卡描述
        JLabel descLabel = new JLabel(level.getDescription(), JLabel.CENTER);
        descLabel.setFont(FontManager.getRegularFont(12));
        descLabel.setForeground(new Color(100, 100, 100));

        // 组装信息面板
        infoPanel.add(nameLabel, BorderLayout.NORTH);
        infoPanel.add(descLabel, BorderLayout.CENTER);

        // 组装内容面板 - 使用预览容器来确保预览图居中显示
        contentPanel.add(previewContainer, BorderLayout.CENTER);
        contentPanel.add(infoPanel, BorderLayout.SOUTH);

        // 将内容面板添加到卡片
        card.add(contentPanel, BorderLayout.CENTER);

        // 创建游戏模式选择覆盖层 - 使用专用的ModeOverlayPanel类
        ModeOverlayPanel modeOverlay = new ModeOverlayPanel(
                PRACTICE_MODE_COLOR,
                SPEED_MODE_COLOR
        );
        modeOverlay.setVisible(false); // 默认不可见

        // 为卡片和覆盖层创建包装面板
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new OverlayLayout(wrapperPanel));

        // 添加组件到包装面板 - 注意添加顺序很重要，后添加的会在上层显示
        wrapperPanel.add(modeOverlay);
        wrapperPanel.add(card);

        // 添加鼠标事件监听器
        MouseAdapter cardMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 显示悬停效果
                card.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR, 3));
                card.setBackground(HOVER_CARD_BG);
                contentPanel.setBackground(HOVER_CARD_BG);
                infoPanel.setBackground(HOVER_CARD_BG);
                previewContainer.setBackground(HOVER_CARD_BG); // 确保预览容器也更新颜色

                // 显示模式选择覆盖层
                modeOverlay.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 隐藏悬停效果
                card.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR, 2));
                card.setBackground(NORMAL_CARD_BG);
                contentPanel.setBackground(NORMAL_CARD_BG);
                infoPanel.setBackground(NORMAL_CARD_BG);
                previewContainer.setBackground(NORMAL_CARD_BG); // 确保预览容器也恢复颜色

                // 隐藏模式选择覆盖层
                modeOverlay.setVisible(false);
            }
        };

        card.addMouseListener(cardMouseAdapter);

        // 为模式覆盖层添加鼠标监听器，以处理点击事件和区域悬停效果
        modeOverlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controller != null) {
                    Point clickPoint = e.getPoint();
                    int gameMode;

                    // 判断点击位置选择游戏模式 - 根据垂直位置判断
                    if (clickPoint.y < modeOverlay.getHeight() / 2) {
                        // 上半部分 - 教程模式
                        gameMode = MapModel.PRACTICE_MODE;
                    } else {
                        // 下半部分 - 竞速模式
                        gameMode = MapModel.SPEED_MODE;
                    }

                    // 调用控制器的selectLevel方法，传递关卡索引和游戏模式
                    controller.selectLevel(index, gameMode);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // 保持模式选择覆盖层可见
                modeOverlay.setVisible(true);

                // 保持卡片的悬停效果
                card.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR, 3));
                card.setBackground(HOVER_CARD_BG);
                contentPanel.setBackground(HOVER_CARD_BG);
                infoPanel.setBackground(HOVER_CARD_BG);
                previewContainer.setBackground(HOVER_CARD_BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 检查鼠标是否真的离开了整个区域
                Point p = e.getPoint();
                if (p.x < 0 || p.y < 0 || p.x >= modeOverlay.getWidth() || p.y >= modeOverlay.getHeight()) {
                    // 真正离开了区域，隐藏覆盖层并重置卡片样式
                    modeOverlay.setVisible(false);
                    card.setBorder(BorderFactory.createLineBorder(CARD_BORDER_COLOR, 2));
                    card.setBackground(NORMAL_CARD_BG);
                    contentPanel.setBackground(NORMAL_CARD_BG);
                    infoPanel.setBackground(NORMAL_CARD_BG);
                    previewContainer.setBackground(NORMAL_CARD_BG);

                    // 重置悬停区域
                    modeOverlay.setHoverSection(0);
                }
            }
        });

        // 添加鼠标移动监听器来跟踪不同区域的悬停效果
        modeOverlay.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (e.getY() < modeOverlay.getHeight() / 2) {
                    // 鼠标在上半部分 - 教程模式
                    modeOverlay.setHoverSection(1);
                } else {
                    // 鼠标在下半部分 - 竞速模式
                    modeOverlay.setHoverSection(2);
                }
            }
        });

        return wrapperPanel;
    }

    @Override
    public void hideLevelSelect() {
        this.setVisible(false);
    }

    @Override
    public void showLevelSelect() {
        // 在显示前重置所有关卡状态
        if (controller != null) {
            controller.resetAllLevels();
        }

        // 在显示前更新关卡列表
        populateLevelList();
        this.setVisible(true);
    }

    @Override
    public void setController(LevelSelectController controller) {
        this.controller = controller;
    }

    @Override
    public void showStyledMessage(String message, String title, int messageType) {
        // 创建自定义标签
        JLabel label = new JLabel(message);
        label.setFont(FontManager.getRegularFont(16));

        // 显示自定义对话框
        JOptionPane optionPane = new JOptionPane(
                label,
                messageType,
                JOptionPane.DEFAULT_OPTION);

        // 设置标题字体
        UIManager.put("OptionPane.messageFont", FontManager.getRegularFont(16));
        UIManager.put("OptionPane.buttonFont", FontManager.getButtonFont());
        UIManager.put("OptionPane.titleFont", FontManager.getTitleFont(14));

        // 创建并显示对话框
        JDialog dialog = optionPane.createDialog(this, title);
        dialog.setVisible(true);

        // 恢复默认UI设置
        FrameUtil.initUIDefaults();
    }

    /**
     * 获取关卡选择控制器
     *
     * @return 关卡选择控制器
     */
    public LevelSelectController getController() {
        return controller;
    }

    /**
     * 刷新关卡选择界面
     * 当方块主题改变时调用
     */
    public void refreshDisplay() {
        if (this.isVisible()) {
            // 刷新关卡列表，触发重新加载预览图
            populateLevelList();
        }
    }

    /**
     * 模式覆盖面板，用于显示游戏模式选择
     */
    private class ModeOverlayPanel extends JPanel {

        // 跟踪鼠标当前位置：0=未悬停, 1=上半部分(Tutorial), 2=下半部分(Speedrun)
        private int hoverSection = 0;
        private final Color practiceColor;
        private final Color speedColor;

        public ModeOverlayPanel(Color practiceColor, Color speedColor) {
            this.practiceColor = new Color(
                    practiceColor.getRed(),
                    practiceColor.getGreen(),
                    practiceColor.getBlue(),
                    150);
            this.speedColor = new Color(
                    speedColor.getRed(),
                    speedColor.getGreen(),
                    speedColor.getBlue(),
                    150);
            setOpaque(false);
        }

        public void setHoverSection(int section) {
            if (this.hoverSection != section) {
                this.hoverSection = section;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            // 绘制上半部分 - 教程模式
            if (hoverSection == 1) {
                // 鼠标悬停在此区域时，使用更亮/更深的颜色
                g2d.setColor(new Color(64, 153, 255, 210));
            } else {
                g2d.setColor(practiceColor);
            }
            g2d.fillRect(0, 0, getWidth(), getHeight() / 2);

            // 绘制下半部分 - 竞速模式
            if (hoverSection == 2) {
                // 鼠标悬停在此区域时，使用更亮/更深的颜色
                g2d.setColor(new Color(255, 128, 0, 210));
            } else {
                g2d.setColor(speedColor);
            }
            g2d.fillRect(0, getHeight() / 2, getWidth(), getHeight() / 2);

            // 添加模式标签
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.getTitleFont(20)); // 增大字体从16到20

            // 教程模式标签 - 上部居中
            int tutorialY = getHeight() / 4 + 8;
            g2d.drawString("Tutorial Mode", getWidth() / 2 - 70, tutorialY); // 调整位置以匹配更大的字体

            // 竞速模式标签 - 下部居中
            int speedrunY = getHeight() * 3 / 4 + 8;
            g2d.drawString("Speedrun Mode", getWidth() / 2 - 70, speedrunY); // 调整位置以匹配更大的字体

            g2d.dispose();
        }
    }
}
