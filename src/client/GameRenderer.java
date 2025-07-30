package client;

import game.Item;
import utils.Constants;
import game.Monster;
import server.Level;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * Class chuyên vẽ các thành phần của game
 */
public class GameRenderer {
    private final Color GRID_COLOR = new Color(64, 64, 64);
    private final Color STATUS_COLOR = Color.WHITE;
    
    private int animationFrame = 0;
    private BufferedImage backgroundImage;
    private BufferedImage doorImage;
    private Map<String, BufferedImage> monsterImages = new HashMap<>();
    private Map<String, BufferedImage> playerImages = new HashMap<>();
    private BufferedImage wallImage;


    public GameRenderer() {
            loadBackgroundImage();
            loadDoorImage();
            loadMonsterImages();
             loadPlayerImages(); 
             loadWallImage();
             loadItemImages();
    }
    private BufferedImage coinImage;
private BufferedImage gemImage;
private BufferedImage chestImage;
private BufferedImage keyImage;

private void loadItemImages() {
    coinImage = loadImage("/images/Coin.png");
    gemImage = loadImage("/images/GEM.png");
    chestImage = loadImage("/images/CHEST.png");
   
}

private BufferedImage loadImage(String path) {
    try (InputStream is = getClass().getResourceAsStream(path)) {
        if (is != null) {
            return ImageIO.read(is);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return null;
}

private void drawItems(Graphics2D g2d, Level level) {
    if (level == null) return;
    for (Item item : level.getItems()) {
        if (!item.isCollected()) {
            drawItem(g2d, item);
        }
    }
}

private void drawItem(Graphics2D g2d, Item item) {
    Point pos = item.getPosition();
    int x = pos.x * Constants.CELL_SIZE;
    int y = pos.y * Constants.CELL_SIZE;
    BufferedImage img = null;
    switch (item.getType()) {
        case Constants.ITEM_COIN:
            img = coinImage; break;
        case Constants.ITEM_GEM:
            img = gemImage; break;
        case Constants.ITEM_CHEST:
            img = chestImage; break;
      
    }
    if (img != null) {
        g2d.drawImage(img, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE, null);
    } else {
        // Vẽ hình tròn làm đại diện nếu không có ảnh
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x + 4, y + 4, Constants.CELL_SIZE - 8, Constants.CELL_SIZE - 8);
    }
}
    public void render(Graphics g, GamePanel panel) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Vẽ background
        drawBackground(g2d, panel);
        
        // Vẽ grid
        drawGrid(g2d, panel);
        
        // Vẽ walls
        drawWalls(g2d, panel.getWalls());
        drawItems(g2d, panel.getCurrentLevelData());
        // Vẽ door
        drawDoor(g2d, panel.getDoor());
        
        // Vẽ monsters
        drawMonsters(g2d, panel.getCurrentLevelData());
        
        // Vẽ players
        drawPlayers(g2d, panel);
        
        // Vẽ status
        drawStatus(g2d, panel);
            
     
        
        // Vẽ win animation nếu có
        if (panel.isShowWinAnimation()) {
            drawWinAnimation(g2d, panel);
        }
    }


  private void drawBackground(Graphics2D g2d, GamePanel panel) {
    if (backgroundImage != null) {
        g2d.drawImage(backgroundImage, 0, 0, panel.getWidth(), panel.getHeight(), null);
    } else {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, panel.getWidth(), panel.getHeight());
    }
}
  
private void loadBackgroundImage() {
   try {
        InputStream is = getClass().getResourceAsStream("/images/brne.png");
        //   InputStream is = getClass().getResourceAsStream("/images/DUCDEPTRAI.png");
        if (is != null) {
            backgroundImage = ImageIO.read(is);
            System.out.println("Load ảnh background thành công!");
        } else {
            System.err.println("Không tìm thấy ảnh background trong resources!");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
  private void loadMonsterImages() {
        loadMonsterImage("TINH", "/images/Slime_(Dragon_Quest).png");
        loadMonsterImage("LENXUONG", "/images/Titan.png");
        loadMonsterImage("TRAIPHAI", "/images/Titan2.png");
    }

 private void loadMonsterImage(String type, String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                monsterImages.put(type.toUpperCase(), img);
                System.out.println("Load ảnh quái vật " + type + " thành công!");
            } else {
                System.err.println("Không tìm thấy ảnh quái vật " + type + " tại " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    
    /**
     * Vẽ lưới
     */
    private void drawGrid(Graphics2D g2d, GamePanel panel) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1));
        
        // Vẽ đường dọc
        for (int x = 0; x <= panel.getGridWidth(); x++) {
            int xPos = x * Constants.CELL_SIZE;
            g2d.drawLine(xPos, 0, xPos, panel.getGridHeight() * Constants.CELL_SIZE);
        }
        
        // Vẽ đường ngang
        for (int y = 0; y <= panel.getGridHeight(); y++) {
            int yPos = y * Constants.CELL_SIZE;
            g2d.drawLine(0, yPos, panel.getGridWidth() * Constants.CELL_SIZE, yPos);
        }
    }
    private void loadWallImage() {
    try (InputStream is = getClass().getResourceAsStream("/images/wall3.png")) {
        if (is != null) {
            wallImage = ImageIO.read(is);
            System.out.println("Load ảnh tường thành công!");
        } else {
            System.err.println("Không tìm thấy ảnh tường trong resources!");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    /**
     * Vẽ tường
     */
  private void drawWalls(Graphics2D g2d, Set<Point> walls) {
    if (wallImage != null) {
        for (Point wall : walls) {
            int x = wall.x * Constants.CELL_SIZE;
            int y = wall.y * Constants.CELL_SIZE;
            // Vẽ ảnh tường vừa ô lưới
            g2d.drawImage(wallImage, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE, null);
        }
    } else {
        // Nếu chưa load được ảnh, fallback vẽ màu như cũ
        for (Point wall : walls) {
            int x = wall.x * Constants.CELL_SIZE;
            int y = wall.y * Constants.CELL_SIZE;

            g2d.setColor(new Color(101, 67, 33)); // Nâu đá
            g2d.fillRect(x + 1, y + 1, Constants.CELL_SIZE - 2, Constants.CELL_SIZE - 2);

            g2d.setColor(new Color(140, 100, 60)); // Nâu sáng
            g2d.fillRect(x + 2, y + 2, Constants.CELL_SIZE - 6, 2);
            g2d.fillRect(x + 2, y + 2, 2, Constants.CELL_SIZE - 6);

            g2d.setColor(new Color(60, 40, 20)); // Nâu tối
            g2d.fillRect(x + Constants.CELL_SIZE - 3, y + 3, 2, Constants.CELL_SIZE - 3);
            g2d.fillRect(x + 3, y + Constants.CELL_SIZE - 3, Constants.CELL_SIZE - 3, 2);

            g2d.setColor(new Color(40, 25, 15)); // Nâu đen
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(x + 1, y + 1, Constants.CELL_SIZE - 2, Constants.CELL_SIZE - 2);
        }
    }
}


    private void loadDoorImage() {
    try {
        InputStream is = getClass().getResourceAsStream("/images/9530132de98ea26.png");
        if (is != null) {
            doorImage = ImageIO.read(is);
            System.out.println("Load ảnh cửa thành công!");
        } else {
            System.err.println("Không tìm thấy ảnh cửa trong resources!");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
    /**
     * Vẽ cửa
     */
   private void drawDoor(Graphics2D g2d, Point door) {
    if (door == null) return;

    int x = door.x * Constants.CELL_SIZE;
    int y = door.y * Constants.CELL_SIZE;

    if (doorImage != null) {
        // Vẽ ảnh cửa đúng vị trí, kích thước ô lưới
        g2d.drawImage(doorImage, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE, null);
    } else {
        // Nếu chưa load được ảnh, fallback về vẽ thủ công
        drawMagicalAura(g2d, x, y);
        drawMainDoor(g2d, x, y);
        drawMagicalSymbols(g2d, x, y);
        drawCenterGem(g2d, x, y);
    }
}

/**
 * 🌟 Vẽ hào quang ma thuật
 */
private void drawMagicalAura(Graphics2D g2d, int x, int y) {
    // Hào quang ngoài - xoay chậm
    float rotation = animationFrame * 0.02f;
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Lưu transform cũ
    AffineTransform oldTransform = g2d.getTransform();
    g2d.rotate(rotation, centerX, centerY);
    
    // Vẽ hào quang gradient
    RadialGradientPaint aura = new RadialGradientPaint(
        centerX, centerY, Constants.CELL_SIZE,
        new float[]{0f, 0.6f, 1f},
        new Color[]{
            new Color(255, 215, 0, 60),   // Vàng trong
            new Color(255, 165, 0, 30),   // Cam giữa  
            new Color(255, 69, 0, 0)      // Đỏ ngoài trong suốt
        }
    );
    g2d.setPaint(aura);
    g2d.fillOval(x - 5, y - 5, Constants.CELL_SIZE + 10, Constants.CELL_SIZE + 10);
    
    g2d.setTransform(oldTransform); // Khôi phục
}

/**
 * 🚪 Vẽ cửa chính
 */
private void drawMainDoor(Graphics2D g2d, int x, int y) {
    // Nền cửa - màu nâu gỗ cổ
    g2d.setColor(new Color(139, 69, 19)); // Nâu gỗ
    g2d.fillRoundRect(x + 3, y + 3, Constants.CELL_SIZE - 6, Constants.CELL_SIZE - 6, 8, 8);
    
    // Viền cửa vàng kim loại
    g2d.setColor(new Color(218, 165, 32)); // Vàng kim loại
    g2d.setStroke(new BasicStroke(2));
    g2d.drawRoundRect(x + 3, y + 3, Constants.CELL_SIZE - 6, Constants.CELL_SIZE - 6, 8, 8);
    
    // Các đường gỗ ngang
    g2d.setColor(new Color(101, 67, 33)); // Nâu tối
    g2d.setStroke(new BasicStroke(1));
    for (int i = 1; i <= 3; i++) {
        int lineY = y + (Constants.CELL_SIZE * i / 4);
        g2d.drawLine(x + 6, lineY, x + Constants.CELL_SIZE - 6, lineY);
    }
}

/**
 * ✨ Vẽ ký hiệu ma thuật
 */
private void drawMagicalSymbols(Graphics2D g2d, int x, int y) {
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Hiệu ứng nhấp nháy
    int alpha = 120 + (int)(80 * Math.sin(animationFrame * 0.3));
    g2d.setColor(new Color(255, 215, 0, alpha)); // Vàng lấp lánh
    g2d.setStroke(new BasicStroke(2));
    
    // Vẽ ngôi sao 5 cánh nhỏ ở 4 góc
    int[] starX = {x + 8, x + Constants.CELL_SIZE - 8, x + 8, x + Constants.CELL_SIZE - 8};
    int[] starY = {y + 8, y + 8, y + Constants.CELL_SIZE - 8, y + Constants.CELL_SIZE - 8};
    
    for (int i = 0; i < 4; i++) {
        drawSmallStar(g2d, starX[i], starY[i], 3);
    }
}

/**
 * ⭐ Vẽ ngôi sao nhỏ
 */
private void drawSmallStar(Graphics2D g2d, int centerX, int centerY, int size) {
    int[] xPoints = new int[10];
    int[] yPoints = new int[10];
    
    for (int i = 0; i < 10; i++) {
        double angle = Math.PI * i / 5;
        int radius = (i % 2 == 0) ? size : size / 2;
        xPoints[i] = centerX + (int)(radius * Math.cos(angle - Math.PI/2));
        yPoints[i] = centerY + (int)(radius * Math.sin(angle - Math.PI/2));
    }
    
    g2d.fillPolygon(xPoints, yPoints, 10);
}

/**
 * 💎 Vẽ báu vật ở giữa
 */
private void drawCenterGem(Graphics2D g2d, int x, int y) {
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Hiệu ứng xoay và lấp lánh
    float rotation = animationFrame * 0.05f;
    int alpha = 180 + (int)(75 * Math.sin(animationFrame * 0.4));
    
    AffineTransform oldTransform = g2d.getTransform();
    g2d.rotate(rotation, centerX, centerY);
    
    // Báu vật kim cương
    g2d.setColor(new Color(0, 191, 255, alpha)); // Xanh kim cương
    int gemSize = 6;
    int[] gemX = {centerX, centerX + gemSize, centerX, centerX - gemSize};
    int[] gemY = {centerY - gemSize, centerY, centerY + gemSize, centerY};
    g2d.fillPolygon(gemX, gemY, 4);
    
    // Viền kim cương
    g2d.setColor(new Color(255, 255, 255, alpha));
    g2d.setStroke(new BasicStroke(1));
    g2d.drawPolygon(gemX, gemY, 4);
    
    g2d.setTransform(oldTransform);
}

    
    /**
     * Vẽ monsters
     */
    private void drawMonsters(Graphics2D g2d, Level level) {
        if (level == null) return;
        
        for (Monster monster : level.getMonsters()) {
            drawMonster(g2d, monster);
        }
    }
    
    /**
     * Vẽ một monster
     */
  private void drawMonster(Graphics2D g2d, Monster monster) {
    Point pos = monster.getPosition();
    int x = pos.x * Constants.CELL_SIZE;
    int y = pos.y * Constants.CELL_SIZE;

    String type = monster.getType().toUpperCase();

    BufferedImage monsterImage = monsterImages.get(type);

    if (monsterImage != null) {
        // Vẽ ảnh quái vật đúng vị trí và kích thước ô lưới
        g2d.drawImage(monsterImage, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE, null);
    } else {
        // Nếu chưa load được ảnh, fallback về vẽ quái vật chung
        drawGenericMonster(g2d, x, y);
    }
}

/**
 * 🕷️ Vẽ nhện tĩnh - đáng sợ và có nhiều chân
 */
private void drawStaticSpider(Graphics2D g2d, int x, int y) {
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Thân nhện - màu đen bóng
    g2d.setColor(new Color(20, 20, 20));
    g2d.fillOval(centerX - 8, centerY - 6, 16, 12);
    
    // Đầu nhện
    g2d.setColor(new Color(40, 40, 40));
    g2d.fillOval(centerX - 5, centerY - 8, 10, 8);
    
    // 8 chân nhện
    g2d.setColor(new Color(60, 40, 20));
    g2d.setStroke(new BasicStroke(2));
    
    // Chân trái (4 chân)
    for (int i = 0; i < 4; i++) {
        int legY = centerY - 6 + (i * 4);
        g2d.drawLine(centerX - 8, legY, centerX - 15, legY - 3);
        g2d.drawLine(centerX - 15, legY - 3, centerX - 18, legY + 2);
    }
    
    // Chân phải (4 chân)
    for (int i = 0; i < 4; i++) {
        int legY = centerY - 6 + (i * 4);
        g2d.drawLine(centerX + 8, legY, centerX + 15, legY - 3);
        g2d.drawLine(centerX + 15, legY - 3, centerX + 18, legY + 2);
    }
    
    // Mắt đỏ lấp lánh
    int alpha = 150 + (int)(100 * Math.sin(animationFrame * 0.2));
    g2d.setColor(new Color(255, 0, 0, alpha));
    g2d.fillOval(centerX - 3, centerY - 6, 2, 2);
    g2d.fillOval(centerX + 1, centerY - 6, 2, 2);
    
    // Viền thân
    g2d.setColor(new Color(80, 80, 80));
    g2d.setStroke(new BasicStroke(1));
    g2d.drawOval(centerX - 8, centerY - 6, 16, 12);
}

/**
 * 🐍 Vẽ rắn di chuyển dọc (lên xuống)
 */
private void drawVerticalSnake(Graphics2D g2d, int x, int y, String direction) {
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Màu rắn xanh lá độc
    Color snakeColor = new Color(34, 139, 34);
    Color snakeDark = new Color(0, 100, 0);
    
    // Thân rắn - hình oval dài
    g2d.setColor(snakeColor);
    if ("UP".equals(direction)) {
        // Đầu hướng lên
        g2d.fillOval(centerX - 6, y + 2, 12, Constants.CELL_SIZE - 4);
        // Đầu rắn
        g2d.setColor(snakeDark);
        g2d.fillOval(centerX - 4, y + 2, 8, 8);
    } else {
        // Đầu hướng xuống  
        g2d.fillOval(centerX - 6, y + 2, 12, Constants.CELL_SIZE - 4);
        // Đầu rắn
        g2d.setColor(snakeDark);
        g2d.fillOval(centerX - 4, y + Constants.CELL_SIZE - 10, 8, 8);
    }
    
    // Vân rắn - các đường zigzag
    g2d.setColor(new Color(255, 255, 0, 150)); // Vàng trong suốt
    g2d.setStroke(new BasicStroke(2));
    for (int i = 0; i < 3; i++) {
        int zigY = y + 6 + (i * 6);
        g2d.drawLine(centerX - 4, zigY, centerX + 4, zigY + 3);
        g2d.drawLine(centerX + 4, zigY + 3, centerX - 4, zigY + 6);
    }
    
    // Mắt rắn
    g2d.setColor(Color.RED);
    if ("UP".equals(direction)) {
        g2d.fillOval(centerX - 2, y + 4, 2, 2);
        g2d.fillOval(centerX + 1, y + 4, 2, 2);
    } else {
        g2d.fillOval(centerX - 2, y + Constants.CELL_SIZE - 8, 2, 2);
        g2d.fillOval(centerX + 1, y + Constants.CELL_SIZE - 8, 2, 2);
    }
}

/**
 * 🐍 Vẽ rắn di chuyển ngang (trái phải)
 */
private void drawHorizontalSnake(Graphics2D g2d, int x, int y, String direction) {
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Màu rắn đỏ độc
    Color snakeColor = new Color(220, 20, 60);
    Color snakeDark = new Color(139, 0, 0);
    
    // Thân rắn - hình oval ngang
    g2d.setColor(snakeColor);
    g2d.fillOval(x + 2, centerY - 6, Constants.CELL_SIZE - 4, 12);
    
    // Đầu rắn
    g2d.setColor(snakeDark);
    if ("LEFT".equals(direction)) {
        // Đầu hướng trái
        g2d.fillOval(x + 2, centerY - 4, 8, 8);
    } else {
        // Đầu hướng phải
        g2d.fillOval(x + Constants.CELL_SIZE - 10, centerY - 4, 8, 8);
    }
    
    // Vân rắn - các vòng tròn
    g2d.setColor(new Color(255, 215, 0, 120)); // Vàng trong suốt
    g2d.setStroke(new BasicStroke(1));
    for (int i = 0; i < 3; i++) {
        int circleX = x + 6 + (i * 6);
        g2d.drawOval(circleX, centerY - 3, 6, 6);
    }
    
    // Mắt rắn
    g2d.setColor(Color.YELLOW);
    if ("LEFT".equals(direction)) {
        g2d.fillOval(x + 4, centerY - 2, 2, 2);
        g2d.fillOval(x + 4, centerY + 1, 2, 2);
    } else {
        g2d.fillOval(x + Constants.CELL_SIZE - 8, centerY - 2, 2, 2);
        g2d.fillOval(x + Constants.CELL_SIZE - 8, centerY + 1, 2, 2);
    }
    
    // Lưỡi rắn (nhấp nháy)
    int tongueAlpha = 100 + (int)(100 * Math.sin(animationFrame * 0.4));
    g2d.setColor(new Color(255, 0, 0, tongueAlpha));
    g2d.setStroke(new BasicStroke(2));
    if ("LEFT".equals(direction)) {
        g2d.drawLine(x + 2, centerY, x - 2, centerY);
    } else {
        g2d.drawLine(x + Constants.CELL_SIZE - 2, centerY, x + Constants.CELL_SIZE + 2, centerY);
    }
}

/**
 * 👾 Vẽ quái vật chung (dự phòng)
 */
private void drawGenericMonster(Graphics2D g2d, int x, int y) {
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // Quái vật hình kim cương
    g2d.setColor(new Color(128, 0, 128));
    int[] xPoints = {centerX, centerX + 8, centerX, centerX - 8};
    int[] yPoints = {centerY - 8, centerY, centerY + 8, centerY};
    g2d.fillPolygon(xPoints, yPoints, 4);
    
    // Viền
    g2d.setColor(new Color(75, 0, 130));
    g2d.setStroke(new BasicStroke(2));
    g2d.drawPolygon(xPoints, yPoints, 4);
    
    // Mắt
    g2d.setColor(Color.WHITE);
    g2d.fillOval(centerX - 3, centerY - 2, 2, 2);
    g2d.fillOval(centerX + 1, centerY - 2, 2, 2);
}

    
    /**
     * Vẽ người chơi
     */

private void loadPlayerImages() {
    loadPlayerImage("FIRE", "/images/Lua.png");
    loadPlayerImage("WATER", "/images/Nuoc.png");
}

private void loadPlayerImage(String type, String path) {
    try (InputStream is = getClass().getResourceAsStream(path)) {
        if (is != null) {
            BufferedImage img = ImageIO.read(is);
            playerImages.put(type.toUpperCase(), img);
            System.out.println("Load ảnh người chơi " + type + " thành công!");
        } else {
            System.err.println("Không tìm thấy ảnh người chơi " + type + " tại " + path);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
   private void drawPlayers(Graphics2D g2d, GamePanel panel) {
     drawPlayer(g2d, panel.getFirePlayer(), "FIRE");
    drawPlayer(g2d, panel.getWaterPlayer(), "WATER");
}
   private void drawPlayer(Graphics2D g2d, Point playerPos, String type) {
    if (playerPos == null) return;

    int x = playerPos.x * Constants.CELL_SIZE;
    int y = playerPos.y * Constants.CELL_SIZE;

    BufferedImage playerImage = playerImages.get(type.toUpperCase());
    if (playerImage != null) {
        g2d.drawImage(playerImage, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE, null);
    } else {
        // Fallback: vẽ hình tròn đại diện người chơi
        g2d.setColor(type.equals("FIRE") ? Color.RED : Color.BLUE);
        g2d.fillOval(x + 4, y + 4, Constants.CELL_SIZE - 8, Constants.CELL_SIZE - 8);
    }
}

    private void drawStatus(Graphics2D g2d, GamePanel panel) {
        String status = panel.getStatus();
        if (status == null || status.isEmpty()) return;
        
        g2d.setColor(STATUS_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(status, 10, panel.getHeight() - 10);
    }
    
    
    private void drawWinAnimation(Graphics2D g2d, GamePanel panel) {
        // Hiệu ứng pháo hoa đơn giản
        g2d.setColor(new Color(255, 215, 0, 100)); // Vàng trong suốt
        for (int i = 0; i < 5; i++) {
            int x = (panel.getWidth() / 6) * (i + 1);
            int y = panel.getHeight() / 3;
            int radius = 20 + (animationFrame * 3) % 30;
            g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        }
        
        // Text chúc mừng
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String winText = "CHÚC MỪNG!";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (panel.getWidth() - fm.stringWidth(winText)) / 2;
        int textY = panel.getHeight() / 2;
        g2d.drawString(winText, textX, textY);
    }
    
    /**
     * Cập nhật frame animation
     */
    public void updateAnimation() {
        animationFrame = (animationFrame + 1) % 100;
    }
    
    public int getAnimationFrame() {
        return animationFrame;
    }
}
