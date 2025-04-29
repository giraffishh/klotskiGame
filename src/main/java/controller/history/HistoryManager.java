package controller.history;

import java.util.Stack;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GameFrame;
import view.game.GamePanel;

/**
 * 历史记录管理器 负责处理游戏中的撤销/重做操作
 */
public class HistoryManager {

    private final Stack<MoveRecord> undoStack; // 撤销栈，存储已执行的移动
    private final Stack<MoveRecord> redoStack; // 重做栈，存储被撤销的移动

    private final GamePanel view;
    private final MapModel model;
    private GameFrame parentFrame;

    public HistoryManager(GamePanel view, MapModel model) {
        this.view = view;
        this.model = model;
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
    }

    /**
     * 设置父窗口引用
     *
     * @param frame 游戏窗口
     */
    public void setParentFrame(GameFrame frame) {
        this.parentFrame = frame;
        // 添加异常处理和空指针检查
        if (frame != null) {
            try {
                updateUndoRedoButtons();
            } catch (Exception e) {
                // 捕获可能的空指针异常等
                System.err.println("Error updating buttons in HistoryManager: " + e.getMessage());
            }
        }
    }

    /**
     * 记录移动操作
     */
    public void recordMove(int[][] beforeState, int originalRow, int originalCol,
            BoxComponent selectedBox, int blockId, Direction direction) {
        // 记录移动操作
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

        // 弹出最近的移动记录
        MoveRecord record = undoStack.pop();

        // 保存到重做栈
        redoStack.push(record);

        // 恢复到移动前的状态
        model.setMatrix(record.getBeforeState());

        // 更新视图
        updateViewAfterUndoRedo(record, true);

        // 更新步数
        view.setSteps(view.getSteps() - 1);

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

        // 弹出最近撤销的移动记录
        MoveRecord record = redoStack.pop();

        // 保存到撤销栈
        undoStack.push(record);

        // 应用移动后的状态
        model.setMatrix(record.getAfterState());

        // 更新视图
        updateViewAfterUndoRedo(record, false);

        // 更新步数
        view.setSteps(view.getSteps() + 1);

        // 更新按钮状态
        updateUndoRedoButtons();

        System.out.println("Move redone successfully");
        return true;
    }

    /**
     * 清空移动历史
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        updateUndoRedoButtons();
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
    private void updateViewAfterUndoRedo(MoveRecord record, boolean isUndo) {
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
            }

            // 重绘方块
            targetBlock.repaint();
        } else {
            System.err.println("无法找到要撤销/重做的方块，位置: "
                    + (isUndo ? record.getNewRow() : record.getOriginalRow()) + ","
                    + (isUndo ? record.getNewCol() : record.getOriginalCol())
                    + " ID: " + record.getBlockId());
        }

        // 重绘整个面板
        view.repaint();
    }

    /**
     * 根据位置和方块类型ID查找方块组件
     */
    private BoxComponent findBlockByPosition(int row, int col, int blockId) {
        // 从GamePanel获取所有方块组件
        for (BoxComponent box : view.getBoxes()) {
            if (box.getRow() == row && box.getCol() == col) {
                // 对于2x2大方块和2x1水平方块，只检查左上角位置
                if ((blockId == 4 || blockId == 2) && box.getWidth() > view.getGRID_SIZE()) {
                    return box;
                } // 对于1x2垂直方块，只检查左上角位置
                else if (blockId == 3 && box.getHeight() > view.getGRID_SIZE()) {
                    return box;
                } // 对于1x1小方块，检查精确位置
                else if (blockId == 1
                        && box.getWidth() == view.getGRID_SIZE()
                        && box.getHeight() == view.getGRID_SIZE()) {
                    return box;
                }
            }
        }
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
