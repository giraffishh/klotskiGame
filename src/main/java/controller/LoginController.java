package controller;

import javax.swing.JOptionPane;

import model.User;
import service.DatabaseService;
import service.UserSession;
import view.login.LoginView;
import view.util.FrameManager;

/**
 * 登录控制器，处理登录和注册相关的业务逻辑 遵循MVC设计模式，将业务逻辑与UI显示分离
 */
public class LoginController {

    // 数据库服务
    private final DatabaseService databaseService;
    // 用户会话
    private final UserSession userSession;
    // 视图引用
    private final LoginView loginView;

    /**
     * 创建登录控制器
     *
     * @param loginView 登录视图
     */
    public LoginController(LoginView loginView) {
        this.loginView = loginView;
        this.databaseService = DatabaseService.getInstance();
        this.userSession = UserSession.getInstance();
    }

    /**
     * 检查用户是否存在
     *
     * @param username 用户名
     * @return true: 用户已注册; false: 用户未注册
     */
    public boolean checkUserExists(String username) {
        return databaseService.checkUserExists(username);
    }

    /**
     * 验证登录表单输入
     *
     * @param username 用户名
     * @param password 密码
     * @return true: 验证通过; false: 验证失败
     */
    private boolean validateLoginForm(String username, String password) {
        boolean isValid = true;

        // 验证用户名
        if (username.isEmpty()) {
            loginView.setUsernameError(true);
            isValid = false;
        }

        // 验证密码
        if (password.isEmpty()) {
            loginView.setPasswordError(true, "Password cannot be empty");
            isValid = false;
        }

        return isValid;
    }

    /**
     * 验证注册表单输入
     *
     * @param username 用户名
     * @param password 密码
     * @param confirmPassword 确认密码
     * @return true: 验证通过; false: 验证失败
     */
    private boolean validateRegisterForm(String username, String password, String confirmPassword) {
        boolean isValid = true;

        // 验证用户名
        if (username.isEmpty()) {
            loginView.setUsernameError(true);
            isValid = false;
        }

        // 验证密码
        if (password.isEmpty()) {
            loginView.setPasswordError(true, "Password cannot be empty");
            isValid = false;
        }

        // 验证确认密码
        if (confirmPassword.isEmpty()) {
            loginView.setConfirmPasswordError(true);
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            loginView.setConfirmPasswordError(true);
            isValid = false;
        }

        return isValid;
    }

    /**
     * 处理登录请求
     *
     * @param username 用户名
     * @param password 密码
     */
    public void processLogin(String username, String password) {
        // 清除所有错误状态
        loginView.clearAllErrors();

        // 验证表单
        if (!validateLoginForm(username, password)) {
            return;
        }

        // 检查用户是否存在
        if (!databaseService.checkUserExists(username)) {
            loginView.showStyledMessage(
                    "User does not exist",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 尝试登录
        int result = databaseService.loginOrRegister(username, password);
        if (result == 0) {
            User loggedInUser = userSession.getCurrentUser();
            loginView.showStyledMessage(
                    "Login successful!",
                    "Welcome",
                    JOptionPane.INFORMATION_MESSAGE);

            // 使用FrameManager导航到Home页面
            FrameManager.getInstance().navigateFromLoginToHome();
        } else {
            // 修改：不再弹窗，而是直接在密码输入框下方显示错误信息
            loginView.setPasswordError(true, "Incorrect password");
        }
    }

    /**
     * 处理注册请求
     *
     * @param username 用户名
     * @param password 密码
     * @param confirmPassword 确认密码
     */
    public void processRegister(String username, String password, String confirmPassword) {
        // 清除所有错误状态
        loginView.clearAllErrors();

        // 验证表单
        if (!validateRegisterForm(username, password, confirmPassword)) {
            return;
        }

        // 检查用户是否已存在
        if (databaseService.checkUserExists(username)) {
            loginView.showStyledMessage(
                    "Username already exists",
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 尝试注册
        int result = databaseService.loginOrRegister(username, password);
        if (result == 1) {
            User newUser = userSession.getCurrentUser();
            loginView.showStyledMessage(
                    "Register successfully!",
                    "Welcome",
                    JOptionPane.INFORMATION_MESSAGE);

            // 使用FrameManager导航到Home页面
            FrameManager.getInstance().navigateFromLoginToHome();
        } else {
            loginView.showStyledMessage(
                    "Error during registration",
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 处理访客登录请求
     */
    public void processGuestLogin() {
        // 设置访客状态
        User guestUser = new User("Guest", "");
        userSession.setCurrentUser(guestUser);
        userSession.setGuest(true);

        loginView.showStyledMessage(
                "Logged in as guest",
                "Welcome Guest",
                JOptionPane.INFORMATION_MESSAGE);

        // 使用FrameManager导航到Home页面
        FrameManager.getInstance().navigateFromLoginToHome();
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户，如未登录返回null
     */
    public User getCurrentUser() {
        return userSession.getCurrentUser();
    }

    /**
     * 重置表单
     */
    public void resetForm() {
        loginView.resetForm();
    }
}
