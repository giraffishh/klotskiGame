package view.login;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import controller.core.LoginController;
import view.util.FontManager;
import view.util.FrameUtil;
import view.util.SvgIconManager;

/**
 * 登录界面窗口，提供用户名和密码输入功能 登录成功后可跳转到Home主界面 使用 FlatLaf 浅色主题
 */
public class LoginFrame extends JFrame implements LoginView {

    // 窗口尺寸常量
    private static final Dimension LOGIN_MODE_SIZE = new Dimension(480, 370);     // 登录模式窗口尺寸
    private static final Dimension REGISTER_MODE_SIZE = new Dimension(480, 420);  // 注册模式窗口尺寸

    // 用户名输入框
    private JTextField username;
    // 密码输入框
    private JPasswordField password;
    // 确认密码输入框
    private JPasswordField confirmPassword;
    // 提交按钮
    private JButton submitBtn;
    // 访客登录按钮
    private JButton guestLoginBtn;
    // 错误提示标签
    private JLabel usernameErrorLabel;
    private JLabel passwordErrorLabel;
    private JLabel confirmPasswordErrorLabel;

    // 确认密码相关面板
    private JPanel confirmPasswordPanel;
    private JPanel confirmPasswordErrorPanel;

    // 主面板
    private JPanel mainPanel;
    private JPanel formPanel;

    // 是否处于注册模式
    private boolean isRegistrationMode = false;

    // 控制器引用
    private LoginController controller;

    /**
     * 创建登录窗口
     *
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public LoginFrame(int width, int height) {
        this.setTitle("Game Login");

        // 初始化控制器
        this.controller = new LoginController(this);

        // 使用 BorderLayout 和面板组合而不是绝对布局
        this.setLayout(new BorderLayout());

        // 创建主面板，使用 BorderLayout 进行布局
        mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 30, 40, 30, 40);

        // 添加标题
        JLabel titleLabel = FrameUtil.createTitleLabel("Welcome to Game", JLabel.CENTER);
        FrameUtil.setPadding(titleLabel, 0, 0, 30, 0);

        // 将标题添加到主面板顶部
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 创建表单面板 - 改用BoxLayout替代GridLayout，更灵活地控制组件大小
        formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        FrameUtil.setPadding(formPanel, 0, 0, 20, 0);

        // 创建用户名输入面板
        username = FrameUtil.createStyledTextField(15);
        JPanel usernamePanel = FrameUtil.createInputPanel("Username:", username);
        FrameUtil.setPadding(usernamePanel, 0, 0, 0, 50);

        // 创建用户名错误提示标签 - 放在一个固定高度的面板中
        usernameErrorLabel = new JLabel("Username cannot be empty");
        usernameErrorLabel.setForeground(FrameUtil.ERROR_COLOR);
        usernameErrorLabel.setFont(FontManager.getRegularFont(12));
        usernameErrorLabel.setVisible(false);
        JPanel usernameErrorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        usernameErrorPanel.setPreferredSize(new Dimension(width, 22)); // 固定高度
        FrameUtil.setPadding(usernameErrorPanel, 0, 70, 0, 0);
        usernameErrorPanel.add(usernameErrorLabel);

        // 创建密码输入面板
        password = FrameUtil.createStyledPasswordField(15);
        JPanel passwordPanel = FrameUtil.createInputPanel("Password:", password);
        FrameUtil.setPadding(passwordPanel, 0, 0, 0, 50);

        // 创建密码错误提示标签 - 放在一个固定高度的面板中
        passwordErrorLabel = new JLabel("Password cannot be empty");
        passwordErrorLabel.setForeground(FrameUtil.ERROR_COLOR);
        passwordErrorLabel.setFont(FontManager.getRegularFont(12));
        passwordErrorLabel.setVisible(false);
        JPanel passwordErrorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        passwordErrorPanel.setPreferredSize(new Dimension(width, 22)); // 固定高度
        FrameUtil.setPadding(passwordErrorPanel, 0, 70, 0, 0);
        passwordErrorPanel.add(passwordErrorLabel);

        // 创建确认密码输入面板
        confirmPassword = FrameUtil.createStyledPasswordField(15);
        confirmPasswordPanel = FrameUtil.createInputPanel("Confirm Password:", confirmPassword);
        FrameUtil.setPadding(confirmPasswordPanel, 0, 0, 0, 50);
        confirmPasswordPanel.setVisible(false); // 初始隐藏

        // 创建确认密码错误提示标签
        confirmPasswordErrorLabel = new JLabel("Passwords do not match");
        confirmPasswordErrorLabel.setForeground(FrameUtil.ERROR_COLOR);
        confirmPasswordErrorLabel.setFont(FontManager.getRegularFont(12));
        confirmPasswordErrorLabel.setVisible(false);
        confirmPasswordErrorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        confirmPasswordErrorPanel.setPreferredSize(new Dimension(width, 22));
        FrameUtil.setPadding(confirmPasswordErrorPanel, 0, 70, 0, 0);
        confirmPasswordErrorPanel.add(confirmPasswordErrorLabel);
        confirmPasswordErrorPanel.setVisible(false); // 初始隐藏

        // 创建按钮
        submitBtn = FrameUtil.createStyledButton("Log / Reg", true, SvgIconManager.getLoginIcon());
        // 访客登录按钮
        guestLoginBtn = FrameUtil.createStyledButton("Guest Login", false, SvgIconManager.getGuestIcon());

        // 创建按钮面板
        JButton[] buttons = {submitBtn, guestLoginBtn};
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
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(confirmPasswordPanel);
        formPanel.add(Box.createVerticalStrut(0));
        formPanel.add(confirmPasswordErrorPanel);
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
                    // 检查用户名是否已注册 - 使用控制器
                    boolean userExists = controller.checkUserExists(usernameText);
                    // 根据检查结果更新按钮文本和颜色
                    if (userExists) {
                        setRegistrationMode(false); // 登录模式
                        submitBtn.setText("Login");
                        submitBtn.setBackground(FrameUtil.LOGIN_COLOR);
                        submitBtn.setMargin(new java.awt.Insets(5, 35, 5, 15));
                    } else {
                        setRegistrationMode(true); // 注册模式
                        submitBtn.setText("Register");
                        submitBtn.setBackground(FrameUtil.REGISTER_COLOR);
                        submitBtn.setMargin(new java.awt.Insets(5, 20, 5, 15));
                    }
                } else {
                    // 如果用户名为空，恢复原按钮文本和颜色
                    setRegistrationMode(false); // 隐藏确认密码区域
                    submitBtn.setText("Log / Reg");
                    submitBtn.setBackground(FrameUtil.PRIMARY_COLOR);
                    submitBtn.setMargin(new java.awt.Insets(5, 15, 5, 15));
                }
            }
        });

        // 添加提交按钮事件监听器 - 使用控制器处理逻辑
        submitBtn.addActionListener(e -> {
            String usernameText = username.getText().trim();
            String passwordText = new String(password.getPassword());
            String confirmPasswordText = new String(confirmPassword.getPassword());

            // 根据是否为注册模式调用不同的处理逻辑
            if (isRegistrationMode) {
                controller.processRegister(usernameText, passwordText, confirmPasswordText);
            } else {
                controller.processLogin(usernameText, passwordText);
            }
        });

        // 添加访客登录按钮事件监听器
        guestLoginBtn.addActionListener(e -> controller.processGuestLogin());

        // 设置初始窗口大小为登录模式尺寸
        this.setSize(LOGIN_MODE_SIZE);
        this.setLocationRelativeTo(null); // 窗口居中显示
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 设置关闭操作
    }

    /**
     * 设置是否处于注册模式，显示或隐藏确认密码区域
     *
     * @param isRegistration 是否为注册模式
     */
    public void setRegistrationMode(boolean isRegistration) {
        this.isRegistrationMode = isRegistration;
        confirmPasswordPanel.setVisible(isRegistration);
        confirmPasswordErrorPanel.setVisible(isRegistration);

        if (isRegistration) {
            // 注册模式，使用预定义的注册窗口尺寸
            this.setSize(REGISTER_MODE_SIZE);
        } else {
            // 登录模式，使用预定义的登录窗口尺寸
            this.setSize(LOGIN_MODE_SIZE);
            // 清除确认密码字段和错误提示
            confirmPassword.setText("");
            setConfirmPasswordError(false);
        }

        // 刷新窗口布局
        this.revalidate();
        this.repaint();
    }

    @Override
    public void setUsernameError(boolean isError) {
        if (isError) {
            username.setBorder(BorderFactory.createLineBorder(FrameUtil.ERROR_COLOR, 2));
            usernameErrorLabel.setVisible(true);
        } else {
            username.setBorder(UIManager.getBorder("TextField.border"));
            usernameErrorLabel.setVisible(false);
        }
    }

    @Override
    public void setPasswordError(boolean isError, String errorMessage) {
        if (isError) {
            password.setBorder(BorderFactory.createLineBorder(FrameUtil.ERROR_COLOR, 2));
            passwordErrorLabel.setText(errorMessage);
            passwordErrorLabel.setVisible(true);
        } else {
            password.setBorder(UIManager.getBorder("TextField.border"));
            passwordErrorLabel.setVisible(false);
        }
    }

    @Override
    public void setConfirmPasswordError(boolean isError) {
        if (isError) {
            confirmPassword.setBorder(BorderFactory.createLineBorder(FrameUtil.ERROR_COLOR, 2));
            confirmPasswordErrorLabel.setVisible(true);
        } else {
            confirmPassword.setBorder(UIManager.getBorder("TextField.border"));
            confirmPasswordErrorLabel.setVisible(false);
        }
    }

    @Override
    public void clearAllErrors() {
        setUsernameError(false);
        setPasswordError(false, "");
        setConfirmPasswordError(false);
    }

    @Override
    public void resetForm() {
        username.setText(""); // 清空用户名
        password.setText(""); // 清空密码
        confirmPassword.setText(""); // 清空确认密码
        clearAllErrors();    // 清除所有错误状态
        submitBtn.setText("Log / Reg");
        submitBtn.setBackground(FrameUtil.PRIMARY_COLOR);
        submitBtn.setMargin(new java.awt.Insets(5, 15, 5, 15));
        setRegistrationMode(false); // 恢复登录模式
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
                setPasswordError(false, "");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // 当内容为空时不清除错误状态
                if (password.getPassword().length > 0) {
                    setPasswordError(false, "");
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setPasswordError(false, "");
            }
        });

        // 为确认密码输入框添加文档监听器
        confirmPassword.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setConfirmPasswordError(false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // 当内容为空时不清除错误状态
                if (confirmPassword.getPassword().length > 0) {
                    setConfirmPasswordError(false);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setConfirmPasswordError(false);
            }
        });
    }

    @Override
    public void showStyledMessage(String message, String title, int messageType) {
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
        FrameUtil.initUIDefaults();
    }
}
