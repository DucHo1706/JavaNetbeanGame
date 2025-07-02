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

public class GamePanel extends JPanel {
    private int gridWidth = 24;
    private int gridHeight = 16;
    private PrintWriter out;
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
    private boolean showWinAnimation = false;
    private int animationFrame = 0;
    private final Color BACKGROUND_COLOR = new Color(34, 34, 34);
    private final Color GRID_COLOR = new Color(64, 64, 64);
    
    public GamePanel() {
        walls = new HashSet<>();
        firePlayer = new Point(1, 1);
        waterPlayer = new Point(1, 1);
        myPlayer = new Point(1, 1);
        status = "Đang kết nối...";
        
        // Dynamic size - sẽ update khi load level
        updatePanelSize();
        
        setBackground(BACKGROUND_COLOR);
        setFocusable(true);
        
        // Animation timer
        animationTimer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationFrame = (animationFrame + 1) % 8;
                repaint();
            }
        });
        animationTimer.start();
         // Monster movement timer 
    Timer monsterTimer = new Timer(500, e -> {
        if (currentLevelData != null) {
            currentLevelData.updateMonsters();
            checkMonsterCollision();
            repaint();
        }
    });
    monsterTimer.start();
    }
    
    //  cập nhật size panel
    private void updatePanelSize() {
        int newWidth = gridWidth * Constants.CELL_SIZE;
        int newHeight = gridHeight * Constants.CELL_SIZE ;
        
        System.out.println(" Updating panel size to: " + newWidth + "x" + newHeight);
        
        setPreferredSize(new Dimension(newWidth, newHeight));
        setSize(new Dimension(newWidth, newHeight));
        
     
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent.repaint();
            
      
            if (parent instanceof JFrame) {
                ((JFrame) parent).pack();
                System.out.println(" JFrame packed!");
                break;
            }
            parent = parent.getParent();
        }
        
      
        revalidate();
        repaint();
        
        System.out.println("Panel size after update: " + getWidth() + "x" + getHeight());
    }

    //   set network connection
    public void setConnection(PrintWriter out) {
        this.out = out;
    }
    
    public void setPlayerType(String playerType) {
        this.playerType = playerType;
        if ("FIRE".equals(playerType)) {
            myPlayer = firePlayer;
        } else {
            myPlayer = waterPlayer;
        }
        repaint();
    }
    
public void loadLevel(String levelData) {
    System.out.println(" DEBUG: loadLevel called with: " + levelData);
    
    //  khi load level mới
    showWinAnimation = false;
    
    String[] parts = levelData.split(":");
    if (parts.length < 6) return;
    
    System.out.println(" DEBUG: Total parts = " + parts.length);
    for (int i = 0; i < parts.length; i++) {
        System.out.println("  Part[" + i + "] = " + parts[i]);
    }
    
    String levelName = parts[0];
    String[] dimensions = parts[1].split("x");
    gridWidth = Integer.parseInt(dimensions[0]);
    gridHeight = Integer.parseInt(dimensions[1]);
    
    System.out.println(" Grid size: " + gridWidth + "x" + gridHeight);
    updatePanelSize();
    
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
    
    Point fireSpawn = new Point(0, 0);
    Point waterSpawn = new Point(1, 0);
    
    // Tìm WATER: và FIRE: positions
    for (int i = 0; i < parts.length; i++) {
        if (parts[i].equals("WATER") && i + 1 < parts.length) {
            String[] waterCoords = parts[i + 1].split(",");
            if (waterCoords.length == 2) {
                waterSpawn = new Point(Integer.parseInt(waterCoords[0]), Integer.parseInt(waterCoords[1]));
                System.out.println(" Found WATER spawn: " + waterSpawn);
            }
        }
        
        if (parts[i].equals("FIRE") && i + 1 < parts.length) {
            String[] fireCoords = parts[i + 1].split(",");
            if (fireCoords.length == 2) {
                fireSpawn = new Point(Integer.parseInt(fireCoords[0]), Integer.parseInt(fireCoords[1]));
                System.out.println(" Found FIRE spawn: " + fireSpawn);
            }
        }
        
        if (parts[i].equals("DOOR") && i + 1 < parts.length) {
            String[] doorCoords = parts[i + 1].split(",");
            if (doorCoords.length == 2) {
                door = new Point(Integer.parseInt(doorCoords[0]), Integer.parseInt(doorCoords[1]));
                System.out.println(" Found DOOR: " + door);
            }
        }
    }
    
    firePlayer.setLocation(fireSpawn);
    waterPlayer.setLocation(waterSpawn);
    
    if ("FIRE".equals(playerType)) {
        myPlayer.setLocation(fireSpawn);
        System.out.println(" I'm FIRE - set myPlayer to: " + myPlayer);
    } else if ("WATER".equals(playerType)) {
        myPlayer.setLocation(waterSpawn);
        System.out.println(" I'm WATER - set myPlayer to: " + myPlayer);
    }
    
        List<Point> wallsList = new ArrayList<>(walls);
        currentLevelData = new Level(levelName, gridWidth, gridHeight, 
                                    wallsList, waterSpawn, fireSpawn, door);
        
         // Parse monster data nếu có
        parseMonsterData(parts);
     
    
    System.out.println(" Final Fire position: " + firePlayer);
    System.out.println(" Final Water position: " + waterPlayer);
    System.out.println(" Final My player: " + myPlayer + " (type: " + playerType + ")");
    
    setStatus("Level " + currentLevelNumber + " - Di chuyển đến cửa vàng!");
    
    sendPlayerUpdate();
    repaint();
}
 private void parseMonsterData(String[] parts) {
        if (currentLevelData == null) return;
        
        // Tìm phần MONSTERS trong data
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

 // Kiểm tra va chạm với monster
    private void checkMonsterCollision() {
        if (currentLevelData == null || myPlayer == null) return;
        
        Monster monsterAtMyPos = currentLevelData.getMonsterAt(myPlayer.x, myPlayer.y);
        if (monsterAtMyPos != null) {
            // Player chạm monster - game over hoặc reset position
            System.out.println("COLLISION! Player hit monster at " + myPlayer);
            resetPlayerToStart();
        }
    }
    
    // Reset player về vị trí ban đầu
    private void resetPlayerToStart() {
        if (currentLevelData != null) {
            if ("FIRE".equals(playerType)) {
                myPlayer.setLocation(currentLevelData.getFireStart());
                firePlayer.setLocation(currentLevelData.getFireStart());
            } else if ("WATER".equals(playerType)) {
                myPlayer.setLocation(currentLevelData.getWaterStart());
                waterPlayer.setLocation(currentLevelData.getWaterStart());
            }
            
            setStatus("Bạn đã chạm monster! Quay về vị trí ban đầu.");
            sendPlayerUpdate();
            repaint();
        }
    }
    
    //  tìm vị trí 
    private Point findSafePosition(int offset) {
        // Tìm vị trí trống đầu tiên
        for (int y = 0; y < gridHeight; y++) {
            for (int x = offset; x < gridWidth; x++) {
                Point pos = new Point(x, y);
                if (isValidPosition(pos)) {
                    return pos;
                }
            }
        }
        
        // Fallback: góc trên trái
        return new Point(Math.min(offset, gridWidth-1), 0);
    }
    
    //   kiểm tra vị trí hợp lệ
    private boolean isValidPosition(Point pos) {
        return pos.x >= 0 && pos.x < gridWidth && 
               pos.y >= 0 && pos.y < gridHeight && 
               !walls.contains(pos);
    }
    
    //  gửi update 
    private void sendPlayerUpdate() {
        if (myPlayer != null && out != null) {
            try {
                String message = "MOVE:" + myPlayer.x + "," + myPlayer.y;
                out.println(message);
                System.out.println(" Sent: " + message);
            } catch (Exception e) {
                System.err.println("Error sending player update: " + e.getMessage());
            }
        }
    }
    
    public boolean movePlayer(String direction) {
        if (myPlayer == null) return false;
        
        Point newPos = new Point(myPlayer);
        
        switch (direction) {
            case "UP":
                newPos.y--;
                break;
            case "DOWN":
                newPos.y++;
                break;
            case "LEFT":
                newPos.x--;
                break;
            case "RIGHT":
                newPos.x++;
                break;
            default:
                return false;
        }
        
        // Kiểm tra collision
        if (isValidMove(newPos)) {
            myPlayer.setLocation(newPos);
            
       
            if ("FIRE".equals(playerType)) {
                firePlayer.setLocation(newPos);
            } else if ("WATER".equals(playerType)) {
                waterPlayer.setLocation(newPos);
            }
            
       
            checkWinCondition();
            
            // Send update to server
            sendPlayerUpdate();
            
            repaint();
            return true;
        }
          if (isValidMove(newPos)) {
        myPlayer.setLocation(newPos);
        
        if ("FIRE".equals(playerType)) {
            firePlayer.setLocation(newPos);
        } else if ("WATER".equals(playerType)) {
            waterPlayer.setLocation(newPos);
        }
        
        // THÊM DÒNG NÀY
        checkMonsterCollision();
        
        checkWinCondition();
        sendPlayerUpdate();
        repaint();
        return true;
    }
    return false;
    }
    
    //  WIN CHECK
private void checkWinCondition() {
    if (door != null && firePlayer != null && waterPlayer != null) {
        boolean fireAtDoor = firePlayer.equals(door);
        boolean waterAtDoor = waterPlayer.equals(door);
        
        System.out.println(" WIN CHECK:");
        System.out.println("   Fire: " + firePlayer + " == Door: " + door + "? " + fireAtDoor);
        System.out.println("   Water: " + waterPlayer + " == Door: " + door + "? " + waterAtDoor);
        
        if (fireAtDoor && waterAtDoor) {
            System.out.println(" LEVEL COMPLETE! Both players at door!");
            setStatus(" Hoàn thành Level " + currentLevelNumber + "! Chuyển level sau 3 giây...");
            showWinAnimation = true;
            
            // gửi đến server
            if (out != null) {
                out.println("LEVEL_COMPLETE");
                System.out.println(" Sent LEVEL_COMPLETE to server");
            }
        }
    }
}

    
    private boolean isValidMove(Point pos) {
        return isValidPosition(pos);
    }
    
public void updateOtherPlayer(String playerId, int x, int y, String direction) {
    System.out.println(" updateOtherPlayer: " + playerId + " to (" + x + "," + y + ")");
    
    if ("FIRE".equals(playerType)) {
        // I'm fire, so update water player
        waterPlayer.setLocation(x, y);
        System.out.println(" Updated water player to: " + waterPlayer);
    } else {
        // I'm water, so update fire player
        firePlayer.setLocation(x, y);
        System.out.println(" Updated fire player to: " + firePlayer);
    }
    otherPlayer = new Point(x, y);
    checkWinCondition();
    
    repaint();
}

    
    public Point getPlayerPosition() {
        return new Point(myPlayer);
    }
    
    public boolean isLevelComplete() {
        return firePlayer.equals(door) && waterPlayer.equals(door);
    }
    
    public void setStatus(String status) {
        this.status = status;
        
        // Check for win animation
        if (status.contains("Hoàn thành") || status.contains("Chúc mừng")) {
            showWinAnimation = true;
        } else {
            showWinAnimation = false;
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        

        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));
        for (int x = 0; x <= gridWidth; x++) {
            g2d.drawLine(x * Constants.CELL_SIZE, 0, x * Constants.CELL_SIZE, gridHeight * Constants.CELL_SIZE);
        }
        for (int y = 0; y <= gridHeight; y++) {
            g2d.drawLine(0, y * Constants.CELL_SIZE, gridWidth * Constants.CELL_SIZE, y * Constants.CELL_SIZE);
        }
        
                // Draw fantasy stone walls
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

          for (Point wall : walls) {
              int x = wall.x * Constants.CELL_SIZE;
              int y = wall.y * Constants.CELL_SIZE;

              // === STONE WALL DESIGN ===

              // 1. Base stone color với gradient
              GradientPaint stoneGradient = new GradientPaint(
                  x, y, new Color(105, 105, 105),           // Dim gray
                  x + Constants.CELL_SIZE, y + Constants.CELL_SIZE, new Color(169, 169, 169)  // Dark gray
              );
              g2d.setPaint(stoneGradient);
              g2d.fillRoundRect(x + 1, y + 1, Constants.CELL_SIZE - 2, Constants.CELL_SIZE - 2, 4, 4);

              // 2. Stone texture - các khối đá nhỏ
              g2d.setColor(new Color(128, 128, 128)); // Gray

              // Vẽ các đường nứt tạo texture đá
              g2d.setStroke(new BasicStroke(1));

              // Horizontal cracks
              g2d.drawLine(x + 3, y + Constants.CELL_SIZE / 3, x + Constants.CELL_SIZE - 3, y + Constants.CELL_SIZE / 3);
              g2d.drawLine(x + 5, y + 2 * Constants.CELL_SIZE / 3, x + Constants.CELL_SIZE - 2, y + 2 * Constants.CELL_SIZE / 3);

              // Vertical cracks
              g2d.drawLine(x + Constants.CELL_SIZE / 2, y + 2, x + Constants.CELL_SIZE / 2, y + Constants.CELL_SIZE / 3);
              g2d.drawLine(x + 2 * Constants.CELL_SIZE / 3, y + Constants.CELL_SIZE / 3, x + 2 * Constants.CELL_SIZE / 3, y + Constants.CELL_SIZE - 2);

              // 3. Moss and aging effects
              g2d.setColor(new Color(34, 139, 34, 80)); // Forest green, semi-transparent

              // Random moss spots
              Random random = new Random(wall.x * 1000 + wall.y); // Consistent random based on position
              for (int i = 0; i < 3; i++) {
                  int mossX = x + 2 + random.nextInt(Constants.CELL_SIZE - 6);
                  int mossY = y + 2 + random.nextInt(Constants.CELL_SIZE - 6);
                  g2d.fillOval(mossX, mossY, 3 + random.nextInt(3), 2 + random.nextInt(2));
              }

              // 4. 3D lighting effect
              g2d.setStroke(new BasicStroke(2));

              // Top highlight (light source from top-left)
              g2d.setColor(new Color(220, 220, 220, 150)); // Light gray, semi-transparent
              g2d.drawLine(x + 1, y + 1, x + Constants.CELL_SIZE - 1, y + 1); // Top edge
              g2d.drawLine(x + 1, y + 1, x + 1, y + Constants.CELL_SIZE - 1); // Left edge

              // Bottom shadow
              g2d.setColor(new Color(64, 64, 64, 150)); // Dark gray, semi-transparent
              g2d.drawLine(x + 1, y + Constants.CELL_SIZE - 1, x + Constants.CELL_SIZE - 1, y + Constants.CELL_SIZE - 1); // Bottom edge
              g2d.drawLine(x + Constants.CELL_SIZE - 1, y + 1, x + Constants.CELL_SIZE - 1, y + Constants.CELL_SIZE - 1); // Right edge

              // 5. Stone block definition
              g2d.setColor(new Color(90, 90, 90)); // Darker gray for definition
              g2d.setStroke(new BasicStroke(1));
              g2d.drawRoundRect(x + 1, y + 1, Constants.CELL_SIZE - 2, Constants.CELL_SIZE - 2, 4, 4);

              // 6. Ancient runes (occasionally)
              if (random.nextInt(10) == 0) { // 10% chance for runes
                  g2d.setColor(new Color(138, 43, 226, 100)); // Purple, semi-transparent
                  g2d.setFont(new Font("Serif", Font.BOLD, 8));

                  String[] runes = {"◊", "※", "⚡", "✦", "◈", "⬟"};
                  String rune = runes[random.nextInt(runes.length)];

                  FontMetrics fm = g2d.getFontMetrics();
                  int runeX = x + (Constants.CELL_SIZE - fm.stringWidth(rune)) / 2;
                  int runeY = y + (Constants.CELL_SIZE + fm.getAscent()) / 2;

                  g2d.drawString(rune, runeX, runeY);
              }
          }

        
            // Fantasy RPG door
        if (door != null) {
            int x = door.x * Constants.CELL_SIZE;
            int y = door.y * Constants.CELL_SIZE;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Stone door frame
            g2d.setColor(new Color(105, 105, 105)); // Dim gray
            g2d.fillRoundRect(x, y, Constants.CELL_SIZE, Constants.CELL_SIZE, 4, 4);

            // 2. Wooden door
            GradientPaint woodGradient = new GradientPaint(
                x, y, new Color(139, 69, 19),
                x + Constants.CELL_SIZE, y, new Color(160, 82, 45)
            );
            g2d.setPaint(woodGradient);
            g2d.fillRoundRect(x + 2, y + 2, Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4, 8, 8);

            // 3. Iron reinforcements
            g2d.setColor(new Color(64, 64, 64)); // Dark gray
            g2d.setStroke(new BasicStroke(2));

            // Vertical iron bands
            g2d.drawLine(x + 6, y + 4, x + 6, y + Constants.CELL_SIZE - 4);
            g2d.drawLine(x + Constants.CELL_SIZE - 6, y + 4, x + Constants.CELL_SIZE - 6, y + Constants.CELL_SIZE - 4);

            // Horizontal iron bands
            g2d.drawLine(x + 4, y + 8, x + Constants.CELL_SIZE - 4, y + 8);
            g2d.drawLine(x + 4, y + Constants.CELL_SIZE - 8, x + Constants.CELL_SIZE - 4, y + Constants.CELL_SIZE - 8);

            // 4. Ancient lock
            int lockX = x + Constants.CELL_SIZE / 2 - 4;
            int lockY = y + Constants.CELL_SIZE / 2 - 4;

            // Lock body
            g2d.setColor(new Color(184, 134, 11)); // Dark goldenrod
            g2d.fillRoundRect(lockX, lockY + 2, 8, 6, 2, 2);

            // Lock shackle
            g2d.setStroke(new BasicStroke(2));
            g2d.drawArc(lockX + 1, lockY - 1, 6, 6, 0, 180);

            // 5. Mystical runes
            g2d.setColor(new Color(138, 43, 226, 150)); // Blue violet, semi-transparent
            g2d.setFont(new Font("Serif", Font.BOLD, 8));
            g2d.drawString("◊", x + 4, y + 6);
            g2d.drawString("※", x + Constants.CELL_SIZE - 10, y + 6);
            g2d.drawString("⚡", x + 4, y + Constants.CELL_SIZE - 2);
            g2d.drawString("✦", x + Constants.CELL_SIZE - 10, y + Constants.CELL_SIZE - 2);


        }
        // Draw monsters 
        if (currentLevelData != null && currentLevelData.getMonsters() != null) {
            for (Monster monster : currentLevelData.getMonsters()) {
                drawMonster(g2d, monster);
            }
        }

        // Draw fire player with animation
        drawPlayer(g2d, firePlayer, true);
        
        // Draw water player with animation
        drawPlayer(g2d, waterPlayer, false);
        
         drawElementalInteraction(g2d, firePlayer, waterPlayer);
        
        
        // Highlight my player
        if (myPlayer != null && playerType != null) {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(myPlayer.x * Constants.CELL_SIZE + 2, myPlayer.y * Constants.CELL_SIZE + 2, 
                         Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4);
        }
        
        // Draw status bar
        drawStatusBar(g2d);
        
        // Draw win animation
        if (showWinAnimation) {
            drawWinAnimation(g2d);
        }
    }
    

private void drawPlayer(Graphics2D g2d, Point player, boolean isFire) {
    if (player == null) return;
    
    int x = player.x * Constants.CELL_SIZE;
    int y = player.y * Constants.CELL_SIZE;
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    if (isFire) {
        // === 🔥 FIRE BALL KAWAII VERSION ===
        
        long time = System.currentTimeMillis();
        double bounce = Math.sin(time * 0.008) * 1.5;
        double flameFlicker = Math.sin(time * 0.015) * 0.3 + 0.7;
        
        // 1. 🌟 Cute fire aura
        int auraSize = (int)(16 + flameFlicker * 4);
        RadialGradientPaint fireAura = new RadialGradientPaint(
            x + Constants.CELL_SIZE / 2, y + Constants.CELL_SIZE / 2, auraSize,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{
                new Color(255, 180, 120, 100),
                new Color(255, 140, 100, 60),
                new Color(255, 220, 150, 30)
            }
        );
        g2d.setPaint(fireAura);
        g2d.fillOval(x + Constants.CELL_SIZE/2 - auraSize, y + Constants.CELL_SIZE/2 - auraSize, 
                     auraSize * 2, auraSize * 2);
        
        // 2. 🥰 Cute shadow
        g2d.setColor(new Color(200, 150, 100, 80));
        g2d.fillOval(x + 8, y + Constants.CELL_SIZE - 6, Constants.CELL_SIZE - 16, 6);
        
        // 3. 🔥 Main fire ball - TRÒN HOÀN TOÀN
        int ballSize = Constants.CELL_SIZE - 8;
        RadialGradientPaint ballGradient = new RadialGradientPaint(
            x + Constants.CELL_SIZE / 2, (float)(y + Constants.CELL_SIZE / 2 + bounce),
            ballSize / 2,
            new float[]{0.0f, 0.6f, 1.0f},
            new Color[]{
                new Color(255, 220, 150),  // Vàng nhạt ở giữa
                new Color(255, 160, 120),  // Cam
                new Color(255, 120, 80)    // Đỏ cam ở ngoài
            }
        );
        g2d.setPaint(ballGradient);
        g2d.fillOval(x + 4, y + 4 + (int)bounce, ballSize, ballSize);
        
        // 4. 👀 Big kawaii eyes trên ball
        // Mắt trắng to
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 8, y + 10 + (int)bounce, 8, 10);
        g2d.fillOval(x + Constants.CELL_SIZE - 16, y + 10 + (int)bounce, 8, 10);
        
        // Đồng tử đen
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillOval(x + 9, y + 12 + (int)bounce, 6, 6);
        g2d.fillOval(x + Constants.CELL_SIZE - 15, y + 12 + (int)bounce, 6, 6);
        
        // ✨ Sparkle trong mắt
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 10, y + 13 + (int)bounce, 3, 3);
        g2d.fillOval(x + Constants.CELL_SIZE - 14, y + 13 + (int)bounce, 3, 3);
        g2d.fillOval(x + 12, y + 15 + (int)bounce, 1, 1);
        g2d.fillOval(x + Constants.CELL_SIZE - 12, y + 15 + (int)bounce, 1, 1);
        
        // 5. 😊 Cute smile
        g2d.setColor(new Color(200, 100, 80));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawArc(x + 10, y + 20 + (int)bounce, 12, 8, 0, -180);
        
        // Má hồng
        g2d.setColor(new Color(255, 150, 150, 120));
        g2d.fillOval(x + 6, y + 18 + (int)bounce, 6, 4);
        g2d.fillOval(x + Constants.CELL_SIZE - 12, y + 18 + (int)bounce, 6, 4);
        
        // 6. 🔥 Cute flame hair trên ball
        for (int i = 0; i < 8; i++) {
            double flameTime = time * 0.01 + i * 0.6;
            double flameHeight = 6 + Math.sin(flameTime) * 3;
            double angle = (i * Math.PI * 2 / 8) + Math.sin(flameTime * 0.5) * 0.2;
            
            // Tính vị trí flame xung quanh ball
            double flameX = x + Constants.CELL_SIZE/2 + Math.cos(angle) * (ballSize/2 + 2);
            double flameY = y + Constants.CELL_SIZE/2 + (int)bounce + Math.sin(angle) * (ballSize/2 + 2) - flameHeight;
            
            Color flameColor;
            if (i % 3 == 0) flameColor = new Color(255, 220, 100, 200);
            else if (i % 3 == 1) flameColor = new Color(255, 180, 120, 200);
            else flameColor = new Color(255, 150, 100, 200);
            
            g2d.setColor(flameColor);
            // Flame giọt nước cute
            g2d.fillOval((int)flameX - 2, (int)flameY, 4, (int)flameHeight);
            g2d.fillOval((int)flameX - 1, (int)flameY - 2, 2, 3);
        }
        
        // 8. 🎀 Cute bow on top
        g2d.setColor(new Color(255, 100, 100));
        g2d.fillOval(x + Constants.CELL_SIZE/2 - 4, y + 2 + (int)bounce, 8, 5);
        g2d.setColor(new Color(200, 80, 80));
        g2d.fillOval(x + Constants.CELL_SIZE/2 - 2, y + 3 + (int)bounce, 4, 3);
        
    } else {
        // === 💧 WATER BALL KAWAII VERSION ===
        
        long time = System.currentTimeMillis();
        double flow = Math.sin(time * 0.006) * 1.2;
        double ripple = Math.sin(time * 0.012) * 0.2 + 0.8;
        
        // 1. 💫 Cute water aura
        int auraSize = (int)(16 + ripple * 4);
        RadialGradientPaint waterAura = new RadialGradientPaint(
            x + Constants.CELL_SIZE / 2, y + Constants.CELL_SIZE / 2, auraSize,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{
                new Color(150, 220, 255, 90),
                new Color(120, 200, 255, 55),
                new Color(200, 240, 255, 25)
            }
        );
        g2d.setPaint(waterAura);
        g2d.fillOval(x + Constants.CELL_SIZE/2 - auraSize, y + Constants.CELL_SIZE/2 - auraSize,
                     auraSize * 2, auraSize * 2);
        
        // 2. 🥰 Cute reflection
        g2d.setColor(new Color(100, 180, 220, 100));
        g2d.fillOval(x + 8, y + Constants.CELL_SIZE - 6, Constants.CELL_SIZE - 16, 6);
        
        // 3. 💧 Main water ball - TRÒN HOÀN TOÀN
        int ballSize = Constants.CELL_SIZE - 8;
        RadialGradientPaint ballGradient = new RadialGradientPaint(
            x + Constants.CELL_SIZE / 2, (float)(y + Constants.CELL_SIZE / 2 + flow),
            ballSize / 2,
            new float[]{0.0f, 0.6f, 1.0f},
            new Color[]{
                new Color(200, 240, 255),  // Xanh nhạt ở giữa
                new Color(150, 200, 255),  // Xanh
                new Color(120, 170, 230)   // Xanh đậm ở ngoài
            }
        );
        g2d.setPaint(ballGradient);
        g2d.fillOval(x + 4, y + 4 + (int)flow, ballSize, ballSize);
        
        // 4. 👀 Big kawaii eyes - xanh
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 8, y + 10 + (int)flow, 8, 10);
        g2d.fillOval(x + Constants.CELL_SIZE - 16, y + 10 + (int)flow, 8, 10);
        
        // Đồng tử xanh
        g2d.setColor(new Color(50, 120, 200));
        g2d.fillOval(x + 9, y + 12 + (int)flow, 6, 6);
        g2d.fillOval(x + Constants.CELL_SIZE - 15, y + 12 + (int)flow, 6, 6);
        
        // ✨ Sparkle
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 10, y + 13 + (int)flow, 3, 3);
        g2d.fillOval(x + Constants.CELL_SIZE - 14, y + 13 + (int)flow, 3, 3);
        g2d.fillOval(x + 12, y + 15 + (int)flow, 1, 1);
        g2d.fillOval(x + Constants.CELL_SIZE - 12, y + 15 + (int)flow, 1, 1);
        
        // 5. 😊 Cute smile
        g2d.setColor(new Color(100, 150, 200));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawArc(x + 10, y + 20 + (int)flow, 12, 8, 0, -180);
        
        // Má hồng xanh
        g2d.setColor(new Color(150, 200, 255, 120));
        g2d.fillOval(x + 6, y + 18 + (int)flow, 6, 4);
        g2d.fillOval(x + Constants.CELL_SIZE - 12, y + 18 + (int)flow, 6, 4);
        
        // 6. 💧 Cute water drops xung quanh ball
        for (int i = 0; i < 6; i++) {
            double dropTime = time * 0.008 + i * 1.0;
            double dropHeight = 5 + Math.sin(dropTime) * 2;
            double angle = (i * Math.PI * 2 / 6) + Math.sin(dropTime * 0.3) * 0.3;
            
            double dropX = x + Constants.CELL_SIZE/2 + Math.cos(angle) * (ballSize/2 + 3);
            double dropY = y + Constants.CELL_SIZE/2 + (int)flow + Math.sin(angle) * (ballSize/2 + 3) - dropHeight;
            
            Color dropColor;
            if (i % 2 == 0) dropColor = new Color(150, 220, 255, 180);
            else dropColor = new Color(120, 200, 255, 180);
            
            g2d.setColor(dropColor);
            g2d.fillOval((int)dropX - 2, (int)dropY, 4, (int)dropHeight);
            g2d.fillOval((int)dropX - 1, (int)dropY - 1, 2, 2);
        }
        
        // 7. 💙 Floating bubbles
        for (int i = 0; i < 8; i++) {
            double angle = (time * 0.003 + i * Math.PI * 2 / 8) % (Math.PI * 2);
            int bubbleX = (int)(x + Constants.CELL_SIZE/2 + Math.cos(angle) * (18 + ripple * 4));
            int bubbleY = (int)(y + Constants.CELL_SIZE/2 + Math.sin(angle) * (12 + ripple * 3));
            
            int bubbleSize = 2 + i % 4;
            g2d.setColor(new Color(180, 220, 255, 160));
            g2d.fillOval(bubbleX - bubbleSize/2, bubbleY - bubbleSize/2, bubbleSize, bubbleSize);
            
            // Bubble highlight
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillOval(bubbleX - bubbleSize/4, bubbleY - bubbleSize/4, bubbleSize/2, bubbleSize/2);
        }
        
        // 8. 🌸 Cute flower on top
        g2d.setColor(new Color(100, 150, 255));
        for (int petal = 0; petal < 6; petal++) {
            double petalAngle = petal * Math.PI * 2 / 6;
            int petalX = (int)(x + Constants.CELL_SIZE/2 + Math.cos(petalAngle) * 3);
            int petalY = (int)(y + 2 + (int)flow + Math.sin(petalAngle) * 3);
            g2d.fillOval(petalX - 2, petalY - 2, 4, 4);
        }
        // Nhụy hoa
        g2d.setColor(new Color(255, 255, 150));
        g2d.fillOval(x + Constants.CELL_SIZE/2 - 2, y + 2 + (int)flow - 2, 4, 4);
    }
}

private void drawElementalInteraction(Graphics2D g2d, Point firePlayer, Point waterPlayer) {
    if (firePlayer == null || waterPlayer == null) return;
    
    // Tính khoảng cách giữa 2 nhân vật (theo grid)
    double distance = Math.sqrt(Math.pow(firePlayer.x - waterPlayer.x, 2) + 
                               Math.pow(firePlayer.y - waterPlayer.y, 2));
    
    // Chỉ tạo hiệu ứng khi gần nhau
    if (distance <= 3.0) { // Trong vòng 3 ô
        
        long time = System.currentTimeMillis();
        
        // Tọa độ pixel của 2 nhân vật
        int fireX = firePlayer.x * Constants.CELL_SIZE + Constants.CELL_SIZE / 2;
        int fireY = firePlayer.y * Constants.CELL_SIZE + Constants.CELL_SIZE / 2;
        int waterX = waterPlayer.x * Constants.CELL_SIZE + Constants.CELL_SIZE / 2;
        int waterY = waterPlayer.y * Constants.CELL_SIZE + Constants.CELL_SIZE / 2;
        
        // Điểm giữa 2 nhân vật
        int midX = (fireX + waterX) / 2;
        int midY = (fireY + waterY) / 2;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // === HIỆU ỨNG STEAM (HơI NƯỚC) ===
        if (distance <= 2.5) {
            drawSteamEffect(g2d, midX, midY, time);
        }
    }
}

private void drawSteamEffect(Graphics2D g2d, int centerX, int centerY, long time) {
    // Tạo các đám hơi nước bay lên
    for (int i = 0; i < 8; i++) {
        double steamTime = time * 0.008 + i * 0.5;
        double steamY = centerY - 15 - i * 6 + Math.sin(steamTime) * 4;
        double steamX = centerX + Math.sin(steamTime * 0.7) * (3 + i);
        
        // Kích thước hơi nước tăng dần khi bay lên
        int steamSize = 2 + i / 2;
        
        // Màu hơi nước với độ trong suốt giảm dần
        int alpha = Math.max(20, 180 - i * 20);
        g2d.setColor(new Color(255, 255, 255, alpha));
        
        g2d.fillOval((int)steamX - steamSize/2, (int)steamY - steamSize/2, 
                     steamSize, steamSize);
        
        // Thêm highlight cho hơi nước
        if (i < 4) {
            g2d.setColor(new Color(200, 230, 255, alpha/2));
            g2d.fillOval((int)steamX - steamSize/4, (int)steamY - steamSize/4, 
                         steamSize/2, steamSize/2);
        }
    }
}


    
    private void drawStatusBar(Graphics2D g2d) {
        int statusY = gridHeight * Constants.CELL_SIZE + 10;
        
        // Background
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(0, statusY - 5, getWidth(), 70);
        
        // Status text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(status, 10, statusY + 15);
        
        // Player info
        if (playerType != null) {
            String playerInfo = "Bạn là: " + (playerType.equals("FIRE") ? " Fire Player" : " Water Player");
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString(playerInfo, 10, statusY + 35);
        }
        
        // Controls
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Điều khiển: WASD hoặc ← ↑ ↓ →", 10, statusY + 55);
        
        // Level indicator
        if (currentLevelNumber > 0) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Level: " + currentLevelNumber + "/" + Constants.TOTAL_LEVELS, 
                          getWidth() - 100, statusY + 15);
        }
    }
    
    private void drawWinAnimation(Graphics2D g2d) {
        // Fireworks effect
        g2d.setColor(new Color(255, 255, 0, 100 + (int)(Math.sin(animationFrame) * 50)));
        for (int i = 0; i < 10; i++) {
            int x = (int)(Math.random() * getWidth());
            int y = (int)(Math.random() * gridHeight * Constants.CELL_SIZE);
            int size = 5 + (int)(Math.random() * 10);
            g2d.fillOval(x, y, size, size);
        }
        
        // Congratulations text
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        String text = " LEVEL COMPLETE! ";
        int textX = (getWidth() - fm.stringWidth(text)) / 2;
        int textY = gridHeight * Constants.CELL_SIZE / 2;
        
        // Text shadow
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, textX + 2, textY + 2);
        
        // Main text
        g2d.setColor(Color.YELLOW);
        g2d.drawString(text, textX, textY);
    }
    
    public void setCurrentLevel(int level) {
        this.currentLevelNumber = level;
        repaint();
    }
    private void drawMonster(Graphics2D g2d, Monster monster) {
    int x = monster.getX() * Constants.CELL_SIZE;
    int y = monster.getY() * Constants.CELL_SIZE;
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    if ("TINH".equals(monster.getType())) {
        // Monster tĩnh - đỏ với mắt đỏ
        int alpha = 200 + (int)(55 * Math.sin(animationFrame * 0.5));
        g2d.setColor(new Color(139, 0, 0, alpha));
        g2d.fillOval(x + 3, y + 3, Constants.CELL_SIZE - 6, Constants.CELL_SIZE - 6);
        
        // Eyes
        g2d.setColor(Color.RED);
        g2d.fillOval(x + 8, y + 8, 4, 4);
        g2d.fillOval(x + Constants.CELL_SIZE - 12, y + 8, 4, 4);
        
    } else if ("LENXUONG".equals(monster.getType())) {
        // Monster lên xuống - xanh lá
        g2d.setColor(new Color(0, 128, 0));
        g2d.fillRect(x + 2, y + 2, Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4);
        drawArrow(g2d, x, y, monster.getDirection());
        
    } else if ("TRAIPHAI".equals(monster.getType())) {
        // Monster trái phải - xanh dương
        g2d.setColor(new Color(0, 0, 139));
        g2d.fillRect(x + 2, y + 2, Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4);
        drawArrow(g2d, x, y, monster.getDirection());
    }
    
    // Border cho tất cả monster
    g2d.setColor(Color.BLACK);
    g2d.setStroke(new BasicStroke(2));
    if ("TINH".equals(monster.getType())) {
        g2d.drawOval(x + 3, y + 3, Constants.CELL_SIZE - 6, Constants.CELL_SIZE - 6);
    } else {
        g2d.drawRect(x + 2, y + 2, Constants.CELL_SIZE - 4, Constants.CELL_SIZE - 4);
    }
}

    private void drawArrow(Graphics2D g2d, int x, int y, String direction) {
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));

        int centerX = x + Constants.CELL_SIZE / 2;
        int centerY = y + Constants.CELL_SIZE / 2;

        switch (direction) {
            case "UP":
                g2d.drawLine(centerX, centerY + 6, centerX, centerY - 6);
                g2d.drawLine(centerX, centerY - 6, centerX - 3, centerY - 3);
                g2d.drawLine(centerX, centerY - 6, centerX + 3, centerY - 3);
                break;
            case "DOWN":
                g2d.drawLine(centerX, centerY - 6, centerX, centerY + 6);
                g2d.drawLine(centerX, centerY + 6, centerX - 3, centerY + 3);
                g2d.drawLine(centerX, centerY + 6, centerX + 3, centerY + 3);
                break;
            case "LEFT":
                g2d.drawLine(centerX + 6, centerY, centerX - 6, centerY);
                g2d.drawLine(centerX - 6, centerY, centerX - 3, centerY - 3);
                g2d.drawLine(centerX - 6, centerY, centerX - 3, centerY + 3);
                break;
            case "RIGHT":
                g2d.drawLine(centerX - 6, centerY, centerX + 6, centerY);
                g2d.drawLine(centerX + 6, centerY, centerX + 3, centerY - 3);
                g2d.drawLine(centerX + 6, centerY, centerX + 3, centerY + 3);
                break;
        }
    }
   
}
