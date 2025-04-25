package view.home;

import controller.HomeController;
import service.UserSession;
import view.game.GameFrame;
import view.login.LoginFrame;
import view.settings.SettingsFrame; // 导入 SettingsFrame
import view.util.FontManager;
import view.util.FrameUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Home页面窗口
 * 显示欢迎信息、用户信息和功能按钮
 */
public class HomeFrame extends JFrame implements HomeView {
    // UI组件
    private JLabel welcomeLabel;
    private JLabel usernameLabel;
    private JButton startGameButton;
    private JButton settingsButton; // 添加设置按钮
    private JButton logoutButton;
    
    // 控制器
    private final HomeController controller;
    
    /**
     * 创建Home窗口
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public HomeFrame(int width, int height) {
        this.setTitle("Klotski - Home");
        this.setSize(width, height);
        this.setLocationRelativeTo(null); // 窗口居中显示
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        // 初始化控制器
        this.controller = new HomeController(this);
        
        // 初始化UI组件
        initializeUI();
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 使用BorderLayout作为主布局
        this.setLayout(new BorderLayout());
        
        // 创建主面板，使用BorderLayout
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 30, 40, 30, 40);
        
        // 创建顶部欢迎面板
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomeLabel = new JLabel("Welcome Back", JLabel.CENTER);
        welcomeLabel.setFont(FontManager.getTitleFont(24));
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        
        // 用户名标签
        usernameLabel = new JLabel("", JLabel.CENTER);
        usernameLabel.setFont(FontManager.getRegularFont(18));
        welcomePanel.add(usernameLabel, BorderLayout.SOUTH);
        FrameUtil.setPadding(usernameLabel, 15, 0, 0, 0);
        
        // 添加欢迎面板到主面板顶部
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
        
        // 创建中央内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        FrameUtil.setPadding(contentPanel, 30, 0, 0, 0);
        
        // 开始游戏按钮
        startGameButton = FrameUtil.createStyledButton("Start Game", true);
        startGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startGameButton.setMaximumSize(new Dimension(200, 50));
        startGameButton.setPreferredSize(new Dimension(200, 50));
        
        // 设置按钮
        settingsButton = FrameUtil.createStyledButton("Settings", false); // 使用次要样式
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsButton.setMaximumSize(new Dimension(200, 50));
        settingsButton.setPreferredSize(new Dimension(200, 50));
        
        // 退出登录按钮
        logoutButton = FrameUtil.createStyledButton("Logout", false);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(200, 50));
        logoutButton.setPreferredSize(new Dimension(200, 50));
        
        // 添加按钮到内容面板
        contentPanel.add(startGameButton);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(settingsButton); // 添加设置按钮
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(logoutButton);
        
        // 添加内容面板到主面板中央
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // 添加主面板到窗口
        this.add(mainPanel, BorderLayout.CENTER);
        
        // 添加按钮事件监听器
        startGameButton.addActionListener(e -> controller.startGame());
        settingsButton.addActionListener(e -> controller.openSettings()); // 添加设置按钮监听器
        logoutButton.addActionListener(e -> controller.logout());
    }
    
    @Override
    public void updateUsername(String username) {
        // 更新显示的用户名
        usernameLabel.setText(username);
        
        // 如果是访客模式，调整UI显示
        boolean isGuest = UserSession.getInstance().isGuest();
        if (isGuest) {
            welcomeLabel.setText("Guest Mode");
        } else {
            welcomeLabel.setText("Welcome Back");
        }
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
    public void closeHome() {
        this.setVisible(false);
    }
    
    /**
     * 设置游戏窗口引用
     * @param gameFrame 游戏窗口实例
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.controller.setGameFrame(gameFrame);
    }
    
    /**
     * 设置登录窗口引用
     * @param loginFrame 登录窗口实例
     */
    public void setLoginFrame(LoginFrame loginFrame) {
        this.controller.setLoginFrame(loginFrame);
    }
    
    /**
     * 设置设置窗口引用
     * @param settingsFrame 设置窗口实例
     */
    public void setSettingsFrame(SettingsFrame settingsFrame) {
        this.controller.setSettingsFrame(settingsFrame); // 将 SettingsFrame 传递给控制器
    }

    /**
     * 初始化并显示Home页面
     */
    public void initialize() {
        controller.initializeHome();
    }
}

