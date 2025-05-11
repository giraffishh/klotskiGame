package view.util;

import javax.swing.*;
import java.awt.*;

/**
 * 界面工具类，提供创建常用Swing组件的便捷方法。
 * 用于简化UI组件的创建和添加过程，支持FlatLaf美化界面。
 */
public class FrameUtil {
    // 主题基础颜色
    public static final Color PRIMARY_COLOR = new Color(84, 173, 255);    // 主色调-蓝色
    public static final Color SECONDARY_COLOR = new Color(240, 240, 240); // 次要色调-浅灰色
    public static final Color TEXT_COLOR = Color.DARK_GRAY;               // 文本颜色
    public static final Color ACCENT_COLOR = new Color(255, 153, 0);      // 强调色-橙色

    // 登录界面相关颜色
    public static final Color ERROR_COLOR = new Color(220, 53, 69);       // 错误提示-红色
    public static final Color LOGIN_COLOR = new Color(62, 137, 243);       // 登录按钮-绿色
    public static final Color REGISTER_COLOR = new Color(59, 151, 63);    // 注册按钮-橙色


    // 方块颜色
    public static final Color VERTICAL_BLOCK_COLOR = new Color(100, 149, 237);  // 垂直方块-蓝色
    public static final Color BIG_BLOCK_COLOR = new Color(102, 187, 106);       // 大方块-绿色
    public static final Color HORIZOTAL_BLOCK_COLOR = new Color(143, 143, 255);       // 大方块-绿色



    // 边框颜色
    public static final Color HOVER_BORDER_COLOR = new Color(255, 251, 0);     // 悬停边框-金色
    public static final Color SELECTED_BORDER_COLOR = new Color(255, 0, 0);     // 选中边框-红色
    public static final Color NORMAL_BORDER_COLOR = new Color(50, 50, 50, 150); // 普通边框-深灰色
    public static final Color HOVER_GLOW = new Color(255, 215, 0, 80);          // 悬停光晕-金色

    // 游戏面板背景颜色
    public static final Color PANEL_BACKGROUND_LIGHT = new Color(230, 228, 225); // 面板渐变浅色
    public static final Color PANEL_BACKGROUND_DARK = new Color(215, 213, 210);  // 面板渐变深色
    public static final Color GRID_LINE_COLOR = new Color(200, 198, 195);        // 网格线颜色
    public static final Color PANEL_BORDER_COLOR = new Color(120, 120, 130);     // 面板边框颜色

    // 添加高光效果颜色
    public static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 30);    // 高光效果-半透明白色

    /**
     * 初始化全局UI设置，包括对话框按钮文本和字体
     */
    public static void initUIDefaults() {
        // 设置对话框按钮为英文
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.okButtonText", "OK");


        // 设置对话框使用自定义字体
        UIManager.put("OptionPane.messageFont", FontManager.getRegularFont());
        UIManager.put("OptionPane.buttonFont", FontManager.getButtonFont(14));
        UIManager.put("OptionPane.font", FontManager.getRegularFont());
    }

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
    
    /**
     * 创建一个美化的标题标签
     * @param text 标签文本
     * @param alignment 对齐方式（如JLabel.CENTER）
     * @return 美化后的标题标签
     */
    public static JLabel createTitleLabel(String text, int alignment) {
        JLabel label = new JLabel(text, alignment);
        label.setFont(FontManager.getTitleFont());
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    /**
     * 创建一个美化的按钮
     * @param text 按钮文本
     * @param isPrimary 是否为主要按钮（使用主要颜色）
     * @return 美化后的按钮
     */
    public static JButton createStyledButton(String text, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getButtonFont());
        
        if (isPrimary) {
            button.setBackground(PRIMARY_COLOR);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(SECONDARY_COLOR);
            button.setForeground(TEXT_COLOR);
        }
        
        // 设置按钮首选大小
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 40));
        button.setFocusPainted(false);
        
        return button;
    }
    
    /**
     * 创建一个美化的方向控制按钮
     * @param direction 方向("↑", "↓", "←", "→")
     * @return 美化后的方向按钮
     */
    public static JButton createDirectionButton(String direction) {
        JButton button = new JButton(direction);
        // 使用更大更粗的字体显示箭头
        button.setFont(new Font("Dialog", Font.BOLD, 20));
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        
        // 为方向按钮添加悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(220, 220, 220)); // 悬停颜色
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(SECONDARY_COLOR); // 恢复原色
            }
        });
        
        return button;
    }
    
    /**
     * 创建一个美化的文本输入框
     * @param columns 输入框列数
     * @return 美化后的文本输入框
     */
    public static JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setFont(FontManager.getInputFont());
        textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, 30));
        return textField;
    }
    
    /**
     * 创建一个美化的密码输入框
     * @param columns 输入框列数
     * @return 美化后的密码输入框
     */
    public static JPasswordField createStyledPasswordField(int columns) {
        JPasswordField passwordField = new JPasswordField(columns);
        passwordField.setFont(FontManager.getInputFont());
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 30));
        return passwordField;
    }
    
    /**
     * 创建一个输入字段和标签的组合面板
     * @param labelText 标签文本
     * @param component 输入组件（如JTextField）
     * @return 包含标签和输入字段的面板
     */
    public static JPanel createInputPanel(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(10, 0)); // 使用BorderLayout，设置水平间距为10

        // 创建一个固定宽度的标签面板，确保所有标签右对齐
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setPreferredSize(new Dimension(138, 30)); // 确保较长文本完整显示

        JLabel label = new JLabel(labelText, JLabel.RIGHT);
        label.setFont(FontManager.getRegularFont());

        labelPanel.add(label, BorderLayout.CENTER);
        
        // 创建一个输入组件面板
        JPanel componentPanel = new JPanel(new BorderLayout());
        componentPanel.add(component, BorderLayout.CENTER);
        
        // 将两个面板添加到主面板
        panel.add(labelPanel, BorderLayout.WEST);
        panel.add(componentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建一个带边距的面板
     * @param layout 布局管理器
     * @param top 上边距
     * @param left 左边距
     * @param bottom 底边距
     * @param right 右边距
     * @return 带边距的面板
     */
    public static JPanel createPaddedPanel(LayoutManager layout, int top, int left, int bottom, int right) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return panel;
    }
    
    /**
     * 创建一个带有阴影边框的面板
     * @param layout 布局管理器
     * @return 带阴影边框的面板
     */
    public static JPanel createShadowPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 20), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }
    
    /**
     * 为组件设置边距
     * @param component 要设置边距的组件
     * @param top 上边距
     * @param left 左边距
     * @param bottom 底边距
     * @param right 右边距
     */
    public static void setPadding(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }
    
    /**
     * 创建按钮组面板
     * @param buttons 按钮数组
     * @param gap 按钮之间的间距
     * @return 包含按钮的面板
     */
    public static JPanel createButtonPanel(JButton[] buttons, int gap) {
        JPanel panel = new JPanel(new GridLayout(1, buttons.length, gap, 0));
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }
}

