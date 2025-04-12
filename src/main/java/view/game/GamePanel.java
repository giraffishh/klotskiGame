package view.game;

import controller.GameController;
import model.Direction;
import model.MapModel;
import view.util.FrameUtil;
import view.util.FontManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏面板类，继承自ListenerPanel，实现了键盘和鼠标事件处理。
 * 该类包含一个盒子组件列表，对应MapMatrix中的矩阵数据。
 * 负责游戏界面的显示和交互。
 */
public class GamePanel extends ListenerPanel {
    private List<BoxComponent> boxes;        // 存储所有盒子组件
    private MapModel model;                  // 游戏地图模型
    private GameController controller;       // 游戏控制器
    private JLabel stepLabel;                // 步数显示标签
    private int steps;                       // 当前步数
    private final int GRID_SIZE = 70;        // 网格大小（像素），调整为更大尺寸
    private BoxComponent selectedBox;        // 当前选中的盒子


    /**
     * 构造函数，初始化游戏面板
     * @param model 游戏地图模型
     */
    public GamePanel(MapModel model) {
        boxes = new ArrayList<>();
        this.setVisible(true);
        this.setFocusable(true);
        this.setLayout(null);
        this.setSize(model.getWidth() * GRID_SIZE + 4, model.getHeight() * GRID_SIZE + 4);
        this.model = model;
        this.selectedBox = null;
        initialGame();
    }

    /**
     * 初始化游戏，根据模型数据创建盒子组件
     * 地图示例:
     *                 {1, 2, 2, 1, 1},
     *                 {3, 4, 4, 2, 2},
     *                 {3, 4, 4, 1, 0},
     *                 {1, 2, 2, 1, 0},
     *                 {1, 1, 1, 1, 1}
     * 其中:
     * 1 - 单元格盒子 (1x1)
     * 2 - 水平盒子 (2x1)
     * 3 - 垂直盒子 (1x2)
     * 4 - 大盒子 (2x2)
     * 0 - 空白区域
     */
    public void initialGame() {
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
                    // 创建2x1粉色水平盒子
                    box = new BoxComponent(FrameUtil.PRIMARY_COLOR, i, j);
                    box.setSize(GRID_SIZE * 2, GRID_SIZE);
                    map[i][j] = 0;
                    map[i][j + 1] = 0;
                } else if (map[i][j] == 3) {
                    // 创建1x2蓝色垂直盒子
                    box = new BoxComponent(new Color(100, 149, 237), i, j);
                    box.setSize(GRID_SIZE, GRID_SIZE * 2);
                    map[i][j] = 0;
                    map[i + 1][j] = 0;
                } else if (map[i][j] == 4) {
                    // 创建2x2绿色大盒子
                    box = new BoxComponent(new Color(102, 187, 106), i, j);
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
     * @param g 图形对象
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(FrameUtil.SECONDARY_COLOR);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        this.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FrameUtil.TEXT_COLOR, 2),
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
    }

    /**
     * 处理鼠标点击事件
     * 如果点击到盒子组件，则选中或取消选中
     * @param point 点击的坐标位置
     */
    @Override
    public void doMouseClick(Point point) {
        Component component = this.getComponentAt(point);
        if (component instanceof BoxComponent clickedComponent) {
            if (selectedBox == null) {
                // 没有选中的盒子，选中当前盒子
                selectedBox = clickedComponent;
                selectedBox.setSelected(true);
            } else if (selectedBox != clickedComponent) {
                // 已有选中的盒子，切换到点击的盒子
                selectedBox.setSelected(false);
                clickedComponent.setSelected(true);
                selectedBox = clickedComponent;
            } else {
                // 点击已选中的盒子，取消选中
                clickedComponent.setSelected(false);
                selectedBox = null;
            }
        }
    }

    /**
     * 处理向右移动的事件
     * 如果有选中的盒子，则尝试向右移动
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
     * 处理向左移动的事件
     * 如果有选中的盒子，则尝试向左移动
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
     * 处理向上移动的事件
     * 如果有选中的盒子，则尝试向上移动
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
     * 处理向下移动的事件
     * 如果有选中的盒子，则尝试向下移动
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
     * @param stepLabel 步数显示标签
     */
    public void setStepLabel(JLabel stepLabel) {
        this.stepLabel = stepLabel;
        if (this.stepLabel != null) {
            this.stepLabel.setText("Steps: 0");
        }
    }

    /**
     * 设置游戏控制器
     * @param controller 游戏控制器
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }

    /**
     * 获取当前选中的盒子
     * @return 选中的盒子组件
     */
    public BoxComponent getSelectedBox() {
        return selectedBox;
    }

    /**
     * 获取网格大小
     * @return 网格大小（像素）
     */
    public int getGRID_SIZE() {
        return GRID_SIZE;
    }

    /**
     * 重置游戏面板
     * 清除所有方块组件并重新初始化
     */
    public void resetGame() {
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
}

