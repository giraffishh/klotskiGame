package controller.storage.save;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

import controller.util.BoardSerializer;
import model.MapModel;
import service.DatabaseService;
import service.UserSession; // 添加导入
import view.game.GamePanel;    // 添加导入

/**
 * 存档管理器，负责保存和加载游戏状态
 */
public class SaveManager {

    private GamePanel view; // 移除 final
    private MapModel model; // 移除 final
    private Runnable onLoadCompleteCallback; // 用于加载完成后通知 Controller 更新最短步数

    /**
     * 构造函数
     *
     * @param view 游戏面板
     * @param model 地图模型
     */
    public SaveManager(GamePanel view, MapModel model) {
        this.view = view;
        this.model = model;
    }

    /**
     * 更新内部的视图和模型引用。 当 GameController 复用并加载新关卡时调用。
     *
     * @param newView 新的游戏面板实例
     * @param newModel 新的地图模型实例
     */
    public void updateReferences(GamePanel newView, MapModel newModel) {
        this.view = newView;
        this.model = newModel;
    }

    /**
     * 设置加载完成后的回调函数
     *
     * @param callback 回调函数
     */
    public void setOnLoadCompleteCallback(Runnable callback) {
        this.onLoadCompleteCallback = callback;
    }

    /**
     * 显示加载确认对话框 在实际加载游戏前调用，询问用户是否加载
     *
     * @return 用户是否确认加载
     */
    public boolean showLoadConfirmation() {
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

        // 调用数据库服务加载游戏存档数据
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

            // 格式化游戏时间
            long gameTime = saveData.getGameTime();
            int minutes = (int) (gameTime / 60000);
            int seconds = (int) ((gameTime % 60000) / 1000);
            int centiseconds = (int) ((gameTime % 1000) / 10);
            String gameTimeStr = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);

            String message = String.format("Save Information:\n"
                    + " Level: %d\n"
                    + " Steps: %d\n"
                    + " Game Time: %s\n"
                    + " Save Time: %s\n\n"
                    + "Are you sure you want to load this save?\nCurrent progress will be lost.",
                    saveData.getLevelIndex() + 1, saveData.getSteps(), gameTimeStr, saveTimeStr);

            int choice = JOptionPane.showConfirmDialog(
                    view,
                    message,
                    "Confirm Load",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            // 返回用户选择结果
            return choice == JOptionPane.YES_OPTION;

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
     * 从存档数据创建地图模型
     *
     * @param skipConfirmation 是否跳过确认对话框
     * @return 加载成功则返回MapModel，失败则返回null
     */
    public DatabaseService.GameSaveData getLoadedGameData(boolean skipConfirmation) {
        // 检查用户是否为访客
        if (UserSession.getInstance().isGuest()) {
            JOptionPane.showMessageDialog(view,
                    "Unable to load game: Please login first",
                    "Load Failed",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // 检查用户是否已登录
        if (!UserSession.getInstance().isLoggedIn()) {
            System.out.println("Unable to load game: Login status is abnormal");
            JOptionPane.showMessageDialog(view,
                    "Unable to load game: You are not logged in",
                    "Load Failed",
                    JOptionPane.ERROR_MESSAGE);
            return null;
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
            return null;
        }

        try {
            // 仅当需要确认时才显示确认对话框
            if (!skipConfirmation) {
                if (!showLoadConfirmation()) {
                    return null;
                }
            }

            return saveData;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view,
                    "Error parsing saved game data: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * 保存当前游戏状态到数据库 检查用户是否有已存在的存档，提示新建或覆盖
     *
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

        // 添加模型空值检查
        if (model == null) {
            System.err.println("Cannot save game state: Model is null.");
            JOptionPane.showMessageDialog(view,
                    "Save failed: Game data is missing.",
                    "Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // 添加视图空值检查 (虽然不太可能在这里为null)
        if (view == null) {
            System.err.println("Cannot save game state: View is null.");
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

        // 获取当前游戏用时（毫秒）
        long gameTime = view.getGameTime();

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
            // 直接从模型获取当前关卡索引
            int levelIndex = model.getCurrentLevelIndex();

            // 调用数据库服务保存游戏状态，包含关卡索引
            boolean saved = DatabaseService.getInstance().saveGameState(username, mapState, steps, gameTime, levelIndex, description);

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

    /**
     * 从存档数据创建地图模型
     *
     * @param saveData 存档数据
     * @return 创建的地图模型，如果创建失败返回null
     */
    public MapModel createMapModelFromSave(DatabaseService.GameSaveData saveData) {
        if (saveData == null) {
            return null;
        }

        try {
            // 将字符串类型的地图状态转换为长整型
            long mapStateLong = Long.parseLong(saveData.getMapState());

            // 使用序列化工具将长整型转为矩阵
            int[][] newMatrix = BoardSerializer.deserialize(mapStateLong);

            // 创建地图模型，同时设置关卡索引和加载标志
            MapModel newModel = new MapModel(newMatrix, saveData.getLevelIndex());
            newModel.setLoadedFromSave(true);

            return newModel;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view,
                    "Error creating map model from save data: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
