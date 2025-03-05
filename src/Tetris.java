import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class Tetris {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        final JFrame frame = new JFrame("俄罗斯方块");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        JPanel mainPanel = new JPanel(new CardLayout());
        StartPanel startPanel = new StartPanel(frame, mainPanel);
        GamePanel gamePanel = new GamePanel(frame, mainPanel);
        HighScorePanel highScorePanel = new HighScorePanel(frame, mainPanel);

        mainPanel.add(startPanel, "Start");
        mainPanel.add(gamePanel, "Game");
        mainPanel.add(highScorePanel, "HighScore");

        JMenuBar menuBar = new MenuBar(frame, gamePanel, mainPanel);
        frame.setJMenuBar(menuBar);

        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

/**
 * 游戏配置常量
 */
class GameConfig {
    // 游戏区域配置
    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 30;
    public static final int SIDEBAR_WIDTH = 120;
    
    // 游戏机制配置
    public static final int BASE_SPEED = 800; // 基础下落速度（毫秒）
    public static final double SPEED_FACTOR = 1.2; // 速度增加的指数系数 - 降低，使难度增加更平缓
    public static final double SPEED_MULTIPLIER = 40.0; // 速度增加的乘数 - 降低
    public static final boolean USE_COUNTDOWN_TIMER = false; // 修改：禁用倒计时，改为无限制模式
    public static final int BASE_COUNTDOWN = 120; // 修改：每关基础倒计时增加到120秒
    public static final int COUNTDOWN_DECREASE = 0; // 修改：关卡提升不减少时间
    
    // 积分系统配置
    public static final int BASE_SCORE = 100; // 基础分数
    public static final double COMBO_MULTIPLIER = 0.2; // 连击加成系数
    public static final int LINES_PER_LEVEL = 10; // 每消除10行升一级
    
    /**
     * 根据关卡计算下落速度
     */
    public static int calculateSpeed(int level) {
        return (int) Math.max(100, BASE_SPEED - Math.pow(level, SPEED_FACTOR) * SPEED_MULTIPLIER);
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

/**
 * 颜色工具类
 */
class ColorUtils {
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

// 开始界面面板
class StartPanel extends JPanel {
    public StartPanel(final JFrame frame, final JPanel mainPanel) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("俄罗斯方块", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(title, gbc);

        JButton startButton = new JButton("开始游戏");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "Game");
                GamePanel gamePanel = (GamePanel) mainPanel.getComponent(1);
                gamePanel.startGame();
            }
        });
        gbc.gridy = 1;
        add(startButton, gbc);

        JButton levelsButton = new JButton("选择关卡");
        levelsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String[] options = {"简单", "中等", "困难"};
                int choice = JOptionPane.showOptionDialog(frame, "选择难度", "选择关卡",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                GamePanel gamePanel = (GamePanel) mainPanel.getComponent(1);
                gamePanel.setLevel(choice + 1);
            }
        });
        gbc.gridy = 2;
        add(levelsButton, gbc);

        JButton highScoreButton = new JButton("查看高分");
        highScoreButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "HighScore");
                HighScorePanel highScorePanel = (HighScorePanel) mainPanel.getComponent(2);
                highScorePanel.updateScores();
            }
        });
        gbc.gridy = 3;
        add(highScoreButton, gbc);

        JButton exitButton = new JButton("退出");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        gbc.gridy = 4;
        add(exitButton, gbc);
    }
}

// 游戏面板
class GamePanel extends JPanel {
    // 使用GameConfig中的常量
    private static final int BOARD_WIDTH = GameConfig.BOARD_WIDTH;
    private static final int BOARD_HEIGHT = GameConfig.BOARD_HEIGHT;
    private static final int CELL_SIZE = GameConfig.CELL_SIZE;
    private static final int SIDEBAR_WIDTH = GameConfig.SIDEBAR_WIDTH;

    private int[][] board;
    private Tetromino currentTetromino;
    private boolean isPaused;
    private int score;
    private int level;
    private int linesCleared;  // 新增：已消除行数统计
    private int comboCount;    // 新增：连击计数
    private javax.swing.Timer gameTimer;
    private int timeLeft;
    private javax.swing.Timer countdownTimer;
    private JFrame frame;
    private JPanel mainPanel;
    private Tetromino nextTetromino;

    public GamePanel(JFrame frame, JPanel mainPanel) {
        this.frame = frame;
        this.mainPanel = mainPanel;
        setPreferredSize(new Dimension(BOARD_WIDTH * CELL_SIZE + SIDEBAR_WIDTH, BOARD_HEIGHT * CELL_SIZE));
        board = new int[BOARD_HEIGHT][BOARD_WIDTH];
        isPaused = false;
        score = 0;
        level = 1;
        linesCleared = 0;
        comboCount = 0;

        // 初始化棋盘
        for(int i = 0; i < BOARD_HEIGHT; i++) {
            for(int j = 0; j < BOARD_WIDTH; j++) {
                board[i][j] = 0;
            }
        }

        // 设置键盘监听
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        setFocusable(true);

        // 设置游戏定时器
        gameTimer = new javax.swing.Timer(GameConfig.calculateSpeed(level), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isPaused) {
                    if (currentTetromino != null) {
                        currentTetromino.move(0, 1);
                        if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                            currentTetromino.move(0, -1);
                            currentTetromino.lockToBoard(board);
                            clearCompleteRows();
                            generateNewTetromino();
                        }
                        repaint();
                    } else {
                        gameOver();
                    }
                }
            }
        });
    }

    public void startGame() {
        score = 0;
        level = 1;
        linesCleared = 0;
        comboCount = 0;
        
        // 初始化棋盘
        for(int i = 0; i < BOARD_HEIGHT; i++) {
            for(int j = 0; j < BOARD_WIDTH; j++) {
                board[i][j] = 0;
            }
        }
        
        // 初始化倒计时 - 修改为使用配置参数
        timeLeft = GameConfig.BASE_COUNTDOWN;
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        if (GameConfig.USE_COUNTDOWN_TIMER) {
            countdownTimer = new javax.swing.Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timeLeft--;
                    if (timeLeft <= 0) {
                        gameOver();
                    }
                    repaint();
                }
            });
            countdownTimer.start();
        }
        
        // 生成方块
        nextTetromino = new Tetromino(new Random().nextInt(7)+1, 0, 0);
        generateNewTetromino();
        gameTimer.setDelay(GameConfig.calculateSpeed(level));
        gameTimer.start();
        requestFocusInWindow();
    }
    
    // 生成下一个方块
    private void generateNewTetromino() {
        if(nextTetromino == null) {
            nextTetromino = new Tetromino(new Random().nextInt(7)+1, 0, 0);
        }
        currentTetromino = nextTetromino;
        currentTetromino.move(BOARD_WIDTH/2 - 2 - currentTetromino.getX(), 0 - currentTetromino.getY());
        nextTetromino = new Tetromino(new Random().nextInt(7)+1, 0, 0);
        if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
            gameOver();
        }
    }

    private void handleKeyPress(KeyEvent e) {
        if (currentTetromino == null || isPaused) return;

        int key = e.getKeyCode();
        switch(key) {
            case KeyEvent.VK_LEFT:
                currentTetromino.move(-1, 0);
                if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                    currentTetromino.move(1, 0);
                }
                break;
            case KeyEvent.VK_RIGHT:
                currentTetromino.move(1, 0);
                if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                    currentTetromino.move(-1, 0);
                }
                break;
            case KeyEvent.VK_DOWN:
                currentTetromino.move(0, 1);
                if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                    currentTetromino.move(0, -1);
                    currentTetromino.lockToBoard(board);
                    clearCompleteRows();
                    generateNewTetromino();
                }
                break;
            case KeyEvent.VK_UP:
                currentTetromino.rotate();
                if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                    currentTetromino.rotateBack();
                }
                break;
            case KeyEvent.VK_SPACE:
                while (!currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                    currentTetromino.move(0, 1);
                }
                currentTetromino.move(0, -1);
                currentTetromino.lockToBoard(board);
                clearCompleteRows();
                generateNewTetromino();
                break;
            case KeyEvent.VK_P:
                togglePause();
                break;
            default:
                break;
        }
        repaint();
    }

    private void clearCompleteRows() {
        ArrayList<Integer> rowsToClear = new ArrayList<>();
        
        // 检测完整行
        for(int i = 0; i < BOARD_HEIGHT; i++) {
            boolean isComplete = true;
            for(int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] == 0) {
                    isComplete = false;
                    break;
                }
            }
            if (isComplete) {
                rowsToClear.add(i);
            }
        }
        
        // 如果有行可消除
        if (!rowsToClear.isEmpty()) {
            comboCount++; // 增加连击计数
            int lines = rowsToClear.size();
            linesCleared += lines;
            
            // 计算得分，使用新的积分系统
            int earnedScore = GameConfig.calculateScore(lines, comboCount, level);
            score += earnedScore;
            
            // 动画效果：闪烁消除行
            flashRows(rowsToClear);
            
            // 移除完整行
            for(int row : rowsToClear) {
                for(int j = 0; j < BOARD_WIDTH; j++) {
                    board[row][j] = 0;
                }
                for(int i = row; i > 0; i--) {
                    for(int j = 0; j < BOARD_WIDTH; j++) {
                        board[i][j] = board[i-1][j];
                    }
                }
                for(int j = 0; j < BOARD_WIDTH; j++) {
                    board[0][j] = 0;
                }
            }
            
            checkLevelUp();
        } else {
            comboCount = 0; // 重置连击计数
        }
    }
    
    // 简单的行闪烁动画
    private void flashRows(ArrayList<Integer> rows) {
        // 实际项目中可以添加闪烁动画效果
        repaint();
    }

    private void checkLevelUp() {
        // 新的等级系统：每消除10行升一级
        int newLevel = linesCleared / GameConfig.LINES_PER_LEVEL + 1;
        if (newLevel > level) {
            level = newLevel;
            gameTimer.setDelay(GameConfig.calculateSpeed(level));
            
            // 修改：只有在使用倒计时的情况下才重置倒计时
            if (GameConfig.USE_COUNTDOWN_TIMER) {
                // 重置倒计时，但每关减少的时间不能太多
                timeLeft = Math.max(60, GameConfig.BASE_COUNTDOWN - (level-1) * GameConfig.COUNTDOWN_DECREASE);
            }
        }
    }
    
    private void gameOver() {
        gameTimer.stop();
        if (countdownTimer != null && GameConfig.USE_COUNTDOWN_TIMER) {
            countdownTimer.stop();
        }
        
        // 添加关卡信息到游戏结束提示
        JOptionPane.showMessageDialog(frame, 
            "游戏结束！\n" +
            "最终得分：" + score + "\n" +
            "达到关卡：" + level + "\n" +
            "消除行数：" + linesCleared, 
            "游戏结束", JOptionPane.INFORMATION_MESSAGE);
            
        saveHighScore();
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "Start");
    }

    private void saveHighScore() {
        try {
            File file = new File("highscores.txt");
            ArrayList<Integer> scores = new ArrayList<>();
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextInt()) {
                    scores.add(scanner.nextInt());
                }
                scanner.close();
            }
            scores.add(score);
            Collections.sort(scores, Collections.reverseOrder());
            if (scores.size() > 5) {
                scores = new ArrayList<>(scores.subList(0, 5));
            }
            PrintWriter writer = new PrintWriter(file);
            for(int s : scores) {
                writer.println(s);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLevel(int level) {
        this.level = level;
        gameTimer.setDelay(GameConfig.calculateSpeed(level));
    }

    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            gameTimer.stop();
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
        } else {
            gameTimer.start();
            if (countdownTimer != null) {
                countdownTimer.start();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 启用抗锯齿
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制主游戏区域背景
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRect(0, 0, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
        
        // 绘制网格线
        g2d.setColor(new Color(50, 50, 60));
        for (int i = 0; i <= BOARD_WIDTH; i++) {
            g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
        }
        for (int i = 0; i <= BOARD_HEIGHT; i++) {
            g2d.drawLine(0, i * CELL_SIZE, BOARD_WIDTH * CELL_SIZE, i * CELL_SIZE);
        }

        // 绘制棋盘上的方块
        for(int i = 0; i < BOARD_HEIGHT; i++) {
            for(int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] != 0) {
                    drawBlock(g2d, j, i, getColorForType(board[i][j]));
                }
            }
        }

        // 绘制当前方块
        if (currentTetromino != null) {
            for(Point p : currentTetromino.getBlocks()) {
                int x = p.x + currentTetromino.getX();
                int y = p.y + currentTetromino.getY();
                if (y >= 0 && y < BOARD_HEIGHT && x >= 0 && x < BOARD_WIDTH) {
                    drawBlock(g2d, x, y, currentTetromino.getColor());
                }
            }
        }

        // 绘制侧边栏
        drawSidebar(g2d);
    }
    
    // 获取方块类型对应的颜色
    private Color getColorForType(int type) {
        return Tetromino.getColorForType(type);
    }
    
    // 绘制单个方块，带有3D效果
    private void drawBlock(Graphics2D g, int x, int y, Color color) {
        int blockSize = CELL_SIZE - 1;
        int xPos = x * CELL_SIZE;
        int yPos = y * CELL_SIZE;
        
        // 主体填充
        g.setColor(color);
        g.fillRect(xPos, yPos, blockSize, blockSize);
        
        // 亮边（左上）
        g.setColor(color.brighter());
        g.drawLine(xPos, yPos, xPos + blockSize - 1, yPos); // 上边
        g.drawLine(xPos, yPos, xPos, yPos + blockSize - 1); // 左边
        
        // 暗边（右下）
        g.setColor(color.darker());
        g.drawLine(xPos + blockSize - 1, yPos, xPos + blockSize - 1, yPos + blockSize - 1); // 右边
        g.drawLine(xPos, yPos + blockSize - 1, xPos + blockSize - 1, yPos + blockSize - 1); // 下边
    }
    
    // 绘制侧边栏
    private void drawSidebar(Graphics2D g) {
        int sidebarX = BOARD_WIDTH * CELL_SIZE + 10;
        
        // 侧边栏背景
        g.setColor(new Color(40, 40, 50));
        g.fillRect(BOARD_WIDTH * CELL_SIZE + 5, 0, SIDEBAR_WIDTH, BOARD_HEIGHT * CELL_SIZE);
        
        // 绘制游戏信息
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.white);
        g.drawString("得分: " + score, sidebarX, 30);
        g.drawString("关卡: " + level, sidebarX, 50);
        g.drawString("行数: " + linesCleared, sidebarX, 70);
        g.drawString("连击: " + comboCount, sidebarX, 90);
        
        // 只在使用倒计时时显示倒计时
        if (GameConfig.USE_COUNTDOWN_TIMER) {
            g.drawString("倒计时: " + timeLeft + "s", sidebarX, 110);
            // 下一个方块预览标题在110以后
            g.drawString("下一个:", sidebarX, 140);
        } else {
            // 没有倒计时，下一个方块预览标题可以上移
            g.drawString("下一个:", sidebarX, 120);
        }
        
        // 绘制下一个方块预览
        if(nextTetromino != null) {
            int previewX = sidebarX + 10;
            // 调整预览Y位置，根据是否显示倒计时
            int previewY = GameConfig.USE_COUNTDOWN_TIMER ? 160 : 140;
            
            // 其余绘制代码不变...
            int previewCellSize = 20;
            g.setColor(new Color(30, 30, 40));
            g.fillRect(previewX - 5, previewY - 5, 90, 90);
            
            for(Point p : nextTetromino.getBlocks()){
                int blockX = previewX + p.x * previewCellSize;
                int blockY = previewY + p.y * previewCellSize;
                int blockSize = previewCellSize - 1;
                Color color = nextTetromino.getColor();
                
                g.setColor(color);
                g.fillRect(blockX, blockY, blockSize, blockSize);
                g.setColor(color.brighter());
                g.drawLine(blockX, blockY, blockX + blockSize - 1, blockY);
                g.drawLine(blockX, blockY, blockX, blockY + blockSize - 1);
                g.setColor(color.darker());
                g.drawLine(blockX + blockSize - 1, blockY, blockX + blockSize - 1, blockY + blockSize - 1);
                g.drawLine(blockX, blockY + blockSize - 1, blockX + blockSize - 1, blockY + blockSize - 1);
            }
        }
        
        // 添加更多游戏信息
        int infoY = GameConfig.USE_COUNTDOWN_TIMER ? 280 : 260;
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("操作说明:", sidebarX, infoY);
        g.drawString("↑: 旋转", sidebarX, infoY + 20);
        g.drawString("←→: 移动", sidebarX, infoY + 40);
        g.drawString("↓: 加速下落", sidebarX, infoY + 60);
        g.drawString("空格: 直接落底", sidebarX, infoY + 80);
        g.drawString("P: 暂停游戏", sidebarX, infoY + 100);
    }
}

// 更新方块类，使用新的颜色系统
class Tetromino {
    private static final int BOARD_WIDTH = GameConfig.BOARD_WIDTH;
    private static final int BOARD_HEIGHT = GameConfig.BOARD_HEIGHT;
    
    private int type;
    private ArrayList<Point> blocks;
    private int x, y;
    private Color color;
    private int rotationState = 0;
    
    // 使用HSV颜色模型生成的颜色
    private static final Color[] TYPE_COLORS = ColorUtils.generateTetrominoColors();

    public Tetromino(int type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.color = TYPE_COLORS[(type - 1) % TYPE_COLORS.length];
        this.blocks = getBlocksForType(type, rotationState);
    }
    
    // 获取特定类型方块的颜色
    public static Color getColorForType(int type) {
        return TYPE_COLORS[(type - 1) % TYPE_COLORS.length];
    }

    // 根据方块类型和旋转状态获取方块形状
    private ArrayList<Point> getBlocksForType(int type, int rotation) {
        ArrayList<Point> blocks = new ArrayList<>();
        switch(type) {
            case 1: // I
                if (rotation % 2 == 0) { // 水平
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(3, 0));
                } else { // 垂直
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(1, 2));
                }
                break;
            case 2: // O
                blocks.add(new Point(0, 0));
                blocks.add(new Point(1, 0));
                blocks.add(new Point(0, 1));
                blocks.add(new Point(1, 1));
                break;
            case 3: // T
                if (rotation == 0) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(1, 1));
                } else if (rotation == 1) {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(0, 0));
                } else if (rotation == 2) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(1, -1));
                } else {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(2, 0));
                }
                break;
            case 4: // S
                if (rotation % 2 == 0) {
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(0, 1));
                    blocks.add(new Point(1, 1));
                } else {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(0, 1));
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                }
                break;
            case 5: // Z
                if (rotation % 2 == 0) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(2, 1));
                } else {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(0, 1));
                }
                break;
            case 6: // J
                if (rotation == 0) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(0, 1));
                } else if (rotation == 1) {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(0, 1));
                } else if (rotation == 2) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(2, -1));
                } else {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(2, -1));
                }
                break;
            case 7: // L
                if (rotation == 0) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(2, 1));
                } else if (rotation == 1) {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(0, -1));
                } else if (rotation == 2) {
                    blocks.add(new Point(0, 0));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(2, 0));
                    blocks.add(new Point(0, -1));
                } else {
                    blocks.add(new Point(1, -1));
                    blocks.add(new Point(1, 0));
                    blocks.add(new Point(1, 1));
                    blocks.add(new Point(2, 1));
                }
                break;
        }
        return blocks;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public void rotate() {
        rotationState = (rotationState + 1) % 4;
        blocks = getBlocksForType(type, rotationState);
    }

    public void rotateBack() {
        rotationState = (rotationState + 3) % 4;
        blocks = getBlocksForType(type, rotationState);
    }

    public boolean collidesWithBoard(int[][] board, int boardWidth, int boardHeight) {
        for (Point p : blocks) {
            int newX = x + p.x;
            int newY = y + p.y;
            if (newX < 0 || newX >= boardWidth || newY >= boardHeight || (newY >= 0 && board[newY][newX] != 0)) {
                return true;
            }
        }
        return false;
    }

    public void lockToBoard(int[][] board) {
        for (Point p : blocks) {
            int newX = x + p.x;
            int newY = y + p.y;
            if (newY >= 0 && newY < board.length && newX >= 0 && newX < board[0].length) {
                board[newY][newX] = type;
            }
        }
    }

    public ArrayList<Point> getBlocks() {
        return blocks;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }
}

// 高分面板更新，现在实际实现分数加载
class HighScorePanel extends JPanel {
    private JTextArea scoreArea;
    
    public HighScorePanel(JFrame frame, JPanel mainPanel) {
        setLayout(new BorderLayout());
        JLabel title = new JLabel("高分榜", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        scoreArea = new JTextArea(10, 20);
        scoreArea.setEditable(false);
        scoreArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        add(new JScrollPane(scoreArea), BorderLayout.CENTER);

        JButton backButton = new JButton("返回");
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "Start");
            }
        });
        add(backButton, BorderLayout.SOUTH);
    }

    public void updateScores() {
        try {
            File file = new File("highscores.txt");
            StringBuilder sb = new StringBuilder();
            sb.append("排名\t分数\n");
            sb.append("------------------\n");
            
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                int rank = 1;
                while (scanner.hasNextInt() && rank <= 10) {
                    int score = scanner.nextInt();
                    sb.append(rank).append(".\t").append(score).append("\n");
                    rank++;
                }
                scanner.close();
            } else {
                sb.append("暂无分数记录\n");
            }
            
            scoreArea.setText(sb.toString());
        } catch (IOException e) {
            scoreArea.setText("加载分数时出错: " + e.getMessage());
        }
    }
}

// 菜单栏
class MenuBar extends JMenuBar {
    public MenuBar(JFrame frame, GamePanel gamePanel, JPanel mainPanel) {
        JMenu gameMenu = new JMenu("游戏");
        JMenuItem startItem = new JMenuItem("开始新游戏");
        startItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.startGame();
            }
        });
        gameMenu.add(startItem);

        JMenuItem pauseItem = new JMenuItem("暂停/继续");
        pauseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.togglePause();
            }
        });
        gameMenu.add(pauseItem);

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        gameMenu.add(exitItem);

        add(gameMenu);

        JMenu viewMenu = new JMenu("查看");
        JMenuItem highScoreItem = new JMenuItem("高分榜");
        highScoreItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "HighScore");
                HighScorePanel highScorePanel = (HighScorePanel) mainPanel.getComponent(2);
                highScorePanel.updateScores();
            }
        });
        viewMenu.add(highScoreItem);

        add(viewMenu);
    }
}