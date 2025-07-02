package client;

import utils.Constants;
import server.Level;
import game.Monster;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.io.PrintWriter;

/**
 * Panel chính hiển thị game - đã được refactor
 */
public class GamePanel extends JPanel {
    // Kích thước grid
    private int gridWidth = 24;
    private int gridHeight = 16;
    
    // Các thành phần chính
    private GameRenderer renderer;
    private GameLogic gameLogic;
    
    // Dữ liệu game
    private Point firePlayer;
    private Point waterPlayer;
    private Point myPlayer;
    private Point otherPlayer;
    private Set<Point> walls;
    private Point door;
    private String status;
    private String playerType; // "FIRE" hoặc "WATER"
    private int currentLevelNumber = 1;
    private Level currentLevelData;
    
    // Animation
    private Timer animationTimer;
    private Timer monsterTimer;
    private boolean showWinAnimation = false;
    
    public GamePanel() {
        initializeComponents();
        initializeData();
        setupTimers();
        updatePanelSize();
    }
    
    /**
     * Khởi tạo các components
     */
    private void initializeComponents() {
        renderer = new GameRenderer();
        gameLogic = new GameLogic(this);
        
        setBackground(new Color(34, 34, 34));
        setFocusable(true);
    }
    
    /**
     * Khởi tạo dữ liệu ban đầu
     */
    private void initializeData() {
        walls = new HashSet<>();
        firePlayer = new Point(1, 1);
        waterPlayer = new Point(1, 1);
        myPlayer = new Point(1, 1);
        status = "Đang kết nối...";
    }
    
    /**
     * Thiết lập các timer
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
     * Cập nhật kích thước panel
     */
    private void updatePanelSize() {
        int newWidth = gridWidth * Constants.CELL_SIZE;
        int newHeight = gridHeight * Constants.CELL_SIZE;
        
        setPreferredSize(new Dimension(newWidth, newHeight));
        setSize(new Dimension(newWidth, newHeight));
        
        // Cập nhật parent containers
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
    
    // ==================== PUBLIC METHODS ====================
    
    /**
     * Set kết nối mạng
     */
    public void setConnection(PrintWriter out) {
        gameLogic.setConnection(out);
    }
    
    /**
     * Set loại người chơi
     */
    public void setPlayerType(String playerType) {
        this.playerType = playerType;
        if ("FIRE".equals(playerType)) {
            myPlayer = firePlayer;
        } else {
            myPlayer = waterPlayer;
        }
        repaint();
    }
    
    /**
     * Load level mới
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
     * Di chuyển người chơi
     */
    public boolean movePlayer(String direction) {
        return gameLogic.movePlayer(direction);
    }
    
    /**
     * Cập nhật người chơi khác
     */
    public void updateOtherPlayer(String playerId, int x, int y, String direction) {
        if ("FIRE".equals(playerType)) {
            waterPlayer.setLocation(x, y);
        } else {
            firePlayer.setLocation(x, y);
        }
        repaint();
    }
    
    

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
        
        // Parse spawn points và door
        Point fireSpawn = parseSpawnPoint(parts, "FIRE");
        Point waterSpawn = parseSpawnPoint(parts, "WATER");
        door = parseSpawnPoint(parts, "DOOR");
        
        // Set player positions
        firePlayer.setLocation(fireSpawn);
        waterPlayer.setLocation(waterSpawn);
        
        if ("FIRE".equals(playerType)) {
            myPlayer.setLocation(fireSpawn);
        } else if ("WATER".equals(playerType)) {
            myPlayer.setLocation(waterSpawn);
        }
        
        // Create level data
        List<Point> wallsList = new ArrayList<>(walls);
        currentLevelData = new Level(levelName, gridWidth, gridHeight, 
                                    wallsList, waterSpawn, fireSpawn, door);
        
        // Parse monsters
        parseMonsterData(parts);
        
        setStatus("Level " + currentLevelNumber + " - Di chuyển đến cửa vàng!");
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
    
    
    // Parse spawn point từ level data
     
    private Point parseSpawnPoint(String[] parts, String type) {
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(type) && i + 1 < parts.length) {
                String[] coords = parts[i + 1].split(",");
                if (coords.length == 2) {
                    return new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                }
            }
        }
        return new Point(0, 0); // Default
    }
    
   
     // Parse monster data từ level
     
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
    
    // ==================== GETTERS/SETTERS ====================
    
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
        return new Point(myPlayer);
    }
}
