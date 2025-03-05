package tetris.util;

import java.awt.Color;

public class ColorUtils {
    /**
     * 将HSV颜色转换为RGB颜色
     * @param h 色相 (0-360)
     * @param s 饱和度 (0.0-1.0)
     * @param v 亮度 (0.0-1.0)
     * @return RGB颜色
     */
    public static Color fromHSV(float h, float s, float v) {
        // 确保h在0-360范围内
        h = h % 360;
        if (h < 0) h += 360;
        
        // 将H转换为对应的RGB分量
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = v - c;
        
        float r, g, b;
        
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        return new Color(r + m, g + m, b + m);
    }
    
    /**
     * 生成7种互补色系的颜色，用于方块
     * @return 7种颜色数组
     */
    public static Color[] generateTetrominoColors() {
        Color[] colors = new Color[7];
        float saturation = 0.8f;
        float value = 0.7f;
        
        // 以约51度间隔生成7种不同色相的颜色
        for (int i = 0; i < 7; i++) {
            float hue = i * (360f / 7);
            colors[i] = fromHSV(hue, saturation, value);
        }
        
        return colors;
    }
}
