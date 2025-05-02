
import javax.swing.SwingUtilities;

import model.AppSettings;
import service.DatabaseService;
import service.UserSession;
import view.util.FontManager;
import view.util.FrameManager;
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
            // 使用FrameManager初始化所有窗口
            FrameManager frameManager = FrameManager.getInstance();
            frameManager.initializeAllFrames();

            // 显示登录窗口
            frameManager.showLoginFrame();
        });
    }
}
