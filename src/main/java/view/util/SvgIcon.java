package view.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.io.File;
import java.net.URL;
import java.awt.Color;

/**
 * 自定义SVG图标类，扩展FlatSVGIcon的功能
 * 提供额外的构造方法和实用功能
 */
public class SvgIcon extends FlatSVGIcon {

    /**
     * 从文件创建SVG图标
     * @param file SVG文件
     */
    public SvgIcon(File file) {
        super(file);
    }

    /**
     * 从URL创建自定义大小的SVG图标
     * @param url SVG资源URL
     * @param width 宽度
     * @param height 高度
     */
    public SvgIcon(URL url, int width, int height) {
        super(url);
        if (width > 0 && height > 0) {
            // 使用derive方法设置大小，而不是不存在的setWidth和setHeight方法
            this.derive(width, height);
        }
    }
    
    /**
     * 从资源路径创建SVG图标
     * @param resourceName 资源路径
     */
    public SvgIcon(String resourceName) {
        super(resourceName);
    }
    
    /**
     * 从资源路径创建指定大小的SVG图标
     * @param resourceName 资源路径
     * @param width 宽度
     * @param height 高度
     */
    public SvgIcon(String resourceName, int width, int height) {
        super(resourceName);
        if (width > 0 && height > 0) {
            // 使用derive方法设置大小
            this.derive(width, height);
        }
    }
    
    /**
     * 设置图标颜色
     * @param color 要应用的颜色
     * @return 设置了颜色的图标实例
     */
    public SvgIcon withColor(Color color) {
        if (color != null) {
            // 修正ColorFilter的引用
            this.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        }
        return this;
    }
}
