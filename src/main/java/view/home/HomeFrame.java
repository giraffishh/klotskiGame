package view.home;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import controller.HomeController;
import service.UserSession;
import view.util.FontManager;
import view.util.FrameUtil;

/**
 * Home页面窗口 显示欢迎信息、用户信息和功能按钮
 */
public class HomeFrame extends JFrame implements HomeView {

    // UI组件
    private JLabel welcomeLabel;
    private JLabel usernameLabel;
    private JButton newGameButton;
    private JButton loadGameButton;
    private JButton settingsButton;
    private JButton logoutButton;

    // 控制器
    private final HomeController controller;

    /**
     * 创建Home窗口
     *
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

        // 新游戏按钮 (合并原有的New Game和Level Selection功能)
        newGameButton = FrameUtil.createStyledButton("New Game", true);
        newGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGameButton.setMaximumSize(new Dimension(200, 50));
        newGameButton.setPreferredSize(new Dimension(200, 50));

        // 加载游戏按钮
        loadGameButton = FrameUtil.createStyledButton("Load Game", true);
        loadGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadGameButton.setMaximumSize(new Dimension(200, 50));
        loadGameButton.setPreferredSize(new Dimension(200, 50));
        // 访客模式下禁用加载按钮
        loadGameButton.setEnabled(!UserSession.getInstance().isGuest());

        // 设置按钮
        settingsButton = FrameUtil.createStyledButton("Settings", false);
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsButton.setMaximumSize(new Dimension(200, 50));
        settingsButton.setPreferredSize(new Dimension(200, 50));

        // 退出登录按钮
        logoutButton = FrameUtil.createStyledButton("Logout", false);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(200, 50));
        logoutButton.setPreferredSize(new Dimension(200, 50));

        // 添加按钮到内容面板
        contentPanel.add(newGameButton);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(loadGameButton);  // 添加加载游戏按钮
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(settingsButton);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(logoutButton);

        // 添加内容面板到主面板中央
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 添加主面板到窗口
        this.add(mainPanel, BorderLayout.CENTER);

        // 添加按钮事件监听器
        newGameButton.addActionListener(e -> controller.openLevelSelect()); // 将New Game按钮的行为改为打开关卡选择
        loadGameButton.addActionListener(e -> controller.loadGame());  // 添加加载游戏按钮监听器
        settingsButton.addActionListener(e -> controller.openSettings());
        logoutButton.addActionListener(e -> controller.logout());
    }

    @Override
    public void updateUsername(String username) {
        // 更新显示的用户名
        usernameLabel.setText(username);

        // 如果是访客模式，调整UI显示并禁用加载游戏按钮
        boolean isGuest = UserSession.getInstance().isGuest();
        if (isGuest) {
            welcomeLabel.setText("Guest Mode");
            loadGameButton.setEnabled(false);
        } else {
            welcomeLabel.setText("Welcome Back");
            loadGameButton.setEnabled(true);
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
     * 实现HomeView接口方法，显示Home页面
     */
    @Override
    public void showHome() {
        this.setVisible(true);
    }

    /**
     * 重写setVisible方法，在显示主页面时更新用户信息
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            // 在重新显示时更新用户信息
            controller.initializeHome();
        }
    }

    /**
     * 初始化并显示Home页面
     */
    public void initialize() {
        controller.initializeHome();
    }
}
