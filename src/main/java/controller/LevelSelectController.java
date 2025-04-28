package controller;

import model.MapModel;
import view.game.GameFrame;
import view.home.HomeFrame;
import view.level.LevelSelectView;
import controller.util.BoardSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * 关卡选择控制器
 * 管理华容道游戏的预设关卡，提供选择和加载功能
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
     * @param levelSelectView 关卡选择视图
     */
    public LevelSelectController(LevelSelectView levelSelectView) {
        this.levelSelectView = levelSelectView;
        this.levels = initializeLevels();
    }

    /**
     * 初始化预设关卡
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
     * @param gameFrame 游戏窗口实例
     */
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }

    /**
     * 设置主页窗口引用
     * @param homeFrame 主页窗口实例
     */
    public void setHomeFrame(HomeFrame homeFrame) {
        this.homeFrame = homeFrame;
    }

    /**
     * 获取所有关卡数据
     * @return 关卡数据列表
     */
    public List<LevelData> getLevels() {
        return levels;
    }

    /**
     * 选择并加载关卡
     * @param levelIndex 关卡索引
     */
    public void selectLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) {
            levelSelectView.showStyledMessage("Invalid level selection", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (gameFrame != null) {
            // 获取选中的关卡数据
            LevelData selected = levels.get(levelIndex);

            // 创建新的地图模型
            MapModel mapModel = new MapModel(selected.layout);

            // 加载关卡到游戏窗口
            gameFrame.loadLevel(mapModel);

            // 设置当前关卡索引
            if (gameFrame.getController() != null) {
                gameFrame.getController().setCurrentLevelIndex(levelIndex);
            }

            // 显示游戏窗口，隐藏关卡选择窗口
            gameFrame.setVisible(true);
            levelSelectView.hideLevelSelect();
        } else {
            levelSelectView.showStyledMessage("Game window not properly initialized", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 直接选择第一关
     * 供Home界面的New Game按钮调用
     */
    public void selectFirstLevel() {
        // 始终加载索引为0的第一关
        selectLevel(0);
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
