package controller.game.history;

import java.util.Stack;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
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
    public void recordMove(int[][] beforeState, int originalRow, int originalCol,
            BoxComponent selectedBox, int blockId, Direction direction) {
        // 检查 selectedBox 是否为 null
        if (selectedBox == null) {
            System.err.println("Error recording move: selectedBox is null");
            return;
        }
        // 记录移动操作 - 使用外部定义的 MoveRecord
        MoveRecord record = new MoveRecord(
                beforeState,
                model.copyMatrix(),
                originalRow,
                originalCol,
                selectedBox.getRow(),
                selectedBox.getCol(),
                blockId,
                direction
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

        // 更新视图
        updateViewAfterUndoRedo(record, true);

        // 更新步数 (确保 view 不为 null)
        if (view != null) {
            view.setSteps(view.getSteps() - 1);
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
        model.setMatrix(record.getAfterState());

        // 更新视图
        updateViewAfterUndoRedo(record, false);

        // 更新步数 (确保 view 不为 null)
        if (view != null) {
            view.setSteps(view.getSteps() + 1);
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
     * 在撤销/重做后更新视图
     */
    private void updateViewAfterUndoRedo(MoveRecord record, boolean isUndo) { // 使用外部定义的 MoveRecord
        // 添加 view 的 null 检查
        if (view == null) {
            System.err.println("Error updating view: GamePanel is null");
            return;
        }
        // 根据记录找到对应的方块
        BoxComponent targetBlock = findBlockByPosition(
                isUndo ? record.getNewRow() : record.getOriginalRow(),
                isUndo ? record.getNewCol() : record.getOriginalCol(),
                record.getBlockId()
        );

        if (targetBlock != null) {
            int targetRow, targetCol;

            if (isUndo) {
                // 撤销操作：恢复到原始位置
                targetRow = record.getOriginalRow();
                targetCol = record.getOriginalCol();
            } else {
                // 重做操作：恢复到移动后的位置
                targetRow = record.getNewRow();
                targetCol = record.getNewCol();
            }

            // 更新方块位置
            targetBlock.setRow(targetRow);
            targetBlock.setCol(targetCol);
            targetBlock.setLocation(targetCol * view.getGRID_SIZE() + 2,
                    targetRow * view.getGRID_SIZE() + 2);

            // 如果当前正好是选中的方块，更新选中状态
            BoxComponent selectedBox = view.getSelectedBox();
            if (selectedBox != null && selectedBox == targetBlock) {
                targetBlock.setSelected(true);
            } else if (selectedBox != null) {
                // 如果撤销/重做后，之前选中的方块不再是移动的方块，取消其选中状态
                // 或者根据需要，保持选中状态或选中移动后的方块
                // selectedBox.setSelected(false); // 取消选中之前的方块
                // view.setSelectedBox(targetBlock); // 选中移动后的方块
                // targetBlock.setSelected(true);
            }

            // 重绘方块
            targetBlock.repaint();
        } else {
            System.err.println("无法找到要撤销/重做的方块，位置: "
                    + (isUndo ? record.getNewRow() : record.getOriginalRow()) + ","
                    + (isUndo ? record.getNewCol() : record.getOriginalCol())
                    + " ID: " + record.getBlockId());
            // 尝试通过重新构建视图来恢复？（可能代价较高）
            // view.resetGame(); // 这会丢失当前状态，不是好方法
        }

        // 重绘整个面板
        view.repaint();
    }

    /**
     * 根据位置和方块类型ID查找方块组件
     */
    private BoxComponent findBlockByPosition(int row, int col, int blockId) {
        // 添加 view 的 null 检查
        if (view == null || view.getBoxes() == null) {
            System.err.println("Error finding block: GamePanel or boxes list is null");
            return null;
        }
        // 从GamePanel获取所有方块组件
        for (BoxComponent box : view.getBoxes()) {
            // 检查方块的左上角坐标是否匹配
            if (box.getRow() == row && box.getCol() == col) {
                // 进一步验证方块类型是否匹配 (可选，但更健壮)
                // 这需要 BoxComponent 存储其类型 ID，或者根据尺寸推断
                // 例如: if (matchesType(box, blockId)) return box;
                return box; // 简化：假设左上角坐标唯一标识一个方块
            }
        }
        // 如果找不到，可能是在撤销/重做过程中状态不一致
        System.err.println("Block not found at row=" + row + ", col=" + col + " with ID=" + blockId);
        return null;
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
