package view.login;

import view.util.FontManager;
import view.util.FrameUtil;
import view.game.GameFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 登录界面窗口，提供用户名和密码输入功能
 * 登录成功后可跳转到游戏主界面
 * 使用 FlatLaf 浅色主题
 */
public class LoginFrame extends JFrame {
    // 用户名输入框
    private JTextField username;
    // 密码输入框
    private JPasswordField password;
    // 提交按钮
    private JButton submitBtn;
    // 重置按钮
    private JButton resetBtn;
    // 游戏主窗口引用
    private GameFrame gameFrame;

    /**
     * 创建登录窗口
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public LoginFrame(int width, int height) {
        this.setTitle("Game Login"); // Set window title
        
        // 使用 BorderLayout 和面板组合而不是绝对布局
        this.setLayout(new BorderLayout());
        
        // 创建主面板，使用 BorderLayout 进行布局
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 30, 40, 30, 40);
        
        // 添加标题
        JLabel titleLabel = FrameUtil.createTitleLabel("Welcome to Game", JLabel.CENTER);
        FrameUtil.setPadding(titleLabel, 0, 0, 30, 0);
        
        // 将标题添加到主面板顶部
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 创建表单面板
        JPanel formPanel = FrameUtil.createPaddedPanel(new GridLayout(3, 1, 0, 20), 0, 0, 20, 0);
        
        // 创建用户名输入面板
        username = FrameUtil.createStyledTextField(15);
        JPanel usernamePanel = FrameUtil.createInputPanel("Username:", username);
        
        // 创建密码输入面板
        password = FrameUtil.createStyledPasswordField(15);
        JPanel passwordPanel = FrameUtil.createInputPanel("Password:", password);
        
        // 创建按钮
        submitBtn = FrameUtil.createStyledButton("Login", true);
        resetBtn = FrameUtil.createStyledButton("Reset", false);
        
        // 创建按钮面板
        JButton[] buttons = {submitBtn, resetBtn};
        JPanel buttonPanel = FrameUtil.createButtonPanel(buttons, 15);
        FrameUtil.setPadding(buttonPanel, 10, 20, 0, 20);
        
        // 将元素添加到表单面板
        formPanel.add(usernamePanel);
        formPanel.add(passwordPanel);
        formPanel.add(buttonPanel);
        
        // 将表单面板添加到主面板中心
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // 将主面板添加到窗口
        this.add(mainPanel, BorderLayout.CENTER);
        
        // 添加提交按钮事件监听器
        submitBtn.addActionListener(e -> {
            System.out.println("Username = " + username.getText());
            System.out.println("Password = " + new String(password.getPassword()));
            
            // 简单的校验
            if (username.getText().trim().isEmpty() || password.getPassword().length == 0) {
                JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "登录失败", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // 如果游戏窗口已创建，则显示游戏窗口并隐藏登录窗口
            if (this.gameFrame != null) {
                this.gameFrame.setVisible(true);
                this.setVisible(false);
            }
        });
        
        // 添加重置按钮事件监听器
        resetBtn.addActionListener(e -> {
            username.setText(""); // 清空用户名
            password.setText(""); // 清空密码
        });

        this.setSize(width, height); // 设置窗口尺寸
        this.setLocationRelativeTo(null); // 窗口居中显示
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 设置关闭操作
    }

    /**
     * 设置游戏窗口引用，用于登录成功后的界面跳转
     * @param gameFrame 游戏主窗口实例
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }
}
