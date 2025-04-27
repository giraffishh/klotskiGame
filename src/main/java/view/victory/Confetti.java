package view.victory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Random;

/**
 * 表示单个彩色纸片的类
 */
public class Confetti {
    private double x;
    double y;
    private final int size;
    private Color color;
    private double speed;
    private double rotation = 0;
    private double rotationSpeed;

    public Confetti(double x, double y, int size, Color color, double speed, Random random) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.speed = speed;
        this.rotationSpeed = Math.toRadians(15 + random.nextDouble() * 15);
        this.rotation = random.nextDouble() * Math.PI * 2;
    }

    /**
     * 重置纸片属性以重用对象
     */
    public void reset(double x, double y, Color color, double speed, Random random) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.speed = speed;
        this.rotation = random.nextDouble() * Math.PI * 2;
        this.rotationSpeed = Math.toRadians(15 + random.nextDouble() * 15);
    }

    /**
     * 更新纸片的位置和旋转角度，基于实际经过的时间 (单位：秒)
     */
    public void update(double deltaTimeSeconds) {
        y += speed * deltaTimeSeconds;
        rotation += rotationSpeed * deltaTimeSeconds;
    }

    /**
     * 绘制纸片，直接使用浮点坐标
     */
    public void draw(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();

        g2d.translate((int) x, (int) y);
        g2d.rotate(rotation);

        g2d.setColor(color);
        g2d.fillRect(-size / 2, -size / 2, size, size);

        g2d.setTransform(oldTransform);
    }
}
