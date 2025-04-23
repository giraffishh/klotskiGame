package controller.save;

import controller.util.BoardSerializer;
import model.MapModel;
import service.DatabaseService;
import service.UserSession;
import view.game.GamePanel;

import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 游戏状态管理类，负责游戏的保存、加载和存档检测功能
 */
public class SaveManager {
    private final GamePanel view;
    private final MapModel model;
    // 添加一个回调接口，用于通知加载完成后需要更新最短步数
    private Runnable onLoadCompleteCallback;

    /**
     * 构造函数
     * 
     * @param view 游戏面板视图
     * @param model 地图数据模型
     */
    public SaveManager(GamePanel view, MapModel model) {
        this.view = view;
        this.model = model;
    }

    /**
     * 设置加载完成后的回调函数
     * @param callback 回调函数
     */
    public void setOnLoadCompleteCallback(Runnable callback) {
        this.onLoadCompleteCallback = callback;
    }

    /**
     * 加载游戏存档
     * 从数据库中读取存档并验证完整性
     *
     * @return 加载是否成功
     */
    public boolean loadGameState() {
        // 检查用户是否为访客
        if (UserSession.getInstance().isGuest()) {
            JOptionPane.showMessageDialog(view,
                    "Unable to load game: Please login first",
                    "Load Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 检查用户是否已登录
        if (!UserSession.getInstance().isLoggedIn()) {
            System.out.println("Unable to load game: Login status is abnormal");
            JOptionPane.showMessageDialog(view,
                    "Unable to load game: You are not logged in",
                    "Load Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 获取当前登录用户名
        String username = UserSession.getInstance().getCurrentUser().getUsername();

        // 调用数据库服务加载游戏状态
        DatabaseService.GameSaveData saveData = DatabaseService.getInstance().loadGameSave(username);

        if (saveData == null) {
            JOptionPane.showMessageDialog(view,
                    "No valid save found or save data is corrupted",
                    "Load Failed",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            // 创建一个包含存档信息的确认对话框
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String saveTimeStr = dateFormat.format(saveData.getSaveTime());

            String message = String.format("Save Information:\n" +
                                          " Steps: %d\n" +
                                          " Save Time: %s\n\n" +
                                          "Are you sure you want to load this save?\nCurrent progress will be lost.",
                                          saveData.getSteps(), saveTimeStr);

            int choice = JOptionPane.showConfirmDialog(
                view,
                message,
                "Confirm Load",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            // 如果用户取消了加载，则返回
            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }

            // 用户确认加载，继续处理存档数据
            String mapState = saveData.getMapState();
            int steps = saveData.getSteps();

            // 将字符串类型的地图状态转换为长整型
            long mapStateLong = Long.parseLong(mapState);

            // 使用序列化工具将长整型转为矩阵
            int[][] newMatrix = BoardSerializer.deserialize(mapStateLong);

            // 更新模型数据
            model.setMatrix(newMatrix);

            // 重置游戏面板显示新地图
            view.resetGame();

            // 设置已加载的步数
            view.setSteps(steps);

            javax.swing.SwingUtilities.invokeLater(() -> {

                // 在显示成功消息之前调用回调函数更新最短步数
                if (onLoadCompleteCallback != null) {
                    onLoadCompleteCallback.run();
                }

                JOptionPane.showMessageDialog(view,
                        "Game loaded successfully!",
                        "Load Success",
                        JOptionPane.INFORMATION_MESSAGE);

            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view,
                    "Error parsing saved game data: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * 保存当前游戏状态到数据库
     * 检查用户是否有已存在的存档，提示新建或覆盖
     * @return 保存是否成功
     */
    public boolean saveGameState() {
        // 检查用户是否为访客
        if (UserSession.getInstance().isGuest()) {
            JOptionPane.showMessageDialog(view,
                    "Unable to save game: Please login first",
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 检查用户是否已登录
        if (!UserSession.getInstance().isLoggedIn()) {
            System.out.println("Unable to save game: Login status is abnormal");
            return false;
        }

        // 获取当前登录用户名
        String username = UserSession.getInstance().getCurrentUser().getUsername();

        // 使用序列化工具将地图状态转换为长整型
        long mapStateLong = BoardSerializer.serialize(model.getMatrix());

        // 将长整型转换为字符串以便存储
        String mapState = String.valueOf(mapStateLong);

        // 获取当前步数
        int steps = view.getSteps();

        // 生成存档描述信息（使用当前日期时间）
        String description = "Saved at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 判断用户是否已有存档
        boolean hasExistingSave = hasUserSave();
        String message;
        String title;

        if (hasExistingSave) {
            message = "You already have a saved game. Do you want to overwrite it?";
            title = "Overwrite Save";
        } else {
            message = "Do you want to create a new save?";
            title = "Create Save";
        }

        int result = JOptionPane.showConfirmDialog(view, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // 调用数据库服务保存游戏状态
            boolean saved = DatabaseService.getInstance().saveGameState(username, mapState, steps, description);

            // 显示保存结果
            if (saved) {
                JOptionPane.showMessageDialog(view,
                        hasExistingSave ? "Save successfully overwritten !" : "New save successfully created!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(view,
                        "Save failed. Please make sure you are logged in.",
                        "Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
            
            return saved;
        }
        
        return false;
    }

    /**
     * 检查当前用户是否已有存档
     *
     * @return 用户是否已有存档
     */
    public boolean hasUserSave() {
        if (!UserSession.getInstance().isLoggedIn() || UserSession.getInstance().isGuest()) {
            return false;
        }

        String username = UserSession.getInstance().getCurrentUser().getUsername();
        return DatabaseService.getInstance().hasUserGameSave(username);
    }
}

