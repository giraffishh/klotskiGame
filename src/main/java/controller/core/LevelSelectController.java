package controller.core;

import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import model.MapModel;
import view.game.GameFrame;
import view.level.LevelSelectView;
import view.util.FrameManager;

/**
 * 关卡选择控制器 管理华容道游戏的预设关卡，提供选择和加载功能
 */
public class LevelSelectController {

    // 视图引用
    private final LevelSelectView levelSelectView;

    // 关卡列表
    private final List<LevelData> levels;

    /**
     * 创建关卡选择控制器
     *
     * @param levelSelectView 关卡选择视图
     */
    public LevelSelectController(LevelSelectView levelSelectView) {
        this.levelSelectView = levelSelectView;
        this.levels = loadLevelsFromJson();
    }

    /**
     * 从JSON文件加载关卡数据
     *
     * @return 关卡数据列表
     */
    private List<LevelData> loadLevelsFromJson() {
        List<LevelData> levelList = new ArrayList<>();

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("layouts.json");
            if (inputStream == null) {
                System.err.println("Could not find layouts.json file");
                throw new RuntimeException("Failed to load layouts.json");
            }

            ObjectMapper mapper = new ObjectMapper();
            levelList = mapper.readValue(inputStream, new TypeReference<List<LevelData>>() {
            });
            inputStream.close();

            System.out.println("Successfully loaded " + levelList.size() + " levels from JSON");
        } catch (Exception e) {
            System.err.println("Error loading levels from JSON: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load levels: " + e.getMessage());
        }

        return levelList;
    }

    /**
     * 选择并加载关卡
     *
     * @param levelIndex 关卡索引
     */
    public void selectLevel(int levelIndex) {
        // 检查索引有效性
        if (levelIndex < 0 || levelIndex >= levels.size()) {
            levelSelectView.showStyledMessage("Invalid level selection", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 通过FrameManager获取GameFrame
        FrameManager frameManager = FrameManager.getInstance();
        GameFrame gameFrame = frameManager.getGameFrame();

        if (gameFrame == null) {
            levelSelectView.showStyledMessage("Game window not properly initialized", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 获取选中的关卡数据
            LevelData selected = levels.get(levelIndex);
            if (selected == null || selected.getLayout() == null) {
                levelSelectView.showStyledMessage("Invalid level data", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 创建新的地图模型
            MapModel mapModel = new MapModel(selected.getLayout());
            // 设置模型中的关卡索引
            mapModel.setCurrentLevelIndex(levelIndex);

            // 停止任何可能运行的计时器
            if (gameFrame.getController() != null) {
                gameFrame.getController().stopTimer();
            }

            // 加载关卡到游戏窗口
            gameFrame.initializeGamePanel(mapModel);

            // 验证控制器是否已正确初始化
            GameController controller = gameFrame.getController();
            if (controller != null) {
                System.out.println("Current level index set in model: " + levelIndex);

                // 确保游戏状态被完全重置
                controller.resetTimer();
            }

            // 使用FrameManager导航到游戏界面
            frameManager.navigateFromLevelSelectToGame();

            // 确保游戏面板获得焦点以接收键盘事件
            if (gameFrame.getGamePanel() != null) {
                gameFrame.getGamePanel().requestFocusInWindow();
            }
        } catch (Exception e) {
            // 捕获所有可能的异常
            e.printStackTrace();
            levelSelectView.showStyledMessage(
                    "Failed to load level: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 返回主页
     */
    public void returnToHome() {
        // 使用FrameManager导航到主页
        FrameManager.getInstance().navigateFromLevelSelectToHome();
    }

    /**
     * 获取所有关卡数据
     *
     * @return 关卡数据列表
     */
    public List<LevelData> getLevels() {
        return levels;
    }

    /**
     * 重置所有关卡数据 当从具体关卡返回时调用，确保再次进入时为初始状态
     */
    public void resetAllLevels() {
        // 重新从JSON加载所有关卡数据
        this.levels.clear();
        this.levels.addAll(loadLevelsFromJson());
    }

    /**
     * 关卡数据内部类
     */
    public static class LevelData {

        private String name;
        private String description;
        private int[][] layout;

        // 无参构造函数，用于Jackson反序列化
        public LevelData() {
        }

        public LevelData(String name, String description, int[][] layout) {
            this.name = name;
            this.description = description;
            this.layout = layout;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int[][] getLayout() {
            return layout;
        }

        public void setLayout(int[][] layout) {
            this.layout = layout;
        }
    }
}
