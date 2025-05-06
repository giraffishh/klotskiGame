package view.settings;

import controller.core.SettingsController; // 确保导入控制器
import view.util.FontManager;
import view.util.FrameUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Settings页面窗口
 * 提供用户进行游戏设置的界面
 */
public class SettingsFrame extends JFrame implements SettingsView {

    // UI组件 (示例)
    private JLabel titleLabel;
    private JButton saveButton;
    private JButton cancelButton;
    private JPanel settingsPanel; // 用于放置具体的设置选项
    private JComboBox<String> themeComboBox; // 添加主题选择下拉框

    private SettingsController controller; // 添加控制器引用

    /**
     * 创建Settings窗口
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public SettingsFrame(int width, int height) {
        this.setTitle("Klotski - Settings");
        this.setSize(width, height);
        this.setLocationRelativeTo(null); // 窗口居中显示
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // 关闭时不退出整个程序

        // 初始化UI组件
        initializeUI();

        // 初始化控制器 - 在构造函数中直接创建和设置控制器
        this.initializeController();
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 使用BorderLayout作为主布局
        this.setLayout(new BorderLayout());

        // 创建主面板，带边距
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 20, 20, 20, 20);

        // 标题标签
        titleLabel = FrameUtil.createTitleLabel("Settings", JLabel.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 设置选项面板 (居中)
        settingsPanel = new JPanel();
        // 使用更灵活的布局，例如 BoxLayout
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        FrameUtil.setPadding(settingsPanel, 20, 0, 20, 0); // 上下边距

        // --- 添加主题设置 ---
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setFont(FontManager.getRegularFont(16));
        themeComboBox = new JComboBox<>(new String[]{"Light", "Dark"}); // 示例主题
        themeComboBox.setFont(FontManager.getRegularFont(16));
        themeComboBox.setPreferredSize(new Dimension(150, 30));

        themePanel.add(themeLabel);
        themePanel.add(themeComboBox);
        themePanel.setAlignmentX(Component.LEFT_ALIGNMENT); // 左对齐
        settingsPanel.add(themePanel); // 将主题面板添加到设置面板
        // --- 主题设置结束 ---

        // 添加其他设置...

        mainPanel.add(settingsPanel, BorderLayout.CENTER); // 确保设置面板被添加到主面板

        // 底部按钮面板 - 修改布局以确保按钮完全显示
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10)); // 增加按钮间距和垂直间距
        
        saveButton = FrameUtil.createStyledButton("Save", true);
        cancelButton = FrameUtil.createStyledButton("Cancel", false);
        
        // 为按钮设置固定尺寸，确保文本能完全显示
        Dimension buttonSize = new Dimension(100, 35);
        saveButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // 确保按钮面板有足够的高度
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // 添加底部边距

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加主面板到窗口
        this.add(mainPanel, BorderLayout.CENTER);

        // 添加按钮事件监听器 (调用Controller方法)
        saveButton.addActionListener(e -> {
            if (controller != null) {
                controller.saveSettings(); // 调用控制器的保存方法
            } else {
                System.err.println("SettingsController not set for SettingsFrame.");
                closeSettings();
            }
        });

        cancelButton.addActionListener(e -> {
            if (controller != null) {
                controller.cancelSettings(); // 调用控制器的取消方法
            } else {
                System.err.println("SettingsController not set for SettingsFrame.");
                closeSettings();
            }
        });
    }

    /**
     * 初始化控制器
     */
    public void initializeController() {
        this.controller = new SettingsController(this);
        // 控制器初始化后加载设置
        this.controller.loadSettings();
    }

    @Override
    public void displayThemeSetting(String themeName) {
        // 实现接口方法：设置下拉框的选中项
        if (themeComboBox != null) {
            themeComboBox.setSelectedItem(themeName);
        }
    }

    @Override
    public String getSelectedTheme() {
        // 实现接口方法：返回下拉框的当前选中项
        if (themeComboBox != null) {
            return (String) themeComboBox.getSelectedItem();
        }
        return null; // 或者返回一个默认值
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

    @Override
    public void closeSettings() {
        this.setVisible(false);
        // dispose(); // 如果希望完全释放资源，可以使用dispose()
    }

    /**
     * 设置控制器
     * @param controller SettingsController实例
     */
    public void setController(SettingsController controller) {
        this.controller = controller;
        // 可选：在设置控制器后立即加载设置以更新UI
        if (this.controller != null) {
            this.controller.loadSettings();
        }
    }
}

