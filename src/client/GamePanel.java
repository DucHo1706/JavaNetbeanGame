package client;

import server.Level;
import game.Monster;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.io.PrintWriter;
import utils.Constants;

/**
 * Panel chinh hien thi game - da duoc refactor
 */
public class GamePanel extends JPanel {
    // Kich thuoc grid
    private int gridWidth = 24;
    private int gridHeight = 16;
    
    // Cac thanh phan chinh
    private GameRenderer renderer;
    private GameLogic gameLogic;
    
    // Du lieu game
    private Point firePlayer;
    private Point waterPlayer;
    private Point myPlayer;
    private Point otherPlayer;
    private Set<Point> walls;
    private Point door;
    private String status;
    private String playerType; // Bo enum, dung String
    private int currentLevelNumber = 1;
    private Level currentLevelData;
     private JLabel timeLabel;
    // Animation
    private Timer animationTimer;
    private Timer monsterTimer;
    private boolean showWinAnimation = false;
    
    public GamePanel() {
        initializeComponents();
        initializeData();
        setupTimers();
        updatePanelSize();
          timeLabel = new JLabel("Thời gian: 00:00");
        this.add(timeLabel);
    }
     public void updateTimeRemaining(int giayConLai) {
        int phut = giayConLai / 60;
        int giay = giayConLai % 60;
        String timeText = String.format("Thời gian: %02d:%02d", phut, giay);
       timeLabel.setText(timeText);
      timeLabel.setForeground(Color.WHITE);                    // Màu chữ trắng
      timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));  // Font đẹp hơn, size 18

      // Thêm hiệu ứng khi thời gian sắp hết 
      if (giayConLai <= 30) {
          timeLabel.setForeground(Color.RED);     // Đổi màu đỏ khi < 30 giây
      } else if (giayConLai <= 60) {
          timeLabel.setForeground(Color.YELLOW);  // Màu vàng khi < 1 phút
      } else {
          timeLabel.setForeground(Color.WHITE);   // Màu trắng bình thường
      }
    }
    /**
     * Khoi tao cac components
     */
    private void initializeComponents() {
        renderer = new GameRenderer();
        gameLogic = new GameLogic(this);
        
        setBackground(new Color(34, 34, 34));
        setFocusable(true);
    }
    
    /**
     * Khoi tao du lieu ban dau
     */
    private void initializeData() {
        walls = new HashSet<>();
        firePlayer = new Point(1, 1);
        waterPlayer = new Point(1, 1);
        myPlayer = new Point(1, 1);
        status = "Dang ket noi...";
    }
    
    /**
     * Thiet lap cac timer
     */
    private void setupTimers() {
        // Animation timer
        animationTimer = new Timer(100, e -> {
            renderer.updateAnimation();
            repaint();
        });
        animationTimer.start();
        
        // Monster movement timer
        monsterTimer = new Timer(500, e -> {
            gameLogic.updateMonsters();
            repaint();
        });
        monsterTimer.start();
    }
    
    /**
     * Cap nhat kich thuoc panel
     */
    private void updatePanelSize() {
        int newWidth = gridWidth * Constants.CELL_SIZE;
        int newHeight = gridHeight * Constants.CELL_SIZE;
        
        setPreferredSize(new Dimension(newWidth, newHeight));
        setSize(new Dimension(newWidth, newHeight));
        
        // Cap nhat parent containers
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
    }
    
  
    /**
     * Set ket noi mang
     */
    public void setConnection(PrintWriter out) {
        gameLogic.setConnection(out);
    }
    
    /**
     * Set loai nguoi choi
     * Nhan String truc tiep
     */
    public void setPlayerType(String playerTypeStr) {
        this.playerType = playerTypeStr;
        if ("FIRE".equals(playerType)) {
            myPlayer = firePlayer;
        } else if ("WATER".equals(playerType)) {
            myPlayer = waterPlayer;
        }
        repaint();
    }
    
    /**
     * Load level moi
     */
    public void loadLevel(String levelData) {
        System.out.println("DEBUG: loadLevel called with: " + levelData);
        
        showWinAnimation = false;
        parseLevelData(levelData);
        updatePanelSize();
        gameLogic.sendPlayerUpdate();
        repaint();
    }
    
    /**
     * Di chuyen nguoi choi
     */
    public boolean movePlayer(String direction) {
        return gameLogic.movePlayer(direction);
    }
    
    /**
     * Cap nhat nguoi choi khac
     */
    public void updateOtherPlayer(String playerId, int x, int y, String direction) {
        if ("FIRE".equals(playerType)) {
            if (waterPlayer != null) waterPlayer.setLocation(x, y);
        } else if ("WATER".equals(playerType)) {
            if (firePlayer != null) firePlayer.setLocation(x, y);
        }
        repaint();
    }
    
    /**
     * Phan tich du lieu level tu server
     */
    private void parseLevelData(String levelData) {
        String[] parts = levelData.split(":");
        if (parts.length < 6) return;
        
        // Parse basic info
        String levelName = parts[0];
        String[] dimensions = parts[1].split("x");
        gridWidth = Integer.parseInt(dimensions[0]);
        gridHeight = Integer.parseInt(dimensions[1]);
        
        // Parse walls
        parseWalls(parts);
        
        // Parse spawn points va door
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
        
        // Tao du lieu level
        List<Point> wallsList = new ArrayList<>(walls);
        currentLevelData = new Level(levelName, gridWidth, gridHeight, 
                                    wallsList, waterSpawn, fireSpawn, door);
        
        // Parse monsters
        parseMonsterData(parts);
        
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
    
    /**
     * Parse spawn point theo ten (String)
     */
    private Point parseSpawnPoint(String[] parts, String typeName) {
        for (int i = 0; i < parts.length; i++) {
            if (typeName.equals(parts[i]) && i + 1 < parts.length) {
                String[] coords = parts[i + 1].split(",");
                if (coords.length == 2) {
                    return new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                }
            }
        }
        return new Point(0, 0); // Default
    }
    
    /**
     * parse monster data
     */
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
                                
                                System.out.println("Added monster: " + type + " at (" + x + "," + y + ")");
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
    
    public Point getPlayerPosition() {
        return myPlayer != null ? new Point(myPlayer) : null;
    }
}