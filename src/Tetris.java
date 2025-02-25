import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
// 在 import 区域添加背景音效相关类
// import javax.sound.sampled.*;

public class Tetris {
    // 新增：播放背景音效的方法（bgm.wav 应放在应用目录下）
    /*
    private static void playBackgroundMusic() {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File("bgm.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    */

    public static void main(String[] args) {
        //System.out.println("程序开始运行");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
                // 调用背景音效播放方法
                // playBackgroundMusic();
            }
        });
    }

    // 修改 createAndShowGUI 方法，使用 frame.pack() 自动设置窗口尺寸
    private static void createAndShowGUI() {
       // System.out.println("开始创建GUI");
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

        // 设置菜单栏
        JMenuBar menuBar = new MenuBar(frame, gamePanel, mainPanel);
        frame.setJMenuBar(menuBar);

        frame.add(mainPanel);
      //  System.out.println("程序显示界面");
        // 使用 pack() 而不再使用固定尺寸设置窗口大小
        frame.pack();
        frame.setLocationRelativeTo(null);
       // System.out.println("GUI创建完成，即将显示");
        frame.setVisible(true);
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
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int CELL_SIZE = 30;
    // 调整整体界面宽度，预留侧边区显示下一个方块和倒计时
    private static final int SIDEBAR_WIDTH = 120; 

    private int[][] board;
    private Tetromino currentTetromino;
    private boolean isPaused;
    private int score;
    private int level;
    private javax.swing.Timer gameTimer;
    // 新增倒计时字段
    private int timeLeft;
    private javax.swing.Timer countdownTimer;
    private JFrame frame;
    private JPanel mainPanel;
    // 新增：下一个方块字段
    private Tetromino nextTetromino;

    public GamePanel(JFrame frame, JPanel mainPanel) {
        this.frame = frame;
        this.mainPanel = mainPanel;
        setPreferredSize(new Dimension(BOARD_WIDTH * CELL_SIZE + SIDEBAR_WIDTH, BOARD_HEIGHT * CELL_SIZE));
        board = new int[BOARD_HEIGHT][BOARD_WIDTH];
        isPaused = false;
        score = 0;
        level = 1;

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

        // 设置游戏定时器，使用 javax.swing.Timer
        gameTimer = new javax.swing.Timer(getSpeedForLevel(level), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isPaused) {
                    if (currentTetromino != null) {
                        // 尝试向下移动
                        currentTetromino.move(0, 1);
                        if (currentTetromino.collidesWithBoard(board, BOARD_WIDTH, BOARD_HEIGHT)) {
                            currentTetromino.move(0, -1);
                            // 锁定到棋盘
                            currentTetromino.lockToBoard(board);
                            // 检查完整行
                            clearCompleteRows();
                            // 生成新方块
                            generateNewTetromino();
                        }
                    } else {
                        // 无当前方块，游戏结束
                        gameOver();
                    }
                }
            }
        });
    }

    private int getSpeedForLevel(int level) {
        // 根据关卡调整速度
        return 1000 - (level - 1) * 200; // 简单：1000ms，中等：800ms，困难：600ms
    }

    public void startGame() {
        score = 0;
        level = 1;
        for(int i = 0; i < BOARD_HEIGHT; i++) {
            for(int j = 0; j < BOARD_WIDTH; j++) {
                board[i][j] = 0;
            }
        }
        // 初始化倒计时，每关60秒
        timeLeft = 60;
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
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
        // 新增：生成下一个方块
        nextTetromino = new Tetromino(new Random().nextInt(7)+1, 0, 0);
        generateNewTetromino();
        gameTimer.setDelay(getSpeedForLevel(level));
        gameTimer.start();
        // 添加对焦点的请求，确保键盘监听能生效
        requestFocusInWindow();
    }

    // 修改生成方块方法：由 nextTetromino 转换为 currentTetromino，同时预生成下一个
    private void generateNewTetromino() {
        if(nextTetromino == null) {
            nextTetromino = new Tetromino(new Random().nextInt(7)+1, 0, 0);
        }
        currentTetromino = nextTetromino;
        // 将当前方块的位置调整为起点
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
            score += 100 * level; // 根据关卡加分
        }
        checkLevelUp();
    }

    private void checkLevelUp() {
        int newLevel = score / 1000 + 1; // 每1000分升一级
        if (newLevel > level) {
            level = newLevel;
            gameTimer.setDelay(getSpeedForLevel(level));
            // 重置倒计时到60秒
            timeLeft = 60;
        }
    }

    private void gameOver() {
        gameTimer.stop();
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        JOptionPane.showMessageDialog(frame, "游戏结束！得分：" + score, "游戏结束", JOptionPane.INFORMATION_MESSAGE);
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
        gameTimer.setDelay(getSpeedForLevel(level));
    }

    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            gameTimer.stop();
        } else {
            gameTimer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.black);
        g.fillRect(0, 0, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);

        // 绘制棋盘
        for(int i = 0; i < BOARD_HEIGHT; i++) {
            for(int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] != 0) {
                    g.setColor(getColorForType(board[i][j]));
                    g.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                    g.setColor(Color.darkGray);
                    g.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                } else {
                    g.setColor(Color.gray);
                    g.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                }
            }
        }

        // 绘制当前方块
        if (currentTetromino != null) {
            for(Point p : currentTetromino.getBlocks()) {
                int x = p.x + currentTetromino.getX();
                int y = p.y + currentTetromino.getY();
                if (y >= 0 && y < BOARD_HEIGHT && x >= 0 && x < BOARD_WIDTH) {
                    g.setColor(currentTetromino.getColor());
                    g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                    g.setColor(Color.darkGray);
                    g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                }
            }
        }

        // 绘制得分和关卡
        g.setColor(Color.white);
        g.drawString("得分: " + score, 10, BOARD_HEIGHT * CELL_SIZE + 20);
        g.drawString("关卡: " + level, 10, BOARD_HEIGHT * CELL_SIZE + 40);
        g.drawString("倒计时: " + timeLeft + "s", 10, BOARD_HEIGHT * CELL_SIZE + 60);

        // 绘制侧边栏（右侧）
        int sidebarX = BOARD_WIDTH * CELL_SIZE + 10;
        g.setColor(Color.darkGray);
        g.fillRect(BOARD_WIDTH * CELL_SIZE + 5, 0, SIDEBAR_WIDTH, BOARD_HEIGHT * CELL_SIZE);
        g.setColor(Color.white);
        g.drawString("得分: " + score, sidebarX, 30);
        g.drawString("关卡: " + level, sidebarX, 50);
        g.drawString("倒计时: " + timeLeft + "s", sidebarX, 70);
        // 绘制下一个方块预览标题
        g.drawString("下一个:", sidebarX, 100);
        // 在侧边栏绘制 nextTetromino 的块（缩放显示）
        if(nextTetromino != null) {
            // 预览区域起始位置
            int previewX = sidebarX + 10;
            int previewY = 120;
            int previewCellSize = 20; // 缩小预览块尺寸
            for(Point p : nextTetromino.getBlocks()){
                int blockX = previewX + p.x * previewCellSize;
                int blockY = previewY + p.y * previewCellSize;
                g.setColor(nextTetromino.getColor());
                g.fillRect(blockX, blockY, previewCellSize - 1, previewCellSize - 1);
                g.setColor(Color.darkGray);
                g.drawRect(blockX, blockY, previewCellSize - 1, previewCellSize - 1);
            }
        }
    }

    private Color getColorForType(int type) {
        switch(type) {
            case 1: return Color.cyan;
            case 2: return Color.yellow;
            // 替换 Color.purple 为自定义紫色
            case 3: return new Color(128, 0, 128);
            case 4: return Color.red;
            case 5: return Color.orange;
            case 6: return Color.blue;
            case 7: return Color.green;
            default: return Color.black;
        }
    }
}

// 方块类
class Tetromino {
    // 添加常量，解决 BOARD_WIDTH 和 BOARD_HEIGHT 未定义的问题
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    
    private int type;
    private ArrayList<Point> blocks;
    private int x, y;
    private Color color;
    // 新增旋转状态字段
    private int rotationState = 0;

    public Tetromino(int type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.color = getColorForType(type);
        this.blocks = getBlocksForType(type, rotationState);
    }

    private Color getColorForType(int type) {
        switch(type) {
            case 1: return Color.cyan; // I
            case 2: return Color.yellow; // O
            // 替换 Color.purple 为自定义紫色
            case 3: return new Color(128, 0, 128);
            case 4: return Color.red; // S
            case 5: return Color.orange; // Z
            case 6: return Color.blue; // J
            case 7: return Color.green; // L
            default: return Color.black;
        }
    }

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
            // 类似地定义其他类型
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

    // 修改 rotate 方法，更新 rotationState
    public void rotate() {
        int newRotation = (rotationState + 1) % 4;
        ArrayList<Point> newBlocks = getBlocksForType(type, newRotation);
        ArrayList<Point> oldBlocks = new ArrayList<>(blocks);
        blocks = newBlocks;
        // 仅检查边界（null 表示不检测已有棋盘的占用情况）
        if (collidesWithBoard(null, BOARD_WIDTH, BOARD_HEIGHT)) {
            blocks = oldBlocks; // 恢复原先状态，不更新 rotationState
        } else {
            rotationState = newRotation; // 更新旋转状态
        }
    }

    // 修改 rotateBack 方法，更新 rotationState
    public void rotateBack() {
        rotationState = (rotationState - 1 + 4) % 4;
        blocks = getBlocksForType(type, rotationState);
    }

    private int getRotationState() {
        return rotationState;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public boolean collidesWithBoard(int[][] board, int width, int height) {
        for(Point p : blocks) {
            int bx = p.x + x;
            int by = p.y + y;
            if (bx < 0 || bx >= width || by < 0 || by >= height) {
                return true;
            }
            if (board != null && by >= 0 && board[by][bx] != 0) {
                return true;
            }
        }
        return false;
    }

    public void lockToBoard(int[][] board) {
        for(Point p : blocks) {
            int bx = p.x + x;
            int by = p.y + y;
            if (by >= 0 && by < board.length && bx >= 0 && bx < board[0].length) {
                board[by][bx] = type;
            }
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public ArrayList<Point> getBlocks() { return blocks; }
    public Color getColor() { return color; }
}

// 高分面板
class HighScorePanel extends JPanel {
    public HighScorePanel(JFrame frame, JPanel mainPanel) {
        setLayout(new BorderLayout());
        JLabel title = new JLabel("高分榜", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JTextArea scoreArea = new JTextArea(10, 20);
        scoreArea.setEditable(false);
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
        // 实现高分加载和显示
    }
}

// 菜单栏
class MenuBar extends JMenuBar {
    public MenuBar(JFrame frame, GamePanel gamePanel, JPanel mainPanel) {
        JMenu gameMenu = new JMenu("游戏");
        JMenuItem startItem = new JMenuItem("开始新游戏");
        startItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "Game");
                gamePanel.startGame();
            }
        });
        gameMenu.add(startItem);

        JMenuItem pauseItem = new JMenuItem("暂停");
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

        JMenu levelsMenu = new JMenu("关卡");
        JMenuItem easyItem = new JMenuItem("简单");
        easyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.setLevel(1);
            }
        });
        levelsMenu.add(easyItem);

        JMenuItem mediumItem = new JMenuItem("中等");
        mediumItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.setLevel(2);
            }
        });
        levelsMenu.add(mediumItem);

        JMenuItem hardItem = new JMenuItem("困难");
        hardItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gamePanel.setLevel(3);
            }
        });
        levelsMenu.add(hardItem);

        JMenu highScoresMenu = new JMenu("高分");
        JMenuItem viewScoresItem = new JMenuItem("查看高分");
        viewScoresItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "HighScore");
                HighScorePanel highScorePanel = (HighScorePanel) mainPanel.getComponent(2);
                highScorePanel.updateScores();
            }
        });
        highScoresMenu.add(viewScoresItem);

        add(gameMenu);
        add(levelsMenu);
        add(highScoresMenu);
    }
}