package view.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/**
 * 字体管理工具类
 * 负责加载和管理应用程序中使用的字体资源
 */
public class FontManager {
    // 默认标题字体
    private static Font titleFont;
    // 默认正文字体
    private static Font regularFont;
    // 默认按钮字体
    private static Font buttonFont;
    // 通用输入字体
    private static Font inputFont;

    // 字体路径常量
    private static final String FONT_REGULAR_PATH = "/fonts/comic.ttf";
    private static final String FONT_BOLD_PATH = "/fonts/comicbd.ttf";
    private static final String FONT_CHINESE_PATH = "/fonts/msyh.ttc";

    static {
        // 静态初始化块，在类首次加载时执行
        initFonts();
    }
    
    /**
     * 初始化应用程序字体
     */
    private static void initFonts() {
        try {
            // 加载粗体字体用于标题
            titleFont = loadFont(FONT_BOLD_PATH, Font.BOLD, 22);
            
            // 加载常规字体用于正文
            regularFont = loadFont(FONT_REGULAR_PATH, Font.PLAIN, 16);
            
            // 加载粗体字体用于按钮
            buttonFont = loadFont(FONT_BOLD_PATH, Font.BOLD, 16);
            
            // 加载支持中文的字体用于输入框
            inputFont = loadFont(FONT_CHINESE_PATH, Font.PLAIN, 16);

        } catch (Exception e) {
            System.err.println("无法加载自定义字体，将使用系统默认字体");
            e.printStackTrace();
            
            // 使用系统默认字体作为备选
            titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 22);
            regularFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
            buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, 16);
            inputFont = new Font("宋体", Font.PLAIN, 16);
        }
    }
    
    /**
     * 从资源文件加载字体
     * 
     * @param path 字体资源路径
     * @param style 字体样式
     * @param size 字体大小
     * @return 加载的字体对象
     * @throws IOException 如果字体文件无法读取
     * @throws FontFormatException 如果字体文件格式错误
     */
    private static Font loadFont(String path, int style, float size) throws IOException, FontFormatException {
        InputStream is = FontManager.class.getResourceAsStream(path);
        if (is == null) {
            throw new IOException("找不到字体文件: " + path);
        }
        
        try {
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            Font font = baseFont.deriveFont(style, size);
            
            // 注册字体到图形环境（可选，但有些UI组件可能需要）
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(baseFont);
            
            return font;
        } finally {
            is.close();
        }
    }
    
    /**
     * 获取标题字体
     * @return 标题字体
     */
    public static Font getTitleFont() {
        return titleFont;
    }
    
    /**
     * 获取正文字体
     * @return 正文字体
     */
    public static Font getRegularFont() {
        return regularFont;
    }
    
    /**
     * 获取按钮字体
     * @return 按钮字体
     */
    public static Font getButtonFont() {
        return buttonFont;
    }
    
    /**
     * 获取输入框字体（支持中文）
     * @return 输入框字体
     */
    public static Font getInputFont() {
        return inputFont;
    }

    /**
     * 基于正文字体创建自定义大小的字体
     * @param size 字体大小
     * @return 调整大小后的字体
     */
    public static Font getRegularFont(float size) {
        return regularFont.deriveFont(size);
    }
    
    /**
     * 基于标题字体创建自定义大小的字体
     * @param size 字体大小
     * @return 调整大小后的字体
     */
    public static Font getTitleFont(float size) {
        return titleFont.deriveFont(size);
    }

    /**
     * 基于按钮字体创建自定义大小的字体
     * @param size 字体大小
     * @return 调整大小后的字体
     */
    public static Font getButtonFont(float size) {
        return buttonFont.deriveFont(size);
    }

    /**
     * 基于输入字体创建自定义大小的字体
     * @param size 字体大小
     * @return 调整大小后的字体
     */
    public static Font getInputFont(float size) {
        return inputFont.deriveFont(size);
    }
}
