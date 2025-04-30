package view.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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

        // 替换原来的鼠标事件处理为MouseListener实现
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                doMouseClick(e.getPoint());
            }
        });

        this.setFocusable(true);
    }

    /**
     * 处理键盘事件
     * 当方向键被按下时，调用相应的移动方法
     * 支持Ctrl+Z撤销和Ctrl+Y/Ctrl+Shift+Z重做
     * 支持Ctrl+Shift+V触发胜利条件
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
                case KeyEvent.VK_Z -> {
                    // Ctrl+Z: 撤销
                    if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 &&
                        (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
                        doUndo();
                    }
                    // Ctrl+Shift+Z: 重做
                    else if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 &&
                             (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                        doRedo();
                    }
                }
                case KeyEvent.VK_Y -> {
                    // Ctrl+Y: 重做
                    if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        doRedo();
                    }
                }
                case KeyEvent.VK_V -> {
                    // Ctrl+Shift+V: 触发胜利条件
                    if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 &&
                        (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                        doVictoryShortcut();
                        System.out.println("Victory shortcut key combination pressed: Ctrl+Shift+V");
                    }
                }
            }
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

    /**
     * 处理撤销操作的抽象方法
     */
    public abstract void doUndo();

    /**
     * 处理重做操作的抽象方法
     */
    public abstract void doRedo();

    /**
     * 处理胜利快捷键
     * 触发直接胜利的功能
     */
    public abstract void doVictoryShortcut();
}
