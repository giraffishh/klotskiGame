package view.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * 该类用于启用键盘和鼠标事件监听功能。
 * 作为游戏面板的基类，提供了键盘方向键和鼠标点击的基本事件处理。
 * 子类需要实现具体的移动和点击行为。
 */
public abstract class ListenerPanel extends JPanel {
    /**
     * 构造函数，启用键盘和鼠标事件监听
     */
    public ListenerPanel() {
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.setFocusable(true);
    }

    /**
     * 处理键盘事件
     * 当方向键被按下时，调用相应的移动方法
     * @param e 键盘事件对象
     */
    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_RIGHT -> doMoveRight();
                case KeyEvent.VK_LEFT -> doMoveLeft();
                case KeyEvent.VK_UP -> doMoveUp();
                case KeyEvent.VK_DOWN -> doMoveDown();
            }
        }
    }
    
    /**
     * 处理鼠标事件
     * 当鼠标被点击时，调用doMouseClick方法
     * @param e 鼠标事件对象
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            doMouseClick(e.getPoint());
        }
    }
    
    /**
     * 处理鼠标点击事件的抽象方法
     * @param point 点击的坐标位置
     */
    public abstract void doMouseClick(Point point);

    /**
     * 处理向右移动的抽象方法
     */
    public abstract void doMoveRight();

    /**
     * 处理向左移动的抽象方法
     */
    public abstract void doMoveLeft();

    /**
     * 处理向上移动的抽象方法
     */
    public abstract void doMoveUp();

    /**
     * 处理向下移动的抽象方法
     */
    public abstract void doMoveDown();
}
