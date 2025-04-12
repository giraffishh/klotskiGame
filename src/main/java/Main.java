import com.formdev.flatlaf.FlatLightLaf;
import model.MapModel;
import service.DatabaseService;
import view.util.FontManager;
import view.game.GameFrame;
import view.login.LoginFrame;

import javax.swing.*;

public class Main {
    /**
     * 程序入口方法
     * 初始化并显示登录窗口和游戏窗口
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

        // 初始化数据库服务
        DatabaseService.getInstance();

        SwingUtilities.invokeLater(() -> {
            // 创建登录窗口并显示，增加窗口尺寸以适应更大的组件
            LoginFrame loginFrame = new LoginFrame(460, 370);
            loginFrame.setVisible(true);

            // 创建地图模型，初始化游戏数据
            MapModel mapModel = new MapModel(new int[][]{
                    {1, 2, 2, 1},
                    {1, 3, 2, 2},
                    {1, 3, 4, 4},
                    {0, 0, 4, 4}
            });

            // 创建游戏窗口，但初始不显示
            GameFrame gameFrame = new GameFrame(600, 450, mapModel);
            gameFrame.setVisible(false);

            // 设置登录窗口与游戏窗口的关联，以便登录后显示游戏
            loginFrame.setGameFrame(gameFrame);
        });
    }
}
