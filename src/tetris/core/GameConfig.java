package tetris.core;

/**
 * 游戏配置常量
 */
public class GameConfig {
    // 游戏区域配置
    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 30;
    public static final int SIDEBAR_WIDTH = 120;
    
    // 游戏机制配置
    public static final int BASE_SPEED = 800; // 基础下落速度（毫秒）
    public static final double SPEED_FACTOR = 1.5; // 速度增加的指数系数
    public static final double SPEED_MULTIPLIER = 50.0; // 速度增加的乘数
    
    // 积分系统配置
    public static final int BASE_SCORE = 100; // 基础分数
    public static final double COMBO_MULTIPLIER = 0.2; // 连击加成系数
    public static final int LINES_PER_LEVEL = 10; // 每消除10行升一级
    
    /**
     * 根据关卡计算下落速度
     */
    public static int calculateSpeed(int level) {
        return (int) (BASE_SPEED - Math.pow(level, SPEED_FACTOR) * SPEED_MULTIPLIER);
    }
    
    /**
     * 根据消除行数和连击次数计算分数
     */
    public static int calculateScore(int lines, int combo, int level) {
        if (lines <= 0) return 0;
        
        // 基础分数基于消除行数（单行=100，双行=200，三行=400，四行=800）
        int baseScore = BASE_SCORE * (int)Math.pow(2, lines-1);
        
        // 连击加成
        double comboMultiplier = 1.0 + (combo * COMBO_MULTIPLIER);
        
        // 关卡系数
        double levelMultiplier = 1.0 + (level * 0.5);
        
        return (int)(baseScore * comboMultiplier * levelMultiplier);
    }
}
