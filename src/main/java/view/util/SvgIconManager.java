package view.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * SVG图标管理器，用于加载、缓存和提供SVG图标
 * 基于FlatLaf的SVG图标实现，提供更高质量和更灵活的图标支持
 */
public class SvgIconManager {
    
    private static final Map<String, FlatSVGIcon> iconCache = new HashMap<>();
    private static final String SVG_BASE_PATH = "/images/icons/";
    private static final int BUTTON_ICON_SIZE = 20;
    private static final int DIRECTION_ICON_SIZE = 26;
    
    /**
     * 获取主页图标
     * @return 主页图标
     */
    public static Icon getHomeIcon() {
        return getSvgIcon("home.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取返回主页图标（专用于胜利界面）
     * @return 返回主页图标
     */
    public static Icon getBackToHomeIcon() {
        return getSvgIcon("back_to_home.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取新游戏图标
     * @return 新游戏图标
     */
    public static Icon getNewGameIcon() {
        return getSvgIcon("new_game.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取加载游戏图标
     * @return 加载游戏图标
     */
    public static Icon getLoadGameIcon() {
        return getSvgIcon("load_game.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取设置图标
     * @return 设置图标
     */
    public static Icon getSettingsIcon() {
        return getSvgIcon("settings.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取退出登录图标
     * @return 退出登录图标
     */
    public static Icon getLogoutIcon() {
        return getSvgIcon("logout.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取重启图标
     * @return 重启图标
     */
    public static Icon getRestartIcon() {
        return getSvgIcon("restart.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取保存图标
     * @return 保存图标
     */
    public static Icon getSaveIcon() {
        return getSvgIcon("save.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取撤销图标
     * @return 撤销图标
     */
    public static Icon getUndoIcon() {
        return getSvgIcon("undo.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取重做图标
     * @return 重做图标
     */
    public static Icon getRedoIcon() {
        return getSvgIcon("redo.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取登录图标
     * @return 登录图标
     */
    public static Icon getLoginIcon() {
        return getSvgIcon("login.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取访客图标
     * @return 访客图标
     */
    public static Icon getGuestIcon() {
        return getSvgIcon("guest.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取向上箭头图标（适用于方向按钮）
     * @return 向上箭头图标
     */
    public static Icon getUpArrowIcon() {
        return getSvgIcon("arrow_up.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取向下箭头图标（适用于方向按钮）
     * @return 向下箭头图标
     */
    public static Icon getDownArrowIcon() {
        return getSvgIcon("arrow_down.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取向左箭头图标（适用于方向按钮）
     * @return 向左箭头图标
     */
    public static Icon getLeftArrowIcon() {
        return getSvgIcon("arrow_left.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取向右箭头图标（适用于方向按钮）
     * @return 向右箭头图标
     */
    public static Icon getRightArrowIcon() {
        return getSvgIcon("arrow_right.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取返回图标
     * @return 返回图标
     */
    public static Icon getBackIcon() {
        return getSvgIcon("back.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取提示图标
     * @return 提示图标
     */
    public static Icon getHintIcon() {
        return getSvgIcon("hint.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 加载SVG并转换为Icon
     * @param svgFileName SVG文件名
     * @param size 图标尺寸
     * @return SVG图标
     */
    public static FlatSVGIcon getSvgIcon(String svgFileName, int size) {
        return getSvgIcon(svgFileName, size, null);
    }
    
    /**
     * 加载SVG并转换为带指定颜色的Icon
     * @param svgFileName SVG文件名
     * @param size 图标尺寸
     * @param color 图标颜色，null表示使用原始颜色
     * @return SVG图标
     */
    public static FlatSVGIcon getSvgIcon(String svgFileName, int size, Color color) {
        String cacheKey = svgFileName + "_" + size + "_" + (color != null ? color.getRGB() : "default");
        
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }
        
        try {
            String path = SVG_BASE_PATH + svgFileName;
            URL url = SvgIconManager.class.getResource(path);
            
            if (url == null) {
                System.err.println("SVG资源未找到: " + path);
                return createEmptySvgIcon(size);
            }
            
            FlatSVGIcon icon = new FlatSVGIcon(url);
            
            if (size > 0) {
                icon = icon.derive(size, size);
            }
            
            if (color != null) {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
            }
            
            iconCache.put(cacheKey, icon);
            
            return icon;
        } catch (Exception e) {
            System.err.println("SVG加载错误: " + e.getMessage());
            return createEmptySvgIcon(size);
        }
    }
    
    /**
     * 创建空的SVG图标，在加载失败时使用
     * @param size 图标尺寸
     * @return 空SVG图标
     */
    private static FlatSVGIcon createEmptySvgIcon(int size) {
        StringBuilder emptySvg = new StringBuilder();
        emptySvg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + size + "\" height=\"" + size + "\">");
        emptySvg.append("<rect width=\"" + size + "\" height=\"" + size + "\" fill=\"none\"/>");
        emptySvg.append("</svg>");
        
        FlatSVGIcon icon = new FlatSVGIcon(emptySvg.toString(), SvgIconManager.class.getClassLoader());
        return icon;
    }
}
