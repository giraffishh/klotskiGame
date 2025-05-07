package view.settings;

import controller.core.SettingsController;
import view.util.FontManager;
import view.util.FrameUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Settings页面窗口
 * 提供用户进行游戏设置的界面
 */
public class SettingsFrame extends JFrame implements SettingsView {
    // UI组件
    private JLabel titleLabel;
    private JComboBox<String> themeComboBox;
    private JComboBox<String> blockThemeComboBox;
    private JButton saveButton;
    private JButton cancelButton;
    
    private SettingsController controller;

    /**
     * 创建Settings窗口
     */
    public SettingsFrame(int width, int height) {
        configureFrame(width, height);
        initializeUI();
        initializeController();
    }
    
    /**
     * 配置窗口基本属性
     */
    private void configureFrame(int width, int height) {
        this.setTitle("Klotski - Settings");
        this.setSize(width, height);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        setupLayout();  // setupLayout现在已包含组件创建的逻辑
        setupButtonActions();
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        this.setLayout(new BorderLayout());
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 20, 20, 20, 20);
        this.add(mainPanel, BorderLayout.CENTER);
        
        // 标题
        titleLabel = FrameUtil.createTitleLabel("Settings", JLabel.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 设置面板
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        FrameUtil.setPadding(settingsPanel, 20, 0, 20, 0);
        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        
        // 添加主题设置
        settingsPanel.add(createThemePanel());
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(createBlockThemePanel());
        
        // 添加按钮面板
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建主题设置面板
     */
    private JPanel createThemePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel("Theme:");
        label.setFont(FontManager.getRegularFont(16));
        
        themeComboBox = new JComboBox<>(new String[]{"Light", "Dark"});
        themeComboBox.setFont(FontManager.getRegularFont(16));
        themeComboBox.setPreferredSize(new Dimension(150, 30));
        
        panel.add(label);
        panel.add(themeComboBox);
        
        return panel;
    }
    
    /**
     * 创建方块主题设置面板
     */
    private JPanel createBlockThemePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel("Block Theme:");
        label.setFont(FontManager.getRegularFont(16));
        
        blockThemeComboBox = new JComboBox<>(new String[]{"Classic", "Cartoon"});
        blockThemeComboBox.setFont(FontManager.getRegularFont(16));
        blockThemeComboBox.setPreferredSize(new Dimension(150, 30));
        
        panel.add(label);
        panel.add(blockThemeComboBox);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        Dimension buttonSize = new Dimension(100, 35);
        
        saveButton = FrameUtil.createStyledButton("Save", true);
        saveButton.setPreferredSize(buttonSize);
        
        cancelButton = FrameUtil.createStyledButton("Cancel", false);
        cancelButton.setPreferredSize(buttonSize);
        
        panel.add(saveButton);
        panel.add(cancelButton);
        
        return panel;
    }
    
    /**
     * 设置按钮事件
     */
    private void setupButtonActions() {
        saveButton.addActionListener(e -> {
            if (controller != null) {
                controller.saveSettings();
            } else {
                closeSettings();
            }
        });

        cancelButton.addActionListener(e -> {
            if (controller != null) {
                controller.cancelSettings();
            } else {
                closeSettings();
            }
        });
    }

    /**
     * 初始化控制器
     */
    public void initializeController() {
        this.controller = new SettingsController(this);
        this.controller.loadSettings();
    }

    @Override
    public void displayThemeSetting(String themeName) {
        if (themeComboBox != null) {
            themeComboBox.setSelectedItem(themeName);
        }
    }

    @Override
    public String getSelectedTheme() {
        return themeComboBox != null ? (String) themeComboBox.getSelectedItem() : null;
    }
    
    @Override
    public void displayBlockThemeSetting(String blockThemeName) {
        if (blockThemeComboBox != null) {
            blockThemeComboBox.setSelectedItem(blockThemeName);
        }
    }

    @Override
    public String getSelectedBlockTheme() {
        return blockThemeComboBox != null ? (String) blockThemeComboBox.getSelectedItem() : null;
    }

    @Override
    public void showStyledMessage(String message, String title, int messageType) {
        JLabel label = new JLabel(message);
        label.setFont(FontManager.getRegularFont(16));

        JOptionPane optionPane = new JOptionPane(
                label, messageType, JOptionPane.DEFAULT_OPTION);

        UIManager.put("OptionPane.messageFont", FontManager.getRegularFont(16));
        UIManager.put("OptionPane.buttonFont", FontManager.getButtonFont());
        UIManager.put("OptionPane.titleFont", FontManager.getTitleFont(14));

        JDialog dialog = optionPane.createDialog(this, title);
        dialog.setVisible(true);

        FrameUtil.initUIDefaults();
    }

    @Override
    public void closeSettings() {
        this.setVisible(false);
    }

    /**
     * 设置控制器
     */
    public void setController(SettingsController controller) {
        this.controller = controller;
        if (this.controller != null) {
            this.controller.loadSettings();
        }
    }

    /**
     * 确保窗口显示前刷新设置
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible && controller != null) {
            controller.loadSettings();
        }
        super.setVisible(visible);
    }
}

