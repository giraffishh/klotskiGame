
import javax.swing.SwingUtilities;

import controller.LevelSelectController;
import model.AppSettings;
import service.DatabaseService;
import service.UserSession;
import view.game.GameFrame;
import view.home.HomeFrame;
import view.level.LevelSelectFrame;
import view.login.LoginFrame;
import view.settings.SettingsFrame;
import view.util.FontManager;
import view.util.FrameUtil;

public class Main {

    /**
     * 程序入口方法 初始化并显示登录窗口、主页窗口和游戏窗口 使用SwingUtilities.invokeLater确保UI组件在事件分发线程中创建
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {

        // 确保字体已加载（FontManager 类的静态初始化块会执行字体加载）
        try {
            Class.forName(FontManager.class.getName());
        } catch (ClassNotFoundException e) {
            System.err.println("无法初始化字体管理器");
        }

        // 初始化全局UI设置（包含对话框按钮文本和字体）
        FrameUtil.initUIDefaults();

        // 初始化数据库服务
        DatabaseService.getInstance();

        // 初始化用户会话服务
        UserSession.getInstance();

        // 初始化应用程序设置管理器
        AppSettings.getInstance();

        SwingUtilities.invokeLater(() -> {
            // 创建登录窗口并显示
            LoginFrame loginFrame = new LoginFrame(460, 370);

            // 创建游戏窗口（不再直接传入地图模型）
            GameFrame gameFrame = new GameFrame(700, 550, null);
            gameFrame.setVisible(false);

            // 创建Home窗口
            HomeFrame homeFrame = new HomeFrame(500, 400);
            homeFrame.setVisible(false);

            // 创建关卡选择界面
            LevelSelectFrame levelSelectFrame = new LevelSelectFrame(700, 550);
            LevelSelectController levelSelectController = new LevelSelectController(levelSelectFrame);
            levelSelectFrame.setController(levelSelectController);

            // 创建Settings窗口
            SettingsFrame settingsFrame = new SettingsFrame(400, 300);
            settingsFrame.setVisible(false);

            // 设置窗口之间的相互引用
            loginFrame.setHomeFrame(homeFrame);
            homeFrame.setLoginFrame(loginFrame);
            homeFrame.setGameFrame(gameFrame);
            homeFrame.setSettingsFrame(settingsFrame);
            gameFrame.setHomeFrame(homeFrame);
            gameFrame.setLevelSelectFrame(levelSelectFrame);
            loginFrame.setGameFrame(gameFrame);
            homeFrame.setLevelSelectFrame(levelSelectFrame);
            levelSelectFrame.setHomeFrame(homeFrame);
            levelSelectFrame.setGameFrame(gameFrame);

            // 显示登录窗口
            loginFrame.setVisible(true);
        });
    }
}
