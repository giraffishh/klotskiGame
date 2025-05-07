package view.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片管理器类，用于加载和缓存图片资源。
 */
public class ImageManager {
    private static final Map<String, Image> imageCache = new HashMap<>();

    /**
     * 获取指定名称的图片。
     *
     * @param imageName 图片文件名
     * @return 加载的图片对象
     */
    public static Image getImage(String imageName) {
        if (imageCache.containsKey(imageName)) {
            return imageCache.get(imageName);
        }

        try {
            Image image = ImageIO.read(ImageManager.class.getResource("/images/" + imageName));
            imageCache.put(imageName, image);
            return image;
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Image getCaoCaoImage() {
        return getImage("caocao.jpg");
    }

    public static Image getGuanYuImage() {
        return getImage("guanyu.jpg");
    }

    public static Image getZhangFeiImage() {
        return getImage("zhangfei.jpg");
    }

    public static Image getHuangZhongImage() {
        return getImage("huangzhong.jpg");
    }

    public static Image getMaChaoImage() {
        return getImage("machao.jpg");
    }

    public static Image getZhaoYunImage() {
        return getImage("zhaoyun.jpg");
    }

    public static Image getSoldierImage() {
        return getImage("soldier.jpg");
    }
}
