package view.level;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import controller.LevelSelectController;
import view.util.FontManager;
import view.util.FrameUtil;

/**
 * 关卡选择界面 显示所有可用的华容道关卡布局供玩家选择
 */
public class LevelSelectFrame extends JFrame implements LevelSelectView {

    private JPanel levelPanel;
    private LevelSelectController controller;

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

        // 初始化UI组件
        initializeUI();
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 使用BorderLayout作为主布局
        this.setLayout(new BorderLayout());

        // 创建主面板，使用BorderLayout，增大内边距
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 10, 50, 10, 50);

        // 创建顶部标题面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Select Level", JLabel.CENTER);
        titleLabel.setFont(FontManager.getTitleFont(24)); // 增大标题字体
        // 减小标题上下边距，特别是上边距
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        // 添加标题面板到主面板顶部
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 创建中央内容面板 - 将使用GridLayout显示关卡
        levelPanel = new JPanel();
        levelPanel.setLayout(new GridLayout(0, 2, 30, 30)); // 增大关卡之间的间距

        // 将关卡面板放入滚动面板
        JScrollPane scrollPane = new JScrollPane(levelPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(20); // 增大滚动速度

        // 为滚动面板创建包装面板，减小上下边距
        JPanel scrollPaneWrapper = new JPanel(new BorderLayout());
        scrollPaneWrapper.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // 减小为15像素间距
        scrollPaneWrapper.add(scrollPane, BorderLayout.CENTER);

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
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(FrameUtil.PRIMARY_COLOR, 3)); // 加粗边框

        // 创建关卡标题
        JLabel nameLabel = new JLabel(level.getName(), JLabel.CENTER);
        nameLabel.setFont(FontManager.getTitleFont(20)); // 增大标题字体
        nameLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10)); // 增大内边距

        // 创建关卡描述
        JLabel descLabel = new JLabel(level.getDescription(), JLabel.CENTER);
        descLabel.setFont(FontManager.getRegularFont(16)); // 增大描述字体
        descLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10)); // 增大内边距

        // 创建选择按钮
        JButton selectButton = FrameUtil.createStyledButton("Select", true);
        selectButton.setPreferredSize(new Dimension(120, 45)); // 增大按钮尺寸
        selectButton.setFont(FontManager.getButtonFont().deriveFont(16f)); // 增大按钮字体
        selectButton.addActionListener(e -> {
            if (controller != null) {
                controller.selectLevel(index);
            }
        });

        // 添加组件到卡片
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(nameLabel, BorderLayout.CENTER);
        topPanel.add(descLabel, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0)); // 增大内边距
        buttonPanel.add(selectButton);

        card.add(topPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        return card;
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
}
