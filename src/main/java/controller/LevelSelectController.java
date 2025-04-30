package controller;

import java.util.ArrayList;
import java.util.List;

import controller.util.BoardSerializer;
import model.MapModel;
import view.game.GameFrame;
import view.home.HomeFrame;
import view.level.LevelSelectView;

/**
 * 关卡选择控制器 管理华容道游戏的预设关卡，提供选择和加载功能
 */
public class LevelSelectController {

    // 视图引用
    private final LevelSelectView levelSelectView;

    // 其他窗口引用
    private GameFrame gameFrame;
    private HomeFrame homeFrame;

    // 关卡列表
    private final List<LevelData> levels;

    /**
     * 创建关卡选择控制器
     *
     * @param levelSelectView 关卡选择视图
     */
    public LevelSelectController(LevelSelectView levelSelectView) {
        this.levelSelectView = levelSelectView;
        this.levels = initializeLevels();
    }

    /**
     * 初始化预设关卡
     *
     * @return 关卡数据列表
     */
    private List<LevelData> initializeLevels() {
        List<LevelData> levelList = new ArrayList<>();

        // 添加预设关卡
        // 经典横刀立马布局
        levelList.add(new LevelData(
                "Level 1",
                "Classic Layout",
                new int[][]{
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO}
                }
        ));

        // 兵临城下布局
        levelList.add(new LevelData(
                "Level 2",
                "Medium Difficulty",
                new int[][]{
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.EMPTY, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.EMPTY}
                }
        ));

        // 峰回路转
        levelList.add(new LevelData(
                "Level 3",
                "Medium Difficulty",
                new int[][]{
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.VERTICAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.SOLDIER}
                }
        ));

        // Layout 4
        levelList.add(new LevelData(
                "Level 4",
                "Hard Layout",
                new int[][]{
                    {BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.EMPTY},
                    {BoardSerializer.SOLDIER, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO, BoardSerializer.SOLDIER},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER}
                }
        ));

        // Layout 5
        levelList.add(new LevelData(
                "Level 5",
                "Hard Layout",
                new int[][]{
                    {BoardSerializer.SOLDIER, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL, BoardSerializer.VERTICAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO},
                    {BoardSerializer.EMPTY, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO}
                }
        ));

        // Layout 6
        levelList.add(new LevelData(
                "Level 6",
                "Hard Layout",
                new int[][]{
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.VERTICAL, BoardSerializer.SOLDIER, BoardSerializer.SOLDIER, BoardSerializer.VERTICAL},
                    {BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL, BoardSerializer.HORIZONTAL},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO},
                    {BoardSerializer.SOLDIER, BoardSerializer.EMPTY, BoardSerializer.CAO_CAO, BoardSerializer.CAO_CAO}
                }
        ));

        return levelList;
    }

    /**
     * 设置游戏窗口引用
     *
     * @param gameFrame 游戏窗口实例
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }

    /**
     * 设置主页窗口引用
     *
     * @param homeFrame 主页窗口实例
     */
    public void setHomeFrame(HomeFrame homeFrame) {
        this.homeFrame = homeFrame;
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

            // 停止任何可能运行的计时器
            if (gameFrame.getController() != null) {
                gameFrame.getController().stopTimer();
            }

            // 加载关卡到游戏窗口
            gameFrame.initializeGamePanel(mapModel);

            // 验证控制器是否已正确初始化
            GameController controller = gameFrame.getController();
            if (controller != null) {
                // 设置当前关卡索引
                controller.setCurrentLevelIndex(levelIndex);
                System.out.println("Current level index: " + levelIndex);

                // 确保游戏状态被完全重置
                controller.resetTimer();
            }

            // 显示游戏窗口，隐藏关卡选择窗口
            gameFrame.setVisible(true);
            levelSelectView.hideLevelSelect();

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
     * 重置所有关卡数据 当从具体关卡返回时调用，确保再次进入时为初始状态
     */
    public void resetAllLevels() {
        // 重新初始化所有关卡数据
        this.levels.clear();
        this.levels.addAll(initializeLevels());
    }

    /**
     * 返回主页
     */
    public void returnToHome() {
        if (homeFrame != null) {
            homeFrame.setVisible(true);
            levelSelectView.hideLevelSelect();
        } else {
            levelSelectView.showStyledMessage("Home window not properly initialized", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 关卡数据内部类
     */
    public static class LevelData {

        private final String name;
        private final String description;
        private final int[][] layout;

        public LevelData(String name, String description, int[][] layout) {
            this.name = name;
            this.description = description;
            this.layout = layout;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int[][] getLayout() {
            return layout;
        }
    }
}
