package view.game;

import view.util.FrameUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 游戏方块组件类
 * 表示游戏中的一个彩色方块，可被选中并显示不同边框
 * 继承自JComponent以便自定义绘制
 */
public class BoxComponent extends JComponent {
    // 方块颜色
    private Color color;
    // 方块在网格中的行位置
    private int row;
    // 方块在网格中的列位置
    private int col;
    // 方块是否被选中的标志
    private boolean isSelected;

    /**
     * 创建一个游戏方块组件
     * 
     * @param color 方块的颜色
     * @param row 方块所在的行
     * @param col 方块所在的列
     */
    public BoxComponent(Color color, int row, int col) {
        this.color = color;
        this.row = row;
        this.col = col;
        isSelected = false;
    }

    /**
     * 重写绘制方法，绘制方块及其边框
     * 选中状态下显示红色粗边框，未选中状态显示细边框
     * 
     * @param g Graphics对象，用于绘制组件
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 创建圆角矩形区域
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充方块颜色
        g2d.setColor(color);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        
        // 根据选中状态设置不同的边框
        if(isSelected){
            // 选中状态：红色粗边框
            g2d.setColor(FrameUtil.ACCENT_COLOR.darker());
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
        } else {
            // 未选中状态：深灰色细边框
            g2d.setColor(FrameUtil.TEXT_COLOR);
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
        }
    }

    /**
     * 设置方块的选中状态并重绘
     * 
     * @param selected 是否选中
     */
    public void setSelected(boolean selected) {
        isSelected = selected;
        this.repaint();
    }

    /**
     * 获取方块的行位置
     * 
     * @return 行索引
     */
    public int getRow() {
        return row;
    }

    /**
     * 设置方块的行位置
     * 
     * @param row 行索引
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * 获取方块的列位置
     * 
     * @return 列索引
     */
    public int getCol() {
        return col;
    }

    /**
     * 设置方块的列位置
     * 
     * @param col 列索引
     */
    public void setCol(int col) {
        this.col = col;
    }
}
