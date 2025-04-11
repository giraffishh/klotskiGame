package view.util;

import javax.swing.*;
import java.awt.*;

/**
 * 界面工具类，提供创建常用Swing组件的便捷方法。
 * 用于简化UI组件的创建和添加过程。
 */
public class FrameUtil {
    /**
     * 创建标准文本标签
     * @param frame 父窗口，标签将被添加到此窗口中
     * @param location 标签在窗口中的位置坐标
     * @param width 标签宽度
     * @param height 标签高度
     * @param text 标签显示的文本内容
     * @return 创建的JLabel实例
     */
    public static JLabel createJLabel(JFrame frame, Point location, int width, int height, String text) {
        JLabel jLabel = new JLabel(text);
        jLabel.setSize(width, height);
        jLabel.setLocation(location);
        frame.add(jLabel);
        return jLabel;
    }

    /**
     * 创建带有自定义字体的文本标签
     * @param frame 父窗口，标签将被添加到此窗口中
     * @param name 标签显示的文本内容
     * @param font 标签使用的字体
     * @param location 标签在窗口中的位置坐标
     * @param width 标签宽度
     * @param height 标签高度
     * @return 创建的JLabel实例
     */
    public static JLabel createJLabel(JFrame frame, String name, Font font, Point location, int width, int height) {
        JLabel label = new JLabel(name);
        label.setFont(font);
        label.setLocation(location);
        label.setSize(width, height);
        frame.add(label);
        return label;
    }

    /**
     * 创建文本输入框
     * @param frame 父窗口，输入框将被添加到此窗口中
     * @param location 输入框在窗口中的位置坐标
     * @param width 输入框宽度
     * @param height 输入框高度
     * @return 创建的JTextField实例
     */
    public static JTextField createJTextField(JFrame frame, Point location, int width, int height) {
        JTextField jTextField = new JTextField();
        jTextField.setSize(width, height);
        jTextField.setLocation(location);
        frame.add(jTextField);
        return jTextField;
    }

    /**
     * 创建按钮
     * @param frame 父窗口，按钮将被添加到此窗口中
     * @param name 按钮显示的文本
     * @param location 按钮在窗口中的位置坐标
     * @param width 按钮宽度
     * @param height 按钮高度
     * @return 创建的JButton实例
     */
    public static JButton createButton(JFrame frame, String name, Point location, int width, int height) {
        JButton button = new JButton(name);
        button.setLocation(location);
        button.setSize(width, height);
        frame.add(button);
        return button;
    }
}
