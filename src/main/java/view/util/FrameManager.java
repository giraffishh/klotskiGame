package view.util;

import controller.core.LevelSelectController;
import controller.game.sound.SoundManager;
import model.AppSettings;
import view.game.GameFrame;
import view.home.HomeFrame;
import view.level.LevelSelectFrame;
import view.login.LoginFrame;
import view.settings.SettingsFrame;

/**
 * 窗口管理器类 负责创建和管理所有窗口实例，以及它们之间的相互引用
 */
public class FrameManager {

    // 单例实例
    private static FrameManager instance;

    // 所有窗口实例
    private LoginFrame loginFrame;
    private HomeFrame homeFrame;
    private GameFrame gameFrame;
    private LevelSelectFrame levelSelectFrame;
    private SettingsFrame settingsFrame;
    
    // 添加音效管理器实例
    private SoundManager soundManager;

    /**
     * 私有构造函数，确保只能通过getInstance()方法获取实例
     */
    private FrameManager() {
        // 私有构造函数，防止外部直接创建实例
        soundManager = new SoundManager(); // 初始化音效管理器
    }

    /**
     * 获取单例实例
     *
     * @return FrameManager实例
     */
    public static synchronized FrameManager getInstance() {
        if (instance == null) {
            instance = new FrameManager();
        }
        return instance;
    }

    /**
     * 初始化所有窗口
     */
    public void initializeAllFrames() {
        // 创建登录窗口
        loginFrame = new LoginFrame(460, 370);

        // 创建游戏窗口（不传入地图模型）
        gameFrame = new GameFrame(720, 550, null);
        gameFrame.setVisible(false);

        // 创建主页窗口
        homeFrame = new HomeFrame(500, 400);
        homeFrame.setVisible(false);

        // 创建关卡选择界面
        levelSelectFrame = new LevelSelectFrame(720, 550);
        LevelSelectController levelSelectController = new LevelSelectController(levelSelectFrame);
        levelSelectFrame.setController(levelSelectController);
        levelSelectFrame.setVisible(false);

        // 创建设置窗口
        settingsFrame = new SettingsFrame(500, 400);
        settingsFrame.setVisible(false);
    }

    // Getters for all frames
    public LoginFrame getLoginFrame() {
        return loginFrame;
    }

    public HomeFrame getHomeFrame() {
        return homeFrame;
    }

    public GameFrame getGameFrame() {
        return gameFrame;
    }

    public LevelSelectFrame getLevelSelectFrame() {
        return levelSelectFrame;
    }

    public SettingsFrame getSettingsFrame() {
        return settingsFrame;
    }

    /**
     * 显示登录窗口
     */
    public void showLoginFrame() {
        loginFrame.setVisible(true);
    }

    /**
     * 显示主页窗口
     */
    public void showHomeFrame() {
        homeFrame.setVisible(true);
    }

    /**
     * 显示游戏窗口
     */
    public void showGameFrame() {
        gameFrame.setVisible(true);
    }

    /**
     * 显示关卡选择窗口
     */
    public void showLevelSelectFrame() {
        if (levelSelectFrame != null) {
            levelSelectFrame.showLevelSelect(); // 使用自定义显示方法
            levelSelectFrame.toFront(); // 确保窗口显示在最前面
            levelSelectFrame.requestFocus(); // 尝试请求焦点
        } else {
            System.err.println("Attempted to show LevelSelectFrame, but it is null.");
        }
    }

    /**
     * 显示设置窗口
     */
    public void showSettingsFrame() {
        settingsFrame.setVisible(true);
    }

    /**
     * 关闭登录窗口
     */
    public void hideLoginFrame() {
        loginFrame.setVisible(false);
    }

    /**
     * 关闭主页窗口
     */
    public void hideHomeFrame() {
        homeFrame.setVisible(false);
    }

    /**
     * 关闭游戏窗口
     */
    public void hideGameFrame() {
        gameFrame.setVisible(false);
    }

    /**
     * 关闭关卡选择窗口
     */
    public void hideLevelSelectFrame() {
        levelSelectFrame.hideLevelSelect(); // 使用自定义隐藏方法
    }

    /**
     * 关闭设置窗口
     */
    public void hideSettingsFrame() {
        settingsFrame.setVisible(false);
    }

    /**
     * 从登录界面跳转到主页界面
     */
    public void navigateFromLoginToHome() {
        hideLoginFrame();
        if (homeFrame != null) {
            homeFrame.initialize(); // 初始化Home页面
        }
        showHomeFrame();
        
        // 根据用户设置决定是否播放背景音乐
        if (AppSettings.getInstance().isMusicEnabled()) {
            startBackgroundMusic();
        } else {
            stopBackgroundMusic(); // 确保音乐停止
        }
    }

    /**
     * 从主页界面退出到登录界面
     */
    public void navigateFromHomeToLogin() {
        hideHomeFrame();
        if (loginFrame != null) {
            loginFrame.resetForm(); // 重置登录表单
        }
        showLoginFrame();
    }

    /**
     * 从主页界面导航到游戏界面
     */
    public void navigateFromHomeToGame() {
        hideHomeFrame();
        showGameFrame();
    }

    /**
     * 从主页界面导航到关卡选择界面
     */
    public void navigateFromHomeToLevelSelect() {
        hideHomeFrame();
        showLevelSelectFrame();
    }

    /**
     * 从主页界面导航到设置界面
     */
    public void navigateFromHomeToSettings() {
        hideHomeFrame();
        showSettingsFrame();
    }

    /**
     * 从游戏界面返回主页界面
     */
    public void navigateFromGameToHome() {
        hideGameFrame();
        showHomeFrame();
    }

    /**
     * 从游戏界面导航到关卡选择界面
     */
    public void navigateFromGameToLevelSelect() {
        hideGameFrame(); // 确保游戏窗口是隐藏的
        showLevelSelectFrame(); // 显示关卡选择窗口并置于顶层
    }

    /**
     * 从关卡选择界面返回主页界面
     */
    public void navigateFromLevelSelectToHome() {
        hideLevelSelectFrame();
        showHomeFrame();
    }

    /**
     * 从关卡选择界面导航到游戏界面
     */
    public void navigateFromLevelSelectToGame() {
        hideLevelSelectFrame();
        showGameFrame();
    }

    /**
     * 从设置界面返回主页界面
     */
    public void navigateFromSettingsToHome() {
        // 确保设置已保存
        if (AppSettings.getInstance().hasUnsavedChanges()) {
            AppSettings.getInstance().saveAllSettings();
        }
        
        hideSettingsFrame();
        showHomeFrame();
    }

    /**
     * 关闭所有窗口并显示登录窗口(登出时使用)
     */
    public void logoutToLoginScreen() {
        // 确保在登出前保存设置
        if (AppSettings.getInstance().hasUnsavedChanges()) {
            AppSettings.getInstance().saveAllSettings();
        }
        
        hideHomeFrame();
        hideGameFrame();
        hideLevelSelectFrame();
        hideSettingsFrame();

        if (loginFrame != null) {
            loginFrame.resetForm(); // 重置登录表单
            showLoginFrame();
        }
        
        // 停止背景音乐
        stopBackgroundMusic();
    }

    /**
     * 关闭所有窗口
     */
    public void closeAllFrames() {
        // 保存未保存的设置
        if (AppSettings.getInstance().hasUnsavedChanges()) {
            AppSettings.getInstance().saveAllSettings();
        }
        
        hideHomeFrame();
        hideGameFrame();
        hideLevelSelectFrame();
        hideSettingsFrame();
        hideLoginFrame();
    }

    /**
     * 刷新游戏界面
     * 用于应用主题变更等设置
     */
    public void refreshGameInterface() {
        if (gameFrame != null && gameFrame.isVisible()) {
            gameFrame.refreshGamePanel();
            // 确保控制按钮更新
            gameFrame.updateControlButtonsVisibility();
        }
        
        // 刷新关卡选择界面（如果正在显示）
        if (levelSelectFrame != null && levelSelectFrame.isVisible()) {
            levelSelectFrame.refreshDisplay();
        }
    }

    /**
     * 获取音效管理器实例
     * 
     * @return 音效管理器实例
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }
    
    /**
     * 开始播放背景音乐
     */
    public void startBackgroundMusic() {
        if (soundManager != null) {
            soundManager.startBackgroundMusic();
        }
    }
    
    /**
     * 停止背景音乐
     */
    public void stopBackgroundMusic() {
        if (soundManager != null) {
            soundManager.stopBackgroundMusic();
        }
    }
    
    /**
     * 暂停背景音乐
     */
    public void pauseBackgroundMusic() {
        if (soundManager != null) {
            soundManager.pauseBackgroundMusic();
        }
    }
    
    /**
     * 恢复播放背景音乐
     */
    public void resumeBackgroundMusic() {
        if (soundManager != null) {
            soundManager.resumeBackgroundMusic();
        }
    }
}
