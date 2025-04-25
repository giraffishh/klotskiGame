package controller;

import service.UserSession;
import view.game.GameFrame;
import view.home.HomeView;
import view.login.LoginFrame;

import javax.swing.*;

/**
 * Home页面控制器
 * 处理Home页面的业务逻辑
 */
public class HomeController {
    private final HomeView homeView;
    private GameFrame gameFrame;
    private LoginFrame loginFrame;

    /**
     * 创建HomeController
     * @param homeView Home页面视图
     */
    public HomeController(HomeView homeView) {
        this.homeView = homeView;
    }

    /**
     * 设置游戏窗口引用
     * @param gameFrame 游戏窗口实例
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }

    /**
     * 设置登录窗口引用
     * @param loginFrame 登录窗口实例
     */
    public void setLoginFrame(LoginFrame loginFrame) {
        this.loginFrame = loginFrame;
    }

    /**
     * 开始游戏
     * 显示游戏窗口并隐藏Home窗口
     */
    public void startGame() {
        if (gameFrame != null) {
            gameFrame.setVisible(true);
            homeView.closeHome();
        } else {
            homeView.showStyledMessage("Game window not properly initialized", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 退出登录
     * 清除用户会话，显示登录窗口，关闭Home窗口
     */
    public void logout() {
        // 清除用户会话
        UserSession.getInstance().logout();
        
        // 显示登录窗口
        if (loginFrame != null) {
            loginFrame.resetForm();
            loginFrame.setVisible(true);
        }
        
        // 关闭Home窗口
        homeView.closeHome();
    }

    /**
     * 初始化Home页面
     * 更新显示的用户信息
     */
    public void initializeHome() {
        // 获取当前用户名
        String username = "";
        if (UserSession.getInstance().getCurrentUser() != null) {
            username = UserSession.getInstance().getCurrentUser().getUsername();
        }

        // 更新Home页面显示的用户名
        homeView.updateUsername(username);
    }
}
