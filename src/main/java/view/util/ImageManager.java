package view.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import model.AppSettings;

/**
 * 图片管理器类，用于加载和缓存图片资源。
 */
public class ImageManager {
    private static final Map<String, Image> imageCache = new HashMap<>();
    
    // 图片路径常量
    private static final String SKINS_BASE_PATH = "/images/skins/";
    private static final String CLASSIC_PATH = SKINS_BASE_PATH + "classic/";
    private static final String CARTOON_PATH = SKINS_BASE_PATH + "cartoon/";
    
    // 角色图片常量
    private static final String[][] CHARACTER_IMAGES = {
        {"caocao.jpg", "cartoon_caocao.jpg"},     // 曹操 (2x2)
        {"guanyu.jpg", "cartoon_guanyu.jpg"},     // 关羽 (2x1水平)
        {"huangzhong.jpg", "cartoon_huangzhong.jpg"}, // 黄忠 (1x2垂直)
        {"soldier.jpg", "cartoon_soldier.jpg"}    // 士兵 (1x1)
    };
    
    // 创建一个Random实例用于生成随机数
    private static final Random RANDOM = new Random();
    
    /**
     * 重置图片缓存，用于切换主题时刷新资源
     */
    public static void resetImageCache() {
        imageCache.clear();
    }

    /**
     * 获取指定名称的图片。
     *
     * @param imageName 图片文件名
     * @return 加载的图片对象
     */
    public static Image getImage(String imagePath) {
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath);
        }

        try {
            Image image = ImageIO.read(ImageManager.class.getResource(imagePath));
            imageCache.put(imagePath, image);
            return image;
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取当前方块主题
     * @return 当前方块主题名称
     */
    private static String getCurrentBlockTheme() {
        return AppSettings.getInstance().getCurrentBlockTheme();
    }
    
    /**
     * 判断当前是否使用无图片模式
     * @return 是否为无图片模式
     */
    private static boolean isNoImageMode() {
        return "NoImage".equals(getCurrentBlockTheme());
    }
    
    /**
     * 根据当前主题获取图片路径
     * @param classicName 经典主题图片文件名
     * @param cartoonName 卡通主题图片文件名
     * @return 当前主题对应的完整图片路径，无图片模式返回null
     */
    private static String getThemedImagePath(String classicName, String cartoonName) {
        // 无图片模式返回null
        if (isNoImageMode()) {
            return null;
        }
        
        return "Cartoon".equals(getCurrentBlockTheme()) 
               ? CARTOON_PATH + cartoonName 
               : CLASSIC_PATH + classicName;
    }

    public static Image getCaoCaoImage() {
        String path = getThemedImagePath(CHARACTER_IMAGES[0][0], CHARACTER_IMAGES[0][1]);
        return path == null ? null : getImage(path);
    }

    public static Image getGuanYuImage() {
        String path = getThemedImagePath(CHARACTER_IMAGES[1][0], CHARACTER_IMAGES[1][1]);
        return path == null ? null : getImage(path);
    }

    public static Image getHuangZhongImage() {
        String path = getThemedImagePath(CHARACTER_IMAGES[2][0], CHARACTER_IMAGES[2][1]);
        return path == null ? null : getImage(path);
    }

    public static Image getSoldierImage() {
        String path = getThemedImagePath(CHARACTER_IMAGES[3][0], CHARACTER_IMAGES[3][1]);
        return path == null ? null : getImage(path);
    }
    
    /**
     * 获取水平方块人物图像(2x1)
     * 固定使用关羽图像
     * @return 关羽图像
     */
    public static Image getHorizontalBlockImage() {
        return getGuanYuImage();
    }
    
    /**
     * 获取垂直方块人物图像(1x2)
     * 固定使用黄忠图像
     * @return 黄忠图像
     */
    public static Image getVerticalBlockImage() {
        return getHuangZhongImage();
    }
}
