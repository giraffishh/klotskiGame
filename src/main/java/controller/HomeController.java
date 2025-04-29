package controller;

import controller.save.SaveManager;
import service.DatabaseService;
import service.UserSession;
import view.game.GameFrame;
import view.home.HomeView;
import view.level.LevelSelectFrame;
import view.login.LoginFrame;
import view.settings.SettingsFrame;
import model.MapModel;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Home页面控制器
 * 处理Home页面的业务逻辑
 */
public class HomeController {
    private final HomeView homeView;
    private GameFrame gameFrame;
    private LoginFrame loginFrame;
    private SettingsFrame settingsFrame;
    private SaveManager saveManager;
    private LevelSelectFrame levelSelectFrame;
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
        
        // 创建SaveManager实例
        if (gameFrame != null && gameFrame.getGamePanel() != null) {
            this.saveManager = new SaveManager(gameFrame.getGamePanel(), gameFrame.getGamePanel().getModel());
        }
    }

    /**
     * 设置登录窗口引用
     * @param loginFrame 登录窗口实例
     */
    public void setLoginFrame(LoginFrame loginFrame) {
        this.loginFrame = loginFrame;
    }

    /**
     * 设置设置窗口引用
     * @param settingsFrame 设置窗口实例
     */
    public void setSettingsFrame(SettingsFrame settingsFrame) {
        this.settingsFrame = settingsFrame;
    }

    /**
     * 设置关卡选择界面引用
     * @param levelSelectFrame 关卡选择窗口实例
     */
    public void setLevelSelectFrame(LevelSelectFrame levelSelectFrame) {
        this.levelSelectFrame = levelSelectFrame;
    }

    /**
     * 加载保存的游戏
     * 先检查并确认存档，确认后才显示游戏界面
     */
    public void loadGame() {
        // 检查必要对象是否已初始化
        if (gameFrame == null || saveManager == null) {
            homeView.showStyledMessage(
                "Game window or save manager not properly initialized", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 检查用户是否为访客
        if (UserSession.getInstance().isGuest()) {
            homeView.showStyledMessage(
                "Game saving and loading is not available in guest mode. Please create an account to use this feature.",
                "Guest Mode Restriction", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 检查用户是否有存档
        if (!saveManager.hasUserSave()) {
            homeView.showStyledMessage(
                "No saved game found for current user.", 
                "No Save Found", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 加载前先显示确认对话框
        boolean shouldLoad = saveManager.showLoadConfirmation();
        
        // 只有在用户确认后才进入游戏页面
        if (shouldLoad) {
            // 获取存档数据
            DatabaseService.GameSaveData saveData = saveManager.getLoadedGameData(true);

            if (saveData != null) {
                // 从存档数据创建地图模型
                MapModel mapModel = saveManager.createMapModelFromSave(saveData);

                if (mapModel != null) {
                    // 显示游戏窗口并隐藏主页面
                    gameFrame.setVisible(true);
                    homeView.closeHome();

                    // 使用loadLevel方法加载游戏（与levelSelect使用相同的方法）
                    gameFrame.loadLevel(mapModel);

                    // 设置已保存的步数
                    if (gameFrame.getGamePanel() != null) {
                        gameFrame.getGamePanel().setSteps(saveData.getSteps());
                    }

                    // 设置已保存的游戏时间
                    if (gameFrame.getController() != null) {
                        gameFrame.getController().setLoadedGameTime(saveData.getGameTime());
                    }

                    // 确保游戏面板获得焦点以接收键盘事件
                    gameFrame.getGamePanel().requestFocusInWindow();
                } else {
                    homeView.showStyledMessage(
                        "Failed to create map model from save data",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                homeView.showStyledMessage(
                    "Failed to load save data",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 打开设置页面
     * 显示设置窗口
     */
    public void openSettings() {
        if (settingsFrame != null) {
            settingsFrame.setVisible(true);
        } else {
            homeView.showStyledMessage("Settings window not properly initialized", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 打开关卡选择界面
     */
    public void openLevelSelect() {
        if (levelSelectFrame != null) {
            levelSelectFrame.showLevelSelect();
        } else {
            homeView.showStyledMessage(
                    "关卡选择窗口未正确初始化",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 退出登录
     * 清除用户会话，显示登录窗口，关闭Home窗口
     */
    public void logout() {
        if (loginFrame != null) {
            // 清除用户会话
            UserSession.getInstance().logout();
            
            // 重置登录表单，清除输入框内容
            loginFrame.resetForm();
            
            // 显示登录窗口
            loginFrame.setVisible(true);
            homeView.closeHome();
        } else {
            homeView.showStyledMessage("Login window not properly initialized", "Error", JOptionPane.ERROR_MESSAGE);
            UserSession.getInstance().logout();
            System.exit(0); // 如果loginFrame为null，则直接退出程序
        }
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

