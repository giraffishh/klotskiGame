package view.game;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import controller.core.GameController;
import model.Direction;
import model.MapModel;
import view.util.FrameUtil;

/**
 * 游戏面板类，继承自ListenerPanel，实现了键盘和鼠标事件处理。 该类包含一个盒子组件列表，对应MapMatrix中的矩阵数据。
 * 负责游戏界面的显示和交互。
 */
public class GamePanel extends ListenerPanel {

    private List<BoxComponent> boxes;        // 存储所有盒子组件
    private MapModel model;                  // 游戏地图模型
    private GameController controller;       // 游戏控制器
    private JLabel stepLabel;                // 步数显示标签
    private JLabel minStepsLabel;            // 最短步数显示标签
    private JLabel timeLabel;                // 用时显示标签
    private int steps;                       // 当前步数
    private final int GRID_SIZE = 70;        // 网格大小（像素），调整为更大尺寸
    private BoxComponent selectedBox;        // 当前选中的盒子

    /**
     * 构造函数，初始化游戏面板
     *
     * @param model 游戏地图模型
     */
    public GamePanel(MapModel model) {
        boxes = new ArrayList<>();
        this.setVisible(true);
        this.setFocusable(true);
        this.setLayout(null);
        this.model = model;
        this.selectedBox = null;

        // 如果模型为null，设置默认大小并等待后续setModel调用
        if (model != null) {
            this.setSize(model.getWidth() * GRID_SIZE + 4, model.getHeight() * GRID_SIZE + 4);
            initialGame();
        } else {
            // 设置默认大小
            this.setSize(5 * GRID_SIZE + 4, 5 * GRID_SIZE + 4);
        }
    }

    /**
     * 设置或更新游戏地图模型
     *
     * @param model 新的游戏地图模型
     */
    public void setModel(MapModel model) {
        this.model = model;
        if (model != null) {
            this.setSize(model.getWidth() * GRID_SIZE + 4, model.getHeight() * GRID_SIZE + 4);
            resetGame(); // 重置并初始化游戏
        }
    }

    /**
     * 初始化游戏，根据模型数据创建盒子组件 地图示例: {1, 2, 2, 1, 1}, {3, 4, 4, 2, 2}, {3, 4, 4, 1,
     * 0}, {1, 2, 2, 1, 0}, {1, 1, 1, 1, 1} 其中: 1 - 单元格盒子 (1x1) 2 - 水平盒子 (2x1) 3
     * - 垂直盒子 (1x2) 4 - 大盒子 (2x2) 0 - 空白区域
     */
    public void initialGame() {
        // 确保模型不为null
        if (model == null) {
            return;
        }

        this.steps = 0;
        //复制地图数据
        int[][] map = new int[model.getHeight()][model.getWidth()];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                map[i][j] = model.getId(i, j);
            }
        }
        //构建盒子组件
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                BoxComponent box = null;
                if (map[i][j] == 1) {
                    // 创建1x1橙色盒子
                    box = new BoxComponent(FrameUtil.ACCENT_COLOR, i, j);
                    box.setSize(GRID_SIZE, GRID_SIZE);
                    map[i][j] = 0;
                } else if (map[i][j] == 2) {
                    // 创建2x1紫色水平盒子
                    box = new BoxComponent(FrameUtil.HORIZOTAL_BLOCK_COLOR, i, j);
                    box.setSize(GRID_SIZE * 2, GRID_SIZE);
                    map[i][j] = 0;
                    map[i][j + 1] = 0;
                } else if (map[i][j] == 3) {
                    // 创建1x2蓝色垂直盒子
                    box = new BoxComponent(FrameUtil.VERTICAL_BLOCK_COLOR, i, j);
                    box.setSize(GRID_SIZE, GRID_SIZE * 2);
                    map[i][j] = 0;
                    map[i + 1][j] = 0;
                } else if (map[i][j] == 4) {
                    // 创建2x2绿色大盒子
                    box = new BoxComponent(FrameUtil.BIG_BLOCK_COLOR, i, j);
                    box.setSize(GRID_SIZE * 2, GRID_SIZE * 2);
                    map[i][j] = 0;
                    map[i + 1][j] = 0;
                    map[i][j + 1] = 0;
                    map[i + 1][j + 1] = 0;
                }
                if (box != null) {
                    box.setLocation(j * GRID_SIZE + 2, i * GRID_SIZE + 2);
                    boxes.add(box);
                    this.add(box);
                }
            }
        }
        this.repaint();
    }

    /**
     * 绘制组件，设置背景和边框
     *
     * @param g 图形对象
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 使用更现代的背景效果
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 创建渐变背景 - 使用更柔和的米灰色调
        GradientPaint gradient = new GradientPaint(
                0, 0, FrameUtil.PANEL_BACKGROUND_LIGHT,
                getWidth(), getHeight(), FrameUtil.PANEL_BACKGROUND_DARK);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 如果model为null，只绘制背景和边框
        if (model == null) {
            // 边框 - 使用柔和的深灰色边框
            this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FrameUtil.PANEL_BORDER_COLOR, 2),
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
            return;
        }

        // 绘制网格线 - 使用更柔和的淡灰色线条
        g2d.setColor(FrameUtil.GRID_LINE_COLOR);
        g2d.setStroke(new BasicStroke(0.5f));

        // 绘制水平线
        for (int i = 0; i <= model.getHeight(); i++) {
            g2d.drawLine(0, i * GRID_SIZE, model.getWidth() * GRID_SIZE, i * GRID_SIZE);
        }

        // 绘制垂直线
        for (int i = 0; i <= model.getWidth(); i++) {
            g2d.drawLine(i * GRID_SIZE, 0, i * GRID_SIZE, model.getHeight() * GRID_SIZE);
        }

        // 边框 - 使用柔和的深灰色边框
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FrameUtil.PANEL_BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
    }

    /**
     * 处理鼠标点击事件 如果点击到盒子组件，则选中或取消选中
     *
     * @param point 点击的坐标位置
     */
    @Override
    public void doMouseClick(Point point) {
        // 改进点击检测：先检查视觉上包含点的方块
        BoxComponent clickedBox = null;
        for (BoxComponent box : boxes) {
            Rectangle bounds = box.getBounds();
            // 扩大点击区域，使用户更容易点击到方块边缘
            bounds.grow(2, 2);
            if (bounds.contains(point)) {
                clickedBox = box;
                break;
            }
        }

        if (clickedBox != null) {
            // 点击音效反馈可以在这里添加
            // playClickSound();

            if (selectedBox == null) {
                // 没有选中的盒子，选中当前盒子
                selectedBox = clickedBox;
                selectedBox.setSelected(true);
            } else if (selectedBox != clickedBox) {
                // 已有选中的盒子，切换到点击的盒子
                selectedBox.setSelected(false);
                clickedBox.setSelected(true);
                selectedBox = clickedBox;
            } else {
                // 点击已选中的盒子，取消选中
                clickedBox.setSelected(false);
                selectedBox = null;
            }
            // 确保面板获取焦点以接收键盘事件
            this.requestFocusInWindow();
        }
    }

    /**
     * 处理向右移动的事件 如果有选中的盒子，则尝试向右移动
     */
    @Override
    public void doMoveRight() {
        System.out.println("Click VK_RIGHT");
        if (selectedBox != null) {
            if (controller.doMove(selectedBox.getRow(), selectedBox.getCol(), Direction.RIGHT)) {
                afterMove();
            }
        }
    }

    /**
     * 处理向左移动的事件 如果有选中的盒子，则尝试向左移动
     */
    @Override
    public void doMoveLeft() {
        System.out.println("Click VK_LEFT");
        if (selectedBox != null) {
            if (controller.doMove(selectedBox.getRow(), selectedBox.getCol(), Direction.LEFT)) {
                afterMove();
            }
        }
    }

    /**
     * 处理向上移动的事件 如果有选中的盒子，则尝试向上移动
     */
    @Override
    public void doMoveUp() {
        System.out.println("Click VK_Up");
        if (selectedBox != null) {
            if (controller.doMove(selectedBox.getRow(), selectedBox.getCol(), Direction.UP)) {
                afterMove();
            }
        }
    }

    /**
     * 处理向下移动的事件 如果有选中的盒子，则尝试向下移动
     */
    @Override
    public void doMoveDown() {
        System.out.println("Click VK_DOWN");
        if (selectedBox != null) {
            if (controller.doMove(selectedBox.getRow(), selectedBox.getCol(), Direction.DOWN)) {
                afterMove();
            }
        }
    }

    /**
     * 处理撤销操作 调用控制器的撤销方法
     */
    @Override
    public void doUndo() {
        if (controller != null) {
            controller.undoMove();
        }
    }

    /**
     * 处理重做操作 调用控制器的重做方法
     */
    @Override
    public void doRedo() {
        if (controller != null) {
            controller.redoMove();
        }
    }

    /**
     * 处理胜利快捷键
     * 当按下 'Ctrl+Shift+V' 键时直接触发胜利
     */
    @Override
    public void doVictoryShortcut() {
        System.out.println("Victory shortcut pressed");
        if (controller != null) {
            controller.forceVictory();
        }
    }

    /**
     * 移动后的处理，更新步数显示
     */
    public void afterMove() {
        this.steps++;
        if (this.stepLabel != null) {
            this.stepLabel.setText(String.format("Steps: %d", this.steps));
        }
    }

    /**
     * 设置步数标签
     *
     * @param stepLabel 步数显示标签
     */
    public void setStepLabel(JLabel stepLabel) {
        this.stepLabel = stepLabel;
        if (this.stepLabel != null) {
            this.stepLabel.setText("Steps: 0");
        }
    }

    /**
     * 获取当前步数
     *
     * @return 当前步数
     */
    public int getSteps() {
        return steps;
    }

    /**
     * 设置当前步数 用于从存档加载步数或撤销/重做操作后更新步数
     *
     * @param steps 要设置的步数
     */
    public void setSteps(int steps) {
        this.steps = steps;
        if (this.stepLabel != null) {
            this.stepLabel.setText(String.format("Steps: %d", this.steps));
        }
    }

    /**
     * 设置最短步数标签
     *
     * @param minStepsLabel 最短步数显示标签
     */
    public void setMinStepsLabel(JLabel minStepsLabel) {
        this.minStepsLabel = minStepsLabel;
    }

    /**
     * 设置目标最短步数
     *
     * @param minSteps 最短步数值
     */
    public void setMinSteps(int minSteps) {
        if (this.minStepsLabel != null) {
            if (minSteps >= 0) {
                this.minStepsLabel.setText(String.format("Min Steps: %d", minSteps));
            } else {
                this.minStepsLabel.setText("Min Steps: --");
            }
        }
    }

    /**
     * 设置时间标签
     *
     * @param timeLabel 时间显示标签
     */
    public void setTimeLabel(JLabel timeLabel) {
        this.timeLabel = timeLabel;
        if (this.timeLabel != null) {
            this.timeLabel.setText("Time: 00:00.00"); // 初始显示包含厘秒
        }
    }

    /**
     * 更新时间显示
     *
     * @param timeText 要显示的时间文本
     */
    public void updateTimeDisplay(String timeText) {
        if (this.timeLabel != null) {
            // 使用 SwingUtilities.invokeLater 确保在事件调度线程中更新UI
            SwingUtilities.invokeLater(() -> this.timeLabel.setText(timeText));
        }
    }

    /**
     * 设置游戏控制器
     *
     * @param controller 游戏控制器
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }

    /**
     * 获取游戏控制器
     *
     * @return 游戏控制器
     */
    public GameController getController() {
        return controller;
    }

    /**
     * 获取当前选中的盒子
     *
     * @return 选中的盒子组件
     */
    public BoxComponent getSelectedBox() {
        return selectedBox;
    }

    /**
     * 获取网格大小
     *
     * @return 网格大小（像素）
     */
    public int getGRID_SIZE() {
        return GRID_SIZE;
    }

    /**
     * 获取所有盒子组件
     *
     * @return 盒子组件列表
     */
    public List<BoxComponent> getBoxes() {
        return boxes;
    }

    /**
     * 获取游戏地图模型
     *
     * @return 游戏地图模型
     */
    public MapModel getModel() {
        return model;
    }

    /**
     * 重置游戏面板 清除所有方块组件并重新初始化
     */
    public void resetGame() {
        // 确保模型不为null
        if (model == null) {
            return;
        }

        // 清除选中状态
        if (selectedBox != null) {
            selectedBox.setSelected(false);
            selectedBox = null;
        }

        // 清除所有方块组件
        for (BoxComponent box : boxes) {
            this.remove(box);
        }
        boxes.clear();

        // 重置步数
        this.steps = 0;
        if (this.stepLabel != null) {
            this.stepLabel.setText("Steps: 0");
        }

        // 重新初始化游戏
        initialGame();
    }

    /**
     * 获取当前游戏用时（毫秒）
     *
     * @return 游戏用时
     */
    public long getGameTime() {
        // 请求GameController提供当前游戏用时
        if (controller != null) {
            return controller.getGameTimeInMillis();
        }
        return 0;
    }

    /**
     * 设置加载的游戏时间
     *
     * @param gameTime 游戏时间（毫秒）
     */
    public void setLoadedGameTime(long gameTime) {
        // 通知GameController设置加载的游戏时间
        if (controller != null) {
            controller.setLoadedGameTime(gameTime);
        }
    }

    /**
     * 保存游戏状态，在保存前暂停计时器
     *
     * @return 保存是否成功
     */
    public boolean saveGame() {
        if (controller != null) {
            controller.saveGameState();  // GameController 会负责暂停和恢复计时器
            return true;
        }
        return false;
    }
}
