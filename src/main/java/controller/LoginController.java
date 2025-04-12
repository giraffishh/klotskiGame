package controller;

import service.DatabaseService;
import view.game.GameFrame;
import view.login.LoginView;

import javax.swing.*;

/**
 * 登录控制器，处理登录和注册相关的业务逻辑
 * 遵循MVC设计模式，将业务逻辑与UI显示分离
 */
public class LoginController {
    // 数据库服务
    private final DatabaseService databaseService;
    // 视图引用
    private final LoginView loginView;
    // 游戏窗口引用
    private GameFrame gameFrame;

    /**
     * 创建登录控制器
     * @param loginView 登录视图
     */
    public LoginController(LoginView loginView) {
        this.loginView = loginView;
        this.databaseService = DatabaseService.getInstance();
    }

    /**
     * 设置游戏窗口引用
     * @param gameFrame 游戏主窗口
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }

    /**
     * 检查用户是否存在
     * @param username 用户名
     * @return true: 用户已注册; false: 用户未注册
     */
    public boolean checkUserExists(String username) {
        return databaseService.checkUserExists(username);
    }

    /**
     * 验证表单输入
     * @param username 用户名
     * @param password 密码
     * @return true: 验证通过; false: 验证失败
     */
    public boolean validateForm(String username, String password) {
        boolean isValid = true;
        
        // 验证用户名
        if (username.isEmpty()) {
            loginView.setUsernameError(true);
            isValid = false;
        }
        
        // 验证密码
        if (password.isEmpty()) {
            loginView.setPasswordError(true);
            isValid = false;
        }
        
        return isValid;
    }

    /**
     * 处理登录或注册请求
     * @param username 用户名
     * @param password 密码
     */
    public void processLoginOrRegister(String username, String password) {
        // 清除所有错误状态
        loginView.clearAllErrors();
        
        // 验证表单
        if (!validateForm(username, password)) {
            return;
        }
        
        // 尝试登录或注册
        int result = databaseService.loginOrRegister(username, password);
        switch (result) {
            case 0:
                loginView.showStyledMessage(
                    "Login successful!", 
                    "Welcome", 
                    JOptionPane.INFORMATION_MESSAGE);
                // 显示游戏窗口
                if (this.gameFrame != null) {
                    this.gameFrame.setVisible(true);
                    loginView.setVisible(false);
                }
                break;
            case 1:
                loginView.showStyledMessage(
                    "Register successfully!",
                    "Welcome",
                    JOptionPane.INFORMATION_MESSAGE);
                // 显示游戏窗口
                if (this.gameFrame != null) {
                    this.gameFrame.setVisible(true);
                    loginView.setVisible(false);
                }
                break;
            case 2:
                loginView.showStyledMessage(
                    "Incorrect password",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
                break;
            default:
                loginView.showStyledMessage(
                    "Error during login process",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 重置表单
     */
    public void resetForm() {
        loginView.resetForm();
    }
}
