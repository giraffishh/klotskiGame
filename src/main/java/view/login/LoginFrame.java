package view.login;

import service.DatabaseService;
import view.util.FontManager;
import view.util.FrameUtil;
import view.game.GameFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

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
    // 错误提示标签
    private JLabel usernameErrorLabel;
    private JLabel passwordErrorLabel;

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
        
        // 创建表单面板 - 改用BoxLayout替代GridLayout，更灵活地控制组件大小
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        FrameUtil.setPadding(formPanel, 0, 0, 20, 0);
        
        // 创建用户名输入面板
        username = FrameUtil.createStyledTextField(15);
        JPanel usernamePanel = FrameUtil.createInputPanel("Username:", username);
        
        // 创建用户名错误提示标签 - 放在一个固定高度的面板中
        usernameErrorLabel = new JLabel("Username cannot be empty");
        usernameErrorLabel.setForeground(Color.RED);
        usernameErrorLabel.setFont(FontManager.getRegularFont(12));
        usernameErrorLabel.setVisible(false);
        JPanel usernameErrorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        usernameErrorPanel.setPreferredSize(new Dimension(width, 20)); // 固定高度
        usernameErrorPanel.add(usernameErrorLabel);
        
        // 创建密码输入面板
        password = FrameUtil.createStyledPasswordField(15);
        JPanel passwordPanel = FrameUtil.createInputPanel("Password:", password);
        
        // 创建密码错误提示标签 - 放在一个固定高度的面板中
        passwordErrorLabel = new JLabel("Password cannot be empty");
        passwordErrorLabel.setForeground(Color.RED);
        passwordErrorLabel.setFont(FontManager.getRegularFont(12));
        passwordErrorLabel.setVisible(false);
        JPanel passwordErrorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        passwordErrorPanel.setPreferredSize(new Dimension(width, 20)); // 固定高度
        passwordErrorPanel.add(passwordErrorLabel);
        
        // 创建按钮
        submitBtn = FrameUtil.createStyledButton("Login / Register", true);
        resetBtn = FrameUtil.createStyledButton("Reset", false);
        
        // 创建按钮面板
        JButton[] buttons = {submitBtn, resetBtn};
        JPanel buttonPanel = FrameUtil.createButtonPanel(buttons, 15);
        FrameUtil.setPadding(buttonPanel, 10, 20, 0, 20);
        
        // 将元素添加到表单面板，添加垂直间隔
        formPanel.add(usernamePanel);
        formPanel.add(Box.createVerticalStrut(0));
        formPanel.add(usernameErrorPanel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(passwordPanel);
        formPanel.add(Box.createVerticalStrut(0));
        formPanel.add(passwordErrorPanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(buttonPanel);
        
        // 将表单面板添加到主面板中心
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // 将主面板添加到窗口
        this.add(mainPanel, BorderLayout.CENTER);
        
        // 添加输入框事件监听器，用于清除错误状态
        addTextFieldListeners();
        
        // 添加用户名输入框的焦点监听器，用于检测用户名是否已注册
        username.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String usernameText = username.getText().trim();
                if (!usernameText.isEmpty()) {
                    // 检查用户名是否已注册
                    boolean userExists = DatabaseService.getInstance().checkUserExists(usernameText);
                    // 根据检查结果更新按钮文本
                    if (userExists) {
                        submitBtn.setText("Login");
                    } else {
                        submitBtn.setText("Register");
                    }
                } else {
                    // 如果用户名为空，恢复原按钮文本
                    submitBtn.setText("Login / Register");
                }
            }
        });

        // 添加提交按钮事件监听器
        submitBtn.addActionListener(e -> {
            String usernameText = username.getText().trim();
            String passwordText = new String(password.getPassword());
            
            // 重置所有错误状态
            clearAllErrors();
            
            // 校验用户名和密码不为空
            boolean hasError = false;
            
            if (usernameText.isEmpty()) {
                setUsernameError(true);
                hasError = true;
            }
            
            if (passwordText.isEmpty()) {
                setPasswordError(true);
                hasError = true;
            }
            
            // 如果有错误，不继续提交
            if (hasError) {
                return;
            }
            
            // 尝试登录或注册
            int result = DatabaseService.getInstance().loginOrRegister(usernameText, passwordText);
            switch (result) {
                case 0:
                    showStyledMessage(
                        "Login successful!", 
                        "Welcome", 
                        JOptionPane.INFORMATION_MESSAGE);
                    // 显示游戏窗口
                    if (this.gameFrame != null) {
                        this.gameFrame.setVisible(true);
                        this.setVisible(false);
                    }
                    break;
                case 1:
                    showStyledMessage(
                        "Register successfully!",
                        "Welcome",
                        JOptionPane.INFORMATION_MESSAGE);
                    // 显示游戏窗口
                    if (this.gameFrame != null) {
                        this.gameFrame.setVisible(true);
                        this.setVisible(false);
                    }
                    break;
                case 2:
                    showStyledMessage(
                        "Incorrect password",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                    break;
                default:
                    showStyledMessage(
                        "Error during login process",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // 添加重置按钮事件监听器
        resetBtn.addActionListener(e -> {
            username.setText(""); // 清空用户名
            password.setText(""); // 清空密码
            clearAllErrors();    // 清除所有错误状态
        });

        this.setSize(width, height); // 设置窗口尺寸
        this.setLocationRelativeTo(null); // 窗口居中显示
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 设置关闭操作
    }

    /**
     * 设置用户名错误状态
     * @param isError 是否显示错误
     */
    private void setUsernameError(boolean isError) {
        if (isError) {
            username.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            usernameErrorLabel.setVisible(true);
        } else {
            username.setBorder(UIManager.getBorder("TextField.border"));
            usernameErrorLabel.setVisible(false);
        }
    }
    
    /**
     * 设置密码错误状态
     * @param isError 是否显示错误
     */
    private void setPasswordError(boolean isError) {
        if (isError) {
            password.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            passwordErrorLabel.setVisible(true);
        } else {
            password.setBorder(UIManager.getBorder("TextField.border"));
            passwordErrorLabel.setVisible(false);
        }
    }
    
    /**
     * 清除所有错误状态
     */
    private void clearAllErrors() {
        setUsernameError(false);
        setPasswordError(false);
    }
    
    /**
     * 为输入框添加监听器，在用户输入时清除错误状态
     */
    private void addTextFieldListeners() {
        // 为用户名输入框添加文档监听器
        username.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setUsernameError(false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // 当内容为空时不清除错误状态
                if (username.getText().length() > 0) {
                    setUsernameError(false);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setUsernameError(false);
            }
        });
        
        // 为密码输入框添加文档监听器
        password.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setPasswordError(false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // 当内容为空时不清除错误状态
                if (password.getPassword().length > 0) {
                    setPasswordError(false);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setPasswordError(false);
            }
        });
    }

    /**
     * 显示带有自定义字体的消息对话框
     *
     * @param message 消息内容
     * @param title 对话框标题
     * @param messageType 消息类型
     */
    private void showStyledMessage(String message, String title, int messageType) {
        // 创建自定义选项面板
        JLabel label = new JLabel(message);
        label.setFont(FontManager.getRegularFont(16)); // 使用更大的字体

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
        UIManager.put("OptionPane.messageFont", null);
        UIManager.put("OptionPane.buttonFont", null);
        UIManager.put("OptionPane.titleFont", null);
    }

    /**
     * 设置游戏窗口引用，用于登录成功后的界面跳转
     * @param gameFrame 游戏主窗口实例
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }
}

