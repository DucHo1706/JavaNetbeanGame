package client;

import game.Item;
import server.Level;
import game.Monster;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.io.PrintWriter;
import utils.Constants;

public class GamePanel extends JPanel {
    
    private int gridWidth = 24;
    private int gridHeight = 16;
    
    private GameRenderer renderer;
    private GameLogic gameLogic;
    private NetworkManager networkManager;
    private SoundManager soundManager;
    
    private Point firePlayer;
    private Point waterPlayer;
    private Point myPlayer;
    private Point otherPlayer;
    private Set<Point> walls;
    private Point door;
    private String status;
    private String playerType;
    private int currentLevelNumber = 1;
    private Level currentLevelData;
    
    private JButton readyButton;
    private JTextArea readyStatusArea;
    private Set<String> nguoiChoiDaSanSang = new HashSet<>();
    
    private Timer animationTimer;
    private Timer monsterTimer;
    private boolean showWinAnimation = false;
    private int currentTime = 60;

    private int playerScore = 0;
    private int otherPlayerScore = 0;
    
        public void updateScore(int score) {
            this.playerScore = score;
            repaint();
        }

        public void updateOtherPlayerScore(int score) { 
            this.otherPlayerScore = score;
            repaint();
        }

        public void playItemCollectSound() {
            if (soundManager != null) {
                soundManager.playSoundEffect("/Sound/COLLECTION.wav");
            }
        }
    public GamePanel() {
        initializeComponents();
        initializeData();
        setupTimers();
        setupUI();
        updatePanelSize();
        
        soundManager = new SoundManager();
        soundManager.playSoundHitMonsterEffect("/Sound/Fireboy and Watergirl Soundtrack Main Level Theme.wav");
    }

    private void initializeComponents() {
        renderer = new GameRenderer();
        gameLogic = new GameLogic(this);
        setBackground(new Color(34, 34, 34));
        setFocusable(true);
    }

    private void initializeData() {
        walls = new HashSet<>();
        firePlayer = new Point(1, 1);
        waterPlayer = new Point(1, 1);
        myPlayer = new Point(1, 1);
        status = "Dang ket noi...";
    }

    private void setupTimers() {
        animationTimer = new Timer(100, e -> {
            renderer.updateAnimation();
            repaint();
        });
        animationTimer.start();

        monsterTimer = new Timer(500, e -> {
            gameLogic.updateMonsters();
            repaint();
        });
        monsterTimer.start();
    }

    private void setupUI() {
        readyButton = new JButton("Sẵn sàng");
        readyButton.setVisible(false);
        readyButton.addActionListener(e -> {
            if (networkManager != null) {
                networkManager.sendMessage("SAN_SANG");
            }
            readyButton.setEnabled(false);
        });
        this.add(readyButton);
    }

    private void updatePanelSize() {
        int newWidth = gridWidth * Constants.CELL_SIZE;
        int newHeight = gridHeight * Constants.CELL_SIZE;

        setPreferredSize(new Dimension(newWidth, newHeight));
        setSize(new Dimension(newWidth, newHeight));

        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent.repaint();
            if (parent instanceof JFrame) {
                ((JFrame) parent).pack();
                break;
            }
            parent = parent.getParent();
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g, this);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String timeText = "Thời gian: " + currentTime + "s";
        FontMetrics fm = g2d.getFontMetrics();
        int x = getWidth() - fm.stringWidth(timeText) - 20;
        g2d.drawString(timeText, x, 30);
        
        // Hiển thị điểm số
          String scoreText = "Điểm: " + playerScore;
          g2d.drawString(scoreText, 20, 30);

          // Hiển thị điểm đối thủ (nếu có)
          if (otherPlayerScore > 0) {
              String otherScoreText = "Đối thủ: " + otherPlayerScore;
              g2d.drawString(otherScoreText, 20, 60);
          }

    }
        public int getPlayerScore() { return playerScore; }
        public int getOtherPlayerScore() { return otherPlayerScore; }
    
    
    public void updateTimeDisplay(int timeLeft) {
        this.currentTime = timeLeft;
        repaint();
    }

    public void resetGameState() {
        walls.clear();
        firePlayer = new Point(1, 1);
        waterPlayer = new Point(1, 1);
        myPlayer = new Point(1, 1);
        otherPlayer = null;
        door = null;
        currentLevelNumber = 1;
        currentLevelData = null;
        playerScore = 0;
        otherPlayerScore = 0;
        status = "Chưa vào phòng";
        playerType = null;
        currentTime = 60;
        nguoiChoiDaSanSang.clear();
        showWinAnimation = false;
        repaint();
    }

    public void setNetworkManager(NetworkManager nm) {
        this.networkManager = nm;
    }

    public void setConnection(PrintWriter out) {
        gameLogic.setConnection(out);
    }

    public void setPlayerType(String playerTypeStr) {
        this.playerType = playerTypeStr;
        if ("FIRE".equals(playerType)) {
            myPlayer = firePlayer;
        } else if ("WATER".equals(playerType)) {
            myPlayer = waterPlayer;
        }
        repaint();
    }

    public void loadLevel(String levelData) {
        showWinAnimation = false;
        parseLevelData(levelData);
        updatePanelSize();
        gameLogic.sendPlayerUpdate();
        repaint();
    }

    public boolean movePlayer(String direction) {
        return gameLogic.movePlayer(direction);
    }

    public void updateOtherPlayer(String playerId, int x, int y, String direction) {
        if ("FIRE".equals(playerType)) {
            if (waterPlayer != null) waterPlayer.setLocation(x, y);
        } else if ("WATER".equals(playerType)) {
            if (firePlayer != null) firePlayer.setLocation(x, y);
        }
        repaint();
    }

    public void showReadyButton(boolean show) {
        SwingUtilities.invokeLater(() -> {
            readyButton.setVisible(show);
            readyButton.setEnabled(true);
            if (show) {
                resetReadyStatus();
            }
        });
    }

    public void updateReadyStatus(String playerId) {
        nguoiChoiDaSanSang.add(playerId);
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder("Người chơi đã sẵn sàng:\n");
            for (String id : nguoiChoiDaSanSang) {
                sb.append("- ").append(id).append("\n");
            }
            if (readyStatusArea != null) {
                readyStatusArea.setText(sb.toString());
            }
        });
    }

    public void resetReadyStatus() {
        nguoiChoiDaSanSang.clear();
        SwingUtilities.invokeLater(() -> {
            if (readyStatusArea != null) {
                readyStatusArea.setText("");
            }
        });
    }


   
    private void parseLevelData(String levelData) {
        String[] parts = levelData.split(":");
        if (parts.length < 6) {
            return;
        }

        String levelName = parts[0];
        String[] dimensions = parts[1].split("x");
        gridWidth = Integer.parseInt(dimensions[0]);
        gridHeight = Integer.parseInt(dimensions[1]);

        parseWalls(parts);

        Point fireSpawn = parseSpawnPoint(parts, "FIRE");
        Point waterSpawn = parseSpawnPoint(parts, "WATER");
        door = parseSpawnPoint(parts, "DOOR");

        if (firePlayer == null) firePlayer = new Point();
        if (waterPlayer == null) waterPlayer = new Point();
        firePlayer.setLocation(fireSpawn);
        waterPlayer.setLocation(waterSpawn);

        if ("FIRE".equals(playerType)) {
            myPlayer.setLocation(fireSpawn);
        } else if ("WATER".equals(playerType)) {
            myPlayer.setLocation(waterSpawn);
        }

        List<Point> wallsList = new ArrayList<>(walls);
        currentLevelData = new Level(levelName, gridWidth, gridHeight, wallsList, waterSpawn, fireSpawn, door);

        parseMonsterData(parts);
        parseItemData(parts);
        setStatus("Level " + currentLevelNumber + " - Di chuyen den cua vang!");
    }

    private void parseWalls(String[] parts) {
        walls.clear();
        if (parts.length > 3 && !parts[3].isEmpty()) {
            String[] wallCoords = parts[3].split(";");
            for (String coord : wallCoords) {
                String[] xy = coord.split(",");
                if (xy.length == 2) {
                    walls.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                }
            }
        }
    }

    private Point parseSpawnPoint(String[] parts, String typeName) {
        for (int i = 0; i < parts.length; i++) {
            if (typeName.equals(parts[i]) && i + 1 < parts.length) {
                String[] coords = parts[i + 1].split(",");
                if (coords.length == 2) {
                    return new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                }
            }
        }
        return new Point(0, 0);
    }

    private void parseMonsterData(String[] parts) {
        if (currentLevelData == null) return;

        for (int i = 0; i < parts.length; i++) {
            if ("MONSTERS".equals(parts[i]) && i + 1 < parts.length) {
                String monsterData = parts[i + 1];
                if (!monsterData.isEmpty()) {
                    String[] monsters = monsterData.split(";");
                    int monsterId = 1;

                    for (String monster : monsters) {
                        String[] monsterInfo = monster.split(",");
                        if (monsterInfo.length >= 3) {
                            try {
                                int x = Integer.parseInt(monsterInfo[0]);
                                int y = Integer.parseInt(monsterInfo[1]);
                                String type = monsterInfo[2];
                                Monster newMonster = new Monster(monsterId++, new Point(x, y), type);
                                currentLevelData.addMonster(newMonster);
                            } catch (NumberFormatException e) {
                                System.err.println("Error parsing monster data: " + monster);
                            }
                        }
                    }
                }
                break;
            }
        }
    }
private void parseItemData(String[] parts) {
    if (currentLevelData == null) return;

    for (int i = 0; i < parts.length; i++) {
        if ("ITEMS".equals(parts[i]) && i + 1 < parts.length) {
            String itemData = parts[i + 1];
            if (!itemData.isEmpty()) {
                String[] items = itemData.split(";");
                int itemId = 1;
                for (String itemStr : items) {
                    if (itemStr.trim().isEmpty()) continue;
                    String[] itemInfo = itemStr.split(",");
                    if (itemInfo.length >= 3) {
                        try {
                            int x = Integer.parseInt(itemInfo[0]);
                            int y = Integer.parseInt(itemInfo[1]);
                            String type = itemInfo[2];
                            Item item = new Item(itemId++, new Point(x, y), type);
                            currentLevelData.addItem(item);
                        } catch (NumberFormatException e) {
                            System.err.println("Lỗi parse item: " + itemStr);
                        }
                    }
                }
            }
            break;
        }
    }
}

    public void playMoveSound() {
        if (soundManager != null) {
            soundManager.playSoundEffect("/Sound/WalkingSoundEffect.wav");
        }
    }
    
    public void playMonsterCollisionSound() {
        if (soundManager != null) {
            soundManager.playSoundHitMonsterEffect("/Sound/OuchDuck.wav");
        }
    }

    public void PlayWinningSoundRound1() {
        if (soundManager != null) {
            soundManager.WinEffectSound("/Sound/WinningRound1.wav");
        }
    }
     public void PlayLOSESound() {
        if (soundManager != null) {
            soundManager.WinEffectSound("/Sound/LOST.wav");
        }
    }

    public Point getFirePlayer() { return firePlayer; }
    public Point getWaterPlayer() { return waterPlayer; }
    public Point getMyPlayer() { return myPlayer; }
    public Point getOtherPlayer() { return otherPlayer; }
    public Set<Point> getWalls() { return walls; }
    public Point getDoor() { return door; }
    public String getStatus() { return status; }
    public String getPlayerType() { return playerType; }
    public Level getCurrentLevelData() { return currentLevelData; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public boolean isShowWinAnimation() { return showWinAnimation; }
    public Point getPlayerPosition() { return myPlayer != null ? new Point(myPlayer) : null; }

    public void setStatus(String status) { 
        this.status = status; 
        repaint();
    }

    public void setCurrentLevel(int level) { 
        this.currentLevelNumber = level; 
    }

    public void setShowWinAnimation(boolean show) { 
        this.showWinAnimation = show; 
    }
}
