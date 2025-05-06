package controller.game.solver;

import java.util.List;

import controller.core.VictoryController;
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
    }

    /**
     * 更新最短步数显示
     *
     * @param gameTimeInMillis 当前游戏用时
     * @param moveCount 当前移动步数
     */
    public void updateMinStepsDisplay(long gameTimeInMillis, int moveCount) {
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
        }
    }
}
