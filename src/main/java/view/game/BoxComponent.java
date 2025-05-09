package view.game;

import view.util.FrameUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

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
    // 鼠标悬停效果
    private boolean isHovered;
    // 方块的图片
    private Image image;
    // 方块类型：1(1x1), 2(2x1水平), 3(1x2垂直), 4(2x2曹操)
    private int blockType;

    /**
     * 创建一个游戏方块组件
     * 
     * @param color 方块的颜色
     * @param row 方块所在的行
     * @param col 方块所在的列
     * @param blockType 方块类型
     */
    public BoxComponent(Color color, int row, int col, int blockType) {
        this.color = color;
        this.row = row;
        this.col = col;
        this.blockType = blockType;
        isSelected = false;
        isHovered = false;

        // 添加鼠标事件监听器实现悬停效果
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            // 添加鼠标点击事件处理，将点击事件传递给父容器处理
            @Override
            public void mousePressed(MouseEvent e) {
                // 获取事件的全局坐标
                Point globalPoint = e.getPoint();
                SwingUtilities.convertPointToScreen(globalPoint, BoxComponent.this);

                // 将全局坐标转换为父容器坐标
                Container parent = getParent();
                if (parent != null) {
                    Point parentPoint = new Point(globalPoint);
                    SwingUtilities.convertPointFromScreen(parentPoint, parent);

                    // 创建新的鼠标事件并发送给父容器
                    MouseEvent parentEvent = new MouseEvent(
                        parent,
                        e.getID(),
                        e.getWhen(),
                        e.getModifiersEx(),
                        parentPoint.x,
                        parentPoint.y,
                        e.getClickCount(),
                        e.isPopupTrigger()
                    );
                    parent.dispatchEvent(parentEvent);
                }
            }
        });

        // 确保组件可获得焦点和接收鼠标事件
        this.setFocusable(true);
        this.setEnabled(true);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 设置鼠标指针为手形，增强可点击感
    }

    /**
     * 创建一个游戏方块组件（兼容旧版构造器）
     * 
     * @param color 方块的颜色
     * @param row 方块所在的行
     * @param col 方块所在的列
     */
    public BoxComponent(Color color, int row, int col) {
        this(color, row, col, 0);
    }

    /**
     * 设置方块的图片。
     *
     * @param image 图片对象
     */
    public void setImage(Image image) {
        this.image = image;
        this.repaint();
    }

    /**
     * 重写绘制方法，绘制方块及其边框
     * 选中状态下显示红色边框，未选中状态显示细边框
     * 
     * @param g Graphics对象，用于绘制组件
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 创建圆角矩形区域
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arcSize = 12; // 圆角大小
        int margin = isSelected ? 3 : (isHovered ? 2 : 1); // 根据状态调整边距

        // 填充方块颜色，如果悬停在上面则略微变亮，选中则增加饱和度
        Color fillColor;
        if (isSelected) {
            // 选中状态：增加饱和度和亮度
            fillColor = saturateColor(color, 1.2f, 1.1f);
        } else if (isHovered) {
            // 悬停状态：增加亮度
            fillColor = brightenColor(color, 0.15f);
        } else {
            // 普通状态：稍微提高原始颜色的饱和度
            fillColor = saturateColor(color, 1.1f, 1.05f);
        }
        
        g2d.setColor(fillColor);
        g2d.fillRoundRect(margin, margin, getWidth() - margin*2, getHeight() - margin*2, arcSize, arcSize);
        
        // 绘制图片（如果存在）
        if (image != null) {
            // 保存原始裁剪区域
            Shape oldClip = g2d.getClip();
            Composite oldComposite = g2d.getComposite();
            
            // 设置裁剪区域为方块的圆角矩形，确保图片不会超出方块边界
            g2d.clip(new java.awt.geom.RoundRectangle2D.Float(
                margin, margin, 
                getWidth() - margin*2, 
                getHeight() - margin*2, 
                arcSize, arcSize
            ));
            
            // 绘制图片，使用与方块相同的尺寸和位置
            g.drawImage(image, margin, margin, getWidth() - margin*2, getHeight() - margin*2, this);
            
            // 根据悬停状态或选中状态添加半透明叠加层
            if (isHovered || isSelected) {
                // 创建悬停或选中效果的半透明叠加层
                if (isHovered) {
                    // 悬停状态：添加白色半透明叠加层，使图片变亮
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                    g2d.setColor(Color.WHITE);
                    g2d.fillRoundRect(margin, margin, getWidth() - margin*2, getHeight() - margin*2, arcSize, arcSize);
                } else if (isSelected) {
                    // 选中状态：添加更强烈的白色半透明叠加层
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2d.setColor(new Color(255, 255, 255));
                    g2d.fillRoundRect(margin, margin, getWidth() - margin*2, getHeight() - margin*2, arcSize, arcSize);
                }
            }
            
            // 恢复原始设置
            g2d.setClip(oldClip);
            g2d.setComposite(oldComposite);
        }
        
        // 简化边框绘制逻辑，避免多重边框叠加
        if (isSelected) {
            // 选中状态
            g2d.setColor(FrameUtil.SELECTED_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, arcSize, arcSize);
        } else if (isHovered) {
            // 悬停状态：金色边框
            g2d.setColor(FrameUtil.HOVER_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(3.0f));
            g2d.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, arcSize, arcSize);
            
            // 添加轻微的外发光效果
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2d.setColor(FrameUtil.HOVER_GLOW);
            g2d.setStroke(new BasicStroke(4.5f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arcSize+1, arcSize+1);
        } else {
            // 普通状态：深灰色边框
            g2d.setColor(FrameUtil.NORMAL_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arcSize, arcSize);

            // 添加微妙的高光效果，增强立体感
            g2d.setColor(FrameUtil.HIGHLIGHT_COLOR);
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawLine(4, 4, getWidth() - 5, 4);
            g2d.drawLine(4, 4, 4, getHeight() - 5);
        }
    }

    // 保留方法定义以维持代码结构，但不实现任何标识绘制
    private void drawBlockTypeIndicator(Graphics2D g2d) {
        // 无图片模式下不需要绘制任何标识
    }

    /**
     * 使颜色略微变亮，用于鼠标悬停效果
     * @param color 原始颜色
     * @param amount 变亮的程度
     * @return 增亮后的颜色
     */
    private Color brightenColor(Color color, float amount) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.min(1.0f, hsb[2] + amount));
    }
    
    /**
     * 调整颜色的饱和度和亮度
     * @param color 原始颜色
     * @param saturationFactor 饱和度调整因子
     * @param brightnessFactor 亮度调整因子
     * @return 调整后的颜色
     */
    private Color saturateColor(Color color, float saturationFactor, float brightnessFactor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float newSaturation = Math.min(1.0f, hsb[1] * saturationFactor);
        float newBrightness = Math.min(1.0f, hsb[2] * brightnessFactor);
        return Color.getHSBColor(hsb[0], newSaturation, newBrightness);
    }

    /**
     * 设置方块的选中状态并重绘
     * 
     * @param selected 是否选中
     */
    public void setSelected(boolean selected) {
        isSelected = selected;
        if (selected) {
            isHovered = false; // 选中时取消悬停效果
        }
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

    /**
     * 获取方块类型
     * @return 方块类型
     */
    public int getBlockType() {
        return blockType;
    }
    
    /**
     * 设置方块类型
     * @param blockType 方块类型
     */
    public void setBlockType(int blockType) {
        this.blockType = blockType;
        repaint();
    }
}

