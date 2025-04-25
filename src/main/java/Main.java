import com.formdev.flatlaf.FlatLightLaf;
import controller.util.BoardSerializer;
import model.MapModel;
import service.DatabaseService;
import service.UserSession;
import view.util.FontManager;
import view.game.GameFrame;
import view.home.HomeFrame;
import view.login.LoginFrame;
import view.util.FrameUtil;

import javax.swing.*;

public class Main {
    /**
     * 程序入口方法
     * 初始化并显示登录窗口、主页窗口和游戏窗口
     * 使用SwingUtilities.invokeLater确保UI组件在事件分发线程中创建
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 应用 FlatLaf 浅色主题
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("无法初始化 FlatLaf");
        }

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

        SwingUtilities.invokeLater(() -> {
            // 创建登录窗口并显示，增加窗口尺寸以适应更大的组件
            LoginFrame loginFrame = new LoginFrame(460, 370);

            // 创建地图模型，初始化游戏数据
            MapModel mapModel = new MapModel(new int[][]{
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
            });

            // 创建游戏窗口，增加窗口尺寸以适应更大的棋盘
            GameFrame gameFrame = new GameFrame(700, 550, mapModel);
            gameFrame.setVisible(false);

            // 创建Home窗口
            HomeFrame homeFrame = new HomeFrame();
            homeFrame.setVisible(false);

            // 设置窗口之间的相互引用
            loginFrame.setHomeFrame(homeFrame);
            homeFrame.setLoginFrame(loginFrame);
            homeFrame.setGameFrame(gameFrame);
            loginFrame.setGameFrame(gameFrame);

            // 显示登录窗口
            loginFrame.setVisible(true);
        });
    }
}
