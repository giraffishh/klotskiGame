package controller;

import javax.swing.JOptionPane;

import controller.save.SaveManager;
import model.MapModel;
import service.DatabaseService;
import service.UserSession;
import view.home.HomeView;
import view.util.FrameManager;

/**
 * Home页面控制器 处理Home页面的业务逻辑
 */
public class HomeController {

    private final HomeView homeView;
    private SaveManager saveManager;

    /**
     * 创建HomeController
     *
     * @param homeView Home页面视图
     */
    public HomeController(HomeView homeView) {
        this.homeView = homeView;

        // 创建SaveManager实例
        FrameManager frameManager = FrameManager.getInstance();
        if (frameManager.getGameFrame() != null && frameManager.getGameFrame().getGamePanel() != null) {
            this.saveManager = new SaveManager(
                    frameManager.getGameFrame().getGamePanel(),
                    frameManager.getGameFrame().getGamePanel().getModel()
            );
        }
    }

    /**
     * 加载保存的游戏 先检查并确认存档，确认后才显示游戏界面
     */
    public void loadGame() {
        FrameManager frameManager = FrameManager.getInstance();

        // 检查必要对象是否已初始化
        if (frameManager.getGameFrame() == null || saveManager == null) {
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
                    // 使用FrameManager导航到游戏界面
                    frameManager.navigateFromHomeToGame();

                    // 使用loadLevel方法加载游戏
                    frameManager.getGameFrame().initializeGamePanel(mapModel);

                    // 设置已保存的步数
                    if (frameManager.getGameFrame().getGamePanel() != null) {
                        frameManager.getGameFrame().getGamePanel().setSteps(saveData.getSteps());
                    }

                    // 设置已保存的游戏时间
                    if (frameManager.getGameFrame().getController() != null) {
                        frameManager.getGameFrame().getController().setLoadedGameTime(saveData.getGameTime());
                    }

                    // 确保游戏面板获得焦点以接收键盘事件
                    frameManager.getGameFrame().getGamePanel().requestFocusInWindow();
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
     * 打开设置页面 显示设置窗口
     */
    public void openSettings() {
        FrameManager frameManager = FrameManager.getInstance();
        frameManager.navigateFromHomeToSettings();
    }

    /**
     * 打开关卡选择界面
     */
    public void openLevelSelect() {
        FrameManager frameManager = FrameManager.getInstance();
        frameManager.navigateFromHomeToLevelSelect();
    }

    /**
     * 退出登录 清除用户会话，显示登录窗口，关闭Home窗口
     */
    public void logout() {
        // 清除用户会话
        UserSession.getInstance().logout();

        // 使用FrameManager关闭所有窗口并显示登录窗口
        FrameManager frameManager = FrameManager.getInstance();
        frameManager.logoutToLoginScreen();
    }

    /**
     * 初始化Home页面 更新显示的用户信息
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
