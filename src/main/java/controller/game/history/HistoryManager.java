package controller.game.history;

import java.util.Stack;

import controller.util.BoardSerializer; // 新增导入
import model.MapModel;
import view.game.GameFrame;
import view.game.GamePanel;

/**
 * 历史记录管理器，负责处理游戏的撤销和重做功能
 */
public class HistoryManager {

    private GamePanel view; // 移除 final
    private MapModel model; // 移除 final
    private final Stack<MoveRecord> undoStack;
    private final Stack<MoveRecord> redoStack;
    private GameFrame parentFrame; // 父窗口引用，用于更新按钮状态

    /**
     * 构造函数
     *
     * @param view 游戏面板
     * @param model 地图模型
     */
    public HistoryManager(GamePanel view, MapModel model) {
        this.view = view;
        this.model = model;
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
    }

    /**
     * 更新内部的视图和模型引用，并清空历史记录。 当 GameController 复用并加载新关卡时调用。
     *
     * @param newView 新的游戏面板实例
     * @param newModel 新的地图模型实例
     */
    public void updateReferences(GamePanel newView, MapModel newModel) {
        this.view = newView;
        this.model = newModel;
        clearHistory(); // 模型和视图已更改，历史记录不再有效
    }

    /**
     * 设置父窗口引用
     *
     * @param frame 父窗口
     */
    public void setParentFrame(GameFrame frame) {
        this.parentFrame = frame;
        updateUndoRedoButtons(); // 初始化时更新一次按钮状态
    }

    /**
     * 记录移动操作
     */
    public void recordMove(int[][] beforeState, long layoutAfterMove) {
        // 记录移动操作 - 使用外部定义的 MoveRecord
        MoveRecord record = new MoveRecord(
                beforeState,
                layoutAfterMove
        );

        // 添加到撤销栈
        undoStack.push(record);

        // 清空重做栈，因为新的操作使之前的重做记录无效
        redoStack.clear();

        // 更新按钮状态
        updateUndoRedoButtons();
    }

    /**
     * 撤销上一次移动
     *
     * @return 撤销是否成功
     */
    public boolean undoMove() {
        // 如果没有可撤销的移动，返回失败
        if (undoStack.isEmpty()) {
            return false;
        }

        // 弹出最近的移动记录 - 使用外部定义的 MoveRecord
        MoveRecord record = undoStack.pop();

        // 保存到重做栈
        redoStack.push(record);

        // 恢复到移动前的状态
        model.setMatrix(record.getBeforeState());

        // 更新视图 (确保 view 不为 null)
        if (view != null) {
            int currentSteps = view.getSteps();
            view.resetGame(); // resetGame 会将步数置0，所以需要先保存
            view.setSteps(currentSteps - 1); // 恢复并减1
        }

        // 更新按钮状态
        updateUndoRedoButtons();

        System.out.println("Move undone successfully");
        return true;
    }

    /**
     * 重做上一次撤销的移动
     *
     * @return 重做是否成功
     */
    public boolean redoMove() {
        // 如果没有可重做的移动，返回失败
        if (redoStack.isEmpty()) {
            return false;
        }

        // 弹出最近撤销的移动记录 - 使用外部定义的 MoveRecord
        MoveRecord record = redoStack.pop();

        // 保存到撤销栈
        undoStack.push(record);

        // 应用移动后的状态
        try {
            int[][] layoutArray = BoardSerializer.deserialize(record.getLayoutAfterMove());
            model.setMatrix(layoutArray);
        } catch (IllegalArgumentException e) {
            System.err.println("Error deserializing layout during redo: " + e.getMessage());
            // 如果反序列化失败，可能需要回滚操作或采取其他错误处理
            undoStack.pop(); // 从撤销栈中移除刚刚添加的记录
            redoStack.push(record); // 将其放回重做栈
            return false;
        }

        // 更新视图 (确保 view 不为 null)
        if (view != null) {
            int currentSteps = view.getSteps();
            view.resetGame(); // resetGame 会将步数置0，所以需要先保存
            view.setSteps(currentSteps + 1); // 恢复并加1
        }

        // 更新按钮状态
        updateUndoRedoButtons();

        System.out.println("Move redone successfully");
        return true;
    }

    /**
     * 清空撤销和重做栈，并更新按钮状态
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        updateUndoRedoButtons(); // 清空后更新按钮状态
    }

    /**
     * 更新撤销和重做按钮状态
     */
    private void updateUndoRedoButtons() {
        // 添加空指针检查
        if (parentFrame != null) {
            parentFrame.updateUndoRedoButtons(canUndo(), canRedo());
        }
    }

    /**
     * 检查是否可以撤销
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * 检查是否可以重做
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 获取当前移动次数
     *
     * @return 玩家已执行的移动次数
     */
    public int getMoveCount() {
        return undoStack.size();
    }
}
