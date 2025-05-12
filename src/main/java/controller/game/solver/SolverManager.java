package controller.game.solver;

import java.util.List;

import controller.core.VictoryController;
import controller.util.BoardSerializer;
import model.MapModel;
import view.game.GamePanel;

/**
 * 求解器管理类，负责处理与华容道求解器相关的所有操作
 */
public class SolverManager {

    private KlotskiSolver solver;
    private MapModel model;
    private GamePanel view;
    private VictoryController victoryController;
    private int[] lastCalculatedHint; // 新增：存储上一次计算的提示

    /**
     * 构造函数
     *
     * @param model 游戏地图模型
     * @param view 游戏面板视图
     * @param victoryController 胜利控制器
     */
    public SolverManager(MapModel model, GamePanel view, VictoryController victoryController) {
        this.model = model;
        this.view = view;
        this.victoryController = victoryController;
        this.solver = new KlotskiSolver();
        this.lastCalculatedHint = null; // 初始化提示

        // 初始化华容道求解器
        initializeSolver();
    }

    /**
     * 更新引用
     *
     * @param newModel 新的游戏地图模型
     * @param newView 新的游戏面板视图
     */
    public void updateReferences(MapModel newModel, GamePanel newView) {
        this.model = newModel;
        this.view = newView;
    }

    /**
     * 初始化华容道求解器并预先计算最优解
     */
    public void initializeSolver() {
        // 检查模型是否有效
        if (model == null || model.getWidth() <= 0 || model.getHeight() <= 0) {
            System.err.println("Cannot initialize solver: Invalid model.");
            if (view != null) {
                view.setMinSteps(-1); // 显示无效状态
            }
            return;
        }

        System.out.println("=== Initializing Klotski Solver ===");

        // 获取当前布局并预先计算最优解
        BoardState initialState = new BoardState(model.getSerializedLayout());

        // 重置或创建求解器实例
        this.solver = new KlotskiSolver();

        // 记录求解开始时间
        long solverStartTime = System.currentTimeMillis();

        // 执行初始求解
        List<BoardState> solution = solver.initialSolve(initialState);

        // 记录求解结束时间
        long endTime = System.currentTimeMillis();

        // 输出求解统计信息
        System.out.println("[initialSolve] Solving time: " + (endTime - solverStartTime) + " ms");
        System.out.println("[initialSolve] BFS nodes explored: " + solver.getNodesExploredBFS());

        // 输出求解结果
        if (solution != null && !solution.isEmpty()) {
            System.out.println("Optimal solution found, steps: " + (solution.size() - 1));
        } else {
            System.out.println("No solution found");
        }
        System.out.println("==============================");
        this.lastCalculatedHint = null; // 重置提示
    }

    /**
     * 更新最短步数显示，并计算下一步提示
     *
     * @param gameTimeInMillis 当前游戏用时
     * @param moveCount 当前移动步数
     */
    public void updateMinStepsDisplay(long gameTimeInMillis, int moveCount) {
        this.lastCalculatedHint = null; // 每次更新前重置提示
        try {
            // 获取当前游戏布局的序列化表示
            long currentLayout = model.getSerializedLayout();
            BoardState currentState = new BoardState(currentLayout);

            // 记录求解开始时间
            long solverStartTime = System.currentTimeMillis();

            // 使用求解器获取从当前状态到目标的路径
            List<BoardState> path = solver.findPathFrom(currentState);

            // 记录求解结束时间
            long endTime = System.currentTimeMillis();

            if (path != null && !path.isEmpty()) {
                // 路径长度减1即为所需最少步数
                int minSteps = path.size() - 1;
                view.setMinSteps(minSteps);

                // 输出当前求解信息
                System.out.println("[findPathFrom] Current layout solved in: " + (endTime - solverStartTime) + " ms");
                System.out.println("[findPathFrom] A* nodes explored: " + solver.getNodesExploredAStar());
                System.out.println("[findPathFrom] Minimum steps: " + minSteps);

                // 如果路径足够长，计算下一步提示
                if (path.size() >= 2) {
                    long currentLayoutLong = path.get(0).getLayout();
                    long nextLayoutLong = path.get(1).getLayout();
                    this.lastCalculatedHint = findMovedPieceCoordinates(currentLayoutLong, nextLayoutLong);
                } else {
                    this.lastCalculatedHint = null; // 路径不足以生成提示
                }

                // 使用胜利控制器检查胜利条件
                if (victoryController != null) {
                    // 传递当前的最小步数、游戏用时和当前步数
                    victoryController.checkVictory(
                            minSteps,
                            gameTimeInMillis,
                            moveCount
                    );
                }
            } else {
                // 如果找不到路径，显示默认值
                view.setMinSteps(-1);
                this.lastCalculatedHint = null; // 没有路径，无法提示
                System.out.println("No solution found for current layout");
            }
        } catch (Exception e) {
            System.err.println("Error calculating minimum steps: " + e.getMessage());
            // 不再使用e.printStackTrace()，而是使用更好的日志格式
            System.err.println("Stack trace: ");
            for (StackTraceElement element : e.getStackTrace()) {
                System.err.println("  at " + element);
            }
            view.setMinSteps(-1);
            this.lastCalculatedHint = null; // 出错时无法提示
        }
    }

    /**
     * 获取上一次由 updateMinStepsDisplay 计算出的下一步最佳移动的提示。
     *
     * @return 一个包含两个元素的整数数组 [row, col]，表示建议移动的棋子在当前棋盘上的一个单元格坐标。
     * 如果无法提供提示（例如，已解决、无解或路径错误），则返回 null。
     */
    public int[] getNextMoveHint() {
        if (this.lastCalculatedHint == null) {
            System.out.println("Hint: No hint available from last update or not solvable.");
        }
        return this.lastCalculatedHint;
    }

    /**
     * 比较两个棋盘布局，找出从 currentLayout 到 nextLayout 移动的棋子。 返回该棋子在 currentLayout
     * 中的一个单元格坐标。
     *
     * @param currentLayoutLong 移动前的棋盘布局（长整型）
     * @param nextLayoutLong 移动后的棋盘布局（长整型）
     * @return 一个包含 [row, col] 的数组，表示移动棋子在 currentLayout
     * 中的一个单元格坐标；如果未找到差异或出错，则返回 null。
     */
    private int[] findMovedPieceCoordinates(long currentLayoutLong, long nextLayoutLong) {
        try {
            int[][] boardBefore = BoardSerializer.deserialize(currentLayoutLong);
            int[][] boardAfter = BoardSerializer.deserialize(nextLayoutLong);

            for (int r = 0; r < BoardSerializer.ROWS; r++) {
                for (int c = 0; c < BoardSerializer.COLS; c++) {
                    int pieceInBefore = boardBefore[r][c];
                    int pieceInAfter = boardAfter[r][c];

                    // 如果一个单元格之前有棋子，之后变为空，那么这个棋子就是被移动的棋子的一部分
                    if (pieceInBefore != BoardSerializer.EMPTY && pieceInAfter == BoardSerializer.EMPTY) {
                        // 确保这个棋子确实是移动了，而不是被另一个棋子占据（虽然在标准华容道移动中不太可能）
                        // 简单起见，我们假设任何从非空到空的转变都表示原位置的棋子移动了
                        return new int[]{r, c};
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error deserializing layouts for hint: " + e.getMessage());
            return null;
        }
        // 如果没有找到这样的单元格（理论上对于有效的单步移动总会找到）
        System.err.println("Could not determine moved piece between layouts for hint.");
        return null;
    }
}
