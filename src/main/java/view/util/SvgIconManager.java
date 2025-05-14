package view.util;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * SVG图标管理器，用于加载、缓存和提供SVG图标
 * 提供静态方法获取各种图标
 */
public class SvgIconManager {
    
    // 图标缓存，避免重复加载
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();
    
    // SVG资源基础路径 - 修改为images/icons目录
    private static final String SVG_BASE_PATH = "/images/icons/";
    
    // 按钮图标尺寸
    private static final int BUTTON_ICON_SIZE = 20;
    
    // 方向按钮图标尺寸
    private static final int DIRECTION_ICON_SIZE = 26;
    
    // 超采样因子 - 提高到4使线条更饱满
    private static final int SUPERSAMPLING_FACTOR = 4;
    
    /**
     * 获取主页图标
     * @return 主页图标
     */
    public static ImageIcon getHomeIcon() {
        return getSvgIcon("home.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取返回主页图标（专用于胜利界面）
     * @return 返回主页图标
     */
    public static ImageIcon getBackToHomeIcon() {
        return getSvgIcon("back_to_home.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取新游戏图标
     * @return 新游戏图标
     */
    public static ImageIcon getNewGameIcon() {
        return getSvgIcon("new_game.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取加载游戏图标
     * @return 加载游戏图标
     */
    public static ImageIcon getLoadGameIcon() {
        return getSvgIcon("load_game.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取设置图标
     * @return 设置图标
     */
    public static ImageIcon getSettingsIcon() {
        return getSvgIcon("settings.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取退出登录图标
     * @return 退出登录图标
     */
    public static ImageIcon getLogoutIcon() {
        return getSvgIcon("logout.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取重启图标
     * @return 重启图标
     */
    public static ImageIcon getRestartIcon() {
        return getSvgIcon("restart.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取保存图标
     * @return 保存图标
     */
    public static ImageIcon getSaveIcon() {
        return getSvgIcon("save.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取撤销图标
     * @return 撤销图标
     */
    public static ImageIcon getUndoIcon() {
        return getSvgIcon("undo.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取重做图标
     * @return 重做图标
     */
    public static ImageIcon getRedoIcon() {
        return getSvgIcon("redo.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取登录图标
     * @return 登录图标
     */
    public static ImageIcon getLoginIcon() {
        return getSvgIcon("login.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取访客图标
     * @return 访客图标
     */
    public static ImageIcon getGuestIcon() {
        return getSvgIcon("guest.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取向上箭头图标（适用于方向按钮）
     * @return 向上箭头图标
     */
    public static ImageIcon getUpArrowIcon() {
        return getSvgIcon("arrow_up.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取向下箭头图标（适用于方向按钮）
     * @return 向下箭头图标
     */
    public static ImageIcon getDownArrowIcon() {
        return getSvgIcon("arrow_down.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取向左箭头图标（适用于方向按钮）
     * @return 向左箭头图标
     */
    public static ImageIcon getLeftArrowIcon() {
        return getSvgIcon("arrow_left.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取向右箭头图标（适用于方向按钮）
     * @return 向右箭头图标
     */
    public static ImageIcon getRightArrowIcon() {
        return getSvgIcon("arrow_right.svg", DIRECTION_ICON_SIZE);
    }
    
    /**
     * 获取返回图标
     * @return 返回图标
     */
    public static ImageIcon getBackIcon() {
        return getSvgIcon("back.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 获取提示图标
     * @return 提示图标
     */
    public static ImageIcon getHintIcon() {
        return getSvgIcon("hint.svg", BUTTON_ICON_SIZE);
    }
    
    /**
     * 加载SVG并转换为ImageIcon
     * 使用超采样技术提高清晰度
     * @param svgFileName SVG文件名
     * @param size 图标尺寸
     * @return 转换后的ImageIcon，如果加载失败则返回空图标
     */
    public static ImageIcon getSvgIcon(String svgFileName, int size) {
        String cacheKey = svgFileName + "_" + size;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }
        
        try {
            String path = SVG_BASE_PATH + svgFileName;
            URL url = SvgIconManager.class.getResource(path);
            if (url == null) {
                System.err.println("SVG资源未找到: " + path);
                return createEmptyIcon(size);
            }
            
            // 使用更高精度的超采样技术
            int supersampledSize = size * SUPERSAMPLING_FACTOR;
            
            // 加载SVG文档
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument document = factory.createSVGDocument(url.toString());
            
            // 转换为更大的图像
            BufferedImageTranscoder transcoder = new EnhancedImageTranscoder();
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) supersampledSize);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) supersampledSize);
            transcoder.transcode(new TranscoderInput(document), null);
            
            // 获取超采样图像
            BufferedImage supersampledImage = transcoder.getBufferedImage();
            
            // 使用增强的缩放方法
            BufferedImage finalImage = enhancedScaling(supersampledImage, size);
            
            ImageIcon icon = new ImageIcon(finalImage);
            
            // 缓存图标
            iconCache.put(cacheKey, icon);
            
            return icon;
        } catch (IOException e) {
            System.err.println("SVG加载错误: " + e.getMessage());
            return createEmptyIcon(size);
        } catch (TranscoderException e) {
            System.err.println("SVG转换错误: " + e.getMessage());
            return createEmptyIcon(size);
        }
    }
    
    /**
     * 创建空图标，在SVG加载失败时使用
     * @param size 图标尺寸
     * @return 空图标
     */
    private static ImageIcon createEmptyIcon(int size) {
        BufferedImage emptyImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(emptyImage);
    }
    

    /**
     * 增强的图像缩放方法，使线条更饱满平滑
     */
    private static BufferedImage enhancedScaling(BufferedImage source, int targetSize) {
        // 创建目标图像
        BufferedImage target = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = target.createGraphics();
        
        // 应用高质量渲染提示
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 绘制图像，稍微扩大0.5像素以增加饱满度
        int padding = 1;
        g2d.drawImage(source, -padding, -padding, 
                      targetSize + padding*2, targetSize + padding*2, null);
        g2d.dispose();
        
        // 应用轻微模糊以使线条更加平滑
        return applyLightBlur(target);
    }
    
    /**
     * 应用轻微模糊，增强线条平滑度
     */
    private static BufferedImage applyLightBlur(BufferedImage image) {
        float[] blurKernel = {
            0.01f, 0.02f, 0.01f,
            0.02f, 0.88f, 0.02f,
            0.01f, 0.02f, 0.01f
        };
        
        java.awt.image.ConvolveOp blurOp = new java.awt.image.ConvolveOp(
            new java.awt.image.Kernel(3, 3, blurKernel),
            java.awt.image.ConvolveOp.EDGE_NO_OP, null);
            
        return blurOp.filter(image, null);
    }
    
    /**
     * 增强版的图像转码器，提供更精细的渲染控制
     */
    private static class EnhancedImageTranscoder extends BufferedImageTranscoder {
        @Override
        public BufferedImage createImage(int width, int height) {
            // 使用预乘Alpha的ARGB格式，提高渲染质量
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        }
    }
    
    /**
     * 用于SVG转BufferedImage的转码器
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage bufferedImage;
        
        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        
        @Override
        public void writeImage(BufferedImage image, TranscoderOutput output) {
            this.bufferedImage = image;
        }
        
        public BufferedImage getBufferedImage() {
            return bufferedImage;
        }
    }
}
