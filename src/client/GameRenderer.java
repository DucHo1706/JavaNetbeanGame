package client;

import utils.Constants;
import game.Monster;
import server.Level;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Set;

/**
 * Class chuyên vẽ các thành phần của game
 */
public class GameRenderer {
    private final Color BACKGROUND_COLOR = new Color(34, 34, 34);
    private final Color GRID_COLOR = new Color(64, 64, 64);
    private final Color FIRE_COLOR = Color.RED;
    private final Color WATER_COLOR = Color.CYAN;
    private final Color WALL_COLOR = new Color(139, 69, 19);
    private final Color DOOR_COLOR = Color.YELLOW;
    private final Color STATUS_COLOR = Color.WHITE;
    
    private int animationFrame = 0;
    
    /**
     * Vẽ toàn bộ game panel
     */
    public void render(Graphics g, GamePanel panel) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Vẽ background
        drawBackground(g2d, panel);
        
        // Vẽ grid
        drawGrid(g2d, panel);
        
        // Vẽ walls
        drawWalls(g2d, panel.getWalls());
        
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
    
    /**
     * Vẽ background
     */
  private void drawBackground(Graphics2D g2d, GamePanel panel) {
    // 🌌 Background gradient từ tím đậm đến xanh đen
    GradientPaint gradient = new GradientPaint(
        0, 0, new Color(25, 25, 45),           // Tím đậm trên
        panel.getWidth(), panel.getHeight(), 
        new Color(15, 30, 40)                  // Xanh đen dưới
    );
    g2d.setPaint(gradient);
    g2d.fillRect(0, 0, panel.getWidth(), panel.getHeight());
    
    // ✨ Thêm các ngôi sao nhỏ lấp lánh
    drawStars(g2d, panel);
    
    // 🌙 Thêm ánh trăng mờ ở góc
    drawMoonlight(g2d, panel);
    
    // 💫 Thêm hiệu ứng sương mù nhẹ
    drawMist(g2d, panel);
}

/**
 * ⭐ Vẽ các ngôi sao lấp lánh
 */
private void drawStars(Graphics2D g2d, GamePanel panel) {
    g2d.setColor(new Color(255, 255, 255, 80));
    
    // Tạo 30 ngôi sao ở vị trí cố định
    for (int i = 0; i < 30; i++) {
        int x = (i * 73 + 50) % panel.getWidth();
        int y = (i * 97 + 30) % panel.getHeight();
        
        // Hiệu ứng nhấp nháy nhẹ
        int alpha = 60 + (int)(40 * Math.sin(animationFrame * 0.05 + i));
        g2d.setColor(new Color(255, 255, 255, alpha));
        
        // Vẽ ngôi sao nhỏ
        g2d.fillOval(x, y, 2, 2);
        
        // Một số ngôi sao lớn hơn
        if (i % 5 == 0) {
            g2d.fillOval(x-1, y-1, 4, 4);
        }
    }
}

/**
 * 🌙 Vẽ ánh trăng mờ
 */
private void drawMoonlight(Graphics2D g2d, GamePanel panel) {
    // Ánh trăng ở góc trên phải
    int moonX = panel.getWidth() - 80;
    int moonY = 50;
    
    // Hào quang trăng
    RadialGradientPaint moonGlow = new RadialGradientPaint(
        moonX, moonY, 60,
        new float[]{0f, 0.7f, 1f},
        new Color[]{
            new Color(220, 220, 255, 30),  // Trắng xanh nhạt
            new Color(180, 180, 220, 15),  // Xanh nhạt
            new Color(100, 100, 150, 0)    // Trong suốt
        }
    );
    g2d.setPaint(moonGlow);
    g2d.fillOval(moonX - 60, moonY - 60, 120, 120);
}

/**
 * 💫 Vẽ sương mù nhẹ
 */
private void drawMist(Graphics2D g2d, GamePanel panel) {
    // Sương mù di chuyển chậm
    float mistOffset = (animationFrame * 0.2f) % panel.getWidth();
    
    g2d.setColor(new Color(200, 200, 255, 10));
    
    // Vẽ 3 lớp sương mù
    for (int layer = 0; layer < 3; layer++) {
        float layerOffset = mistOffset + (layer * panel.getWidth() / 3);
        
        for (int i = 0; i < 5; i++) {
            int x = (int)(layerOffset + i * 150) % (panel.getWidth() + 100) - 50;
            int y = panel.getHeight() - 100 + layer * 20 + (int)(10 * Math.sin(animationFrame * 0.03 + i));
            
            // Vẽ đám sương hình oval
            g2d.fillOval(x, y, 100 + layer * 20, 30 + layer * 5);
        }
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
    
    /**
     * Vẽ tường
     */
  private void drawWalls(Graphics2D g2d, Set<Point> walls) {
    for (Point wall : walls) {
        int x = wall.x * Constants.CELL_SIZE;
        int y = wall.y * Constants.CELL_SIZE;
        
        // 🏗️ Tường chính - màu nâu đá cổ
        g2d.setColor(new Color(101, 67, 33)); // Nâu đá
        g2d.fillRect(x + 1, y + 1, Constants.CELL_SIZE - 2, Constants.CELL_SIZE - 2);
        
        // ✨ Highlight trên và trái (ánh sáng)
        g2d.setColor(new Color(140, 100, 60)); // Nâu sáng
        g2d.fillRect(x + 2, y + 2, Constants.CELL_SIZE - 6, 2); // Thanh sáng trên
        g2d.fillRect(x + 2, y + 2, 2, Constants.CELL_SIZE - 6); // Thanh sáng trái
        
        // 🌑 Bóng đổ phải và dưới
        g2d.setColor(new Color(60, 40, 20)); // Nâu tối
        g2d.fillRect(x + Constants.CELL_SIZE - 3, y + 3, 2, Constants.CELL_SIZE - 3); // Bóng phải
        g2d.fillRect(x + 3, y + Constants.CELL_SIZE - 3, Constants.CELL_SIZE - 3, 2); // Bóng dưới
        
        // 🔲 Viền ngoài đậm
        g2d.setColor(new Color(40, 25, 15)); // Nâu đen
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x + 1, y + 1, Constants.CELL_SIZE - 2, Constants.CELL_SIZE - 2);
        
        // 🎨 Thêm texture đá nhỏ (chấm nhỏ)
        drawStoneTexture(g2d, x, y);
    }
}

/**
 * 🪨 Vẽ texture đá cho tường
 */
private void drawStoneTexture(Graphics2D g2d, int x, int y) {
    g2d.setColor(new Color(80, 50, 25, 100)); // Nâu nhạt trong suốt
    
    // Vẽ vài chấm nhỏ làm texture
    int[] dotX = {x + 6, x + 12, x + 18, x + 8, x + 16};
    int[] dotY = {y + 8, y + 6, y + 14, y + 16, y + 20};
    
    for (int i = 0; i < Math.min(dotX.length, dotY.length); i++) {
        if (dotX[i] < x + Constants.CELL_SIZE - 4 && dotY[i] < y + Constants.CELL_SIZE - 4) {
            g2d.fillOval(dotX[i], dotY[i], 2, 2);
        }
    }
    
    // Thêm vài vết nứt nhỏ
    g2d.setColor(new Color(40, 25, 15, 80));
    g2d.setStroke(new BasicStroke(1));
    g2d.drawLine(x + 5, y + 10, x + 8, y + 12); // Vết nứt nhỏ
    g2d.drawLine(x + 15, y + 7, x + 17, y + 11); // Vết nứt nhỏ
}

    
    /**
     * Vẽ cửa
     */
   private void drawDoor(Graphics2D g2d, Point door) {
    if (door == null) return;
    
    int x = door.x * Constants.CELL_SIZE;
    int y = door.y * Constants.CELL_SIZE;
    
    // 🌟 Hào quang ma thuật xung quanh cửa
    drawMagicalAura(g2d, x, y);
    
    // 🚪 Cửa chính - hình chữ nhật cổ điển
    drawMainDoor(g2d, x, y);
    
    // ✨ Ký hiệu ma thuật trên cửa
    drawMagicalSymbols(g2d, x, y);
    
    // 💎 Báu vật lấp lánh ở giữa
    drawCenterGem(g2d, x, y);
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
    
    String type = monster.getType();
    
    switch (type.toUpperCase()) {
        case "TINH":
            drawStaticSpider(g2d, x, y);
            break;
        case "LENXUONG":
            drawVerticalSnake(g2d, x, y, monster.getDirection());
            break;
        case "TRAIPHAI":
            drawHorizontalSnake(g2d, x, y, monster.getDirection());
            break;
        default:
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
   private void drawPlayers(Graphics2D g2d, GamePanel panel) {
    // Vẽ Fire Warrior - Chiến binh lửa
    drawFireWarrior(g2d, panel.getFirePlayer());
    
    // Vẽ Water Warrior - Chiến binh nước
    drawWaterWarrior(g2d, panel.getWaterPlayer());
}

/**
 * ⚔️ Vẽ chiến binh lửa - Fire Warrior
 */
private void drawFireWarrior(Graphics2D g2d, Point player) {
    if (player == null) return;
    
    int x = player.x * Constants.CELL_SIZE;
    int y = player.y * Constants.CELL_SIZE;
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // 🔥 Hào quang lửa xung quanh
    drawFireAura(g2d, centerX, centerY);
    
    // 🛡️ Thân chiến binh
    drawWarriorBody(g2d, centerX, centerY, new Color(139, 69, 19)); // Nâu giáp da
    
    // ⚔️ Kiếm lửa
    drawFireSword(g2d, centerX, centerY);
    
    // 🔥 Mũ chiến binh lửa
    drawFireHelmet(g2d, centerX, centerY);
    
    // 👁️ Mắt sáng
    drawWarriorEyes(g2d, centerX, centerY, Color.ORANGE);
}

/**
 * 🌊 Vẽ chiến binh nước - Water Warrior  
 */
private void drawWaterWarrior(Graphics2D g2d, Point player) {
    if (player == null) return;
    
    int x = player.x * Constants.CELL_SIZE;
    int y = player.y * Constants.CELL_SIZE;
    int centerX = x + Constants.CELL_SIZE / 2;
    int centerY = y + Constants.CELL_SIZE / 2;
    
    // 🌊 Hào quang nước xung quanh
    drawWaterAura(g2d, centerX, centerY);
    
    // 🛡️ Thân chiến binh
    drawWarriorBody(g2d, centerX, centerY, new Color(70, 130, 180)); // Xanh giáp thép
    
    // 🔱 Đinh ba nước
    drawWaterTrident(g2d, centerX, centerY);
    
    // 🌊 Mũ chiến binh nước
    drawWaterHelmet(g2d, centerX, centerY);
    
    // 👁️ Mắt sáng
    drawWarriorEyes(g2d, centerX, centerY, Color.CYAN);
}

/**
 * 🔥 Hào quang lửa
 */
private void drawFireAura(Graphics2D g2d, int centerX, int centerY) {
    // Hiệu ứng lửa nhấp nháy
    int flameAlpha = 80 + (int)(40 * Math.sin(animationFrame * 0.4));
    
    // Lửa ngoài
    RadialGradientPaint fireAura = new RadialGradientPaint(
        centerX, centerY, Constants.CELL_SIZE / 2 + 3,
        new float[]{0f, 0.7f, 1f},
        new Color[]{
            new Color(255, 69, 0, flameAlpha),    // Đỏ cam trong
            new Color(255, 140, 0, flameAlpha/2), // Cam giữa
            new Color(255, 215, 0, 0)             // Vàng ngoài trong suốt
        }
    );
    g2d.setPaint(fireAura);
    g2d.fillOval(centerX - Constants.CELL_SIZE/2 - 2, centerY - Constants.CELL_SIZE/2 - 2, 
                 Constants.CELL_SIZE + 4, Constants.CELL_SIZE + 4);
}

/**
 * 🌊 Hào quang nước
 */
private void drawWaterAura(Graphics2D g2d, int centerX, int centerY) {
    // Hiệu ứng nước lấp lánh
    int waveAlpha = 60 + (int)(30 * Math.sin(animationFrame * 0.3));
    
    RadialGradientPaint waterAura = new RadialGradientPaint(
        centerX, centerY, Constants.CELL_SIZE / 2 + 3,
        new float[]{0f, 0.6f, 1f},
        new Color[]{
            new Color(0, 191, 255, waveAlpha),    // Xanh dương trong
            new Color(135, 206, 250, waveAlpha/2), // Xanh nhạt giữa  
            new Color(240, 248, 255, 0)           // Trắng ngoài trong suốt
        }
    );
    g2d.setPaint(waterAura);
    g2d.fillOval(centerX - Constants.CELL_SIZE/2 - 2, centerY - Constants.CELL_SIZE/2 - 2,
                 Constants.CELL_SIZE + 4, Constants.CELL_SIZE + 4);
}

/**
 * 🛡️ Thân chiến binh
 */
private void drawWarriorBody(Graphics2D g2d, int centerX, int centerY, Color armorColor) {
    // Thân người - hình oval
    g2d.setColor(armorColor);
    g2d.fillOval(centerX - 8, centerY - 2, 16, 12);
    
    // Giáp ngực - hình chữ nhật bo góc
    g2d.setColor(armorColor.darker());
    g2d.fillRoundRect(centerX - 6, centerY - 1, 12, 8, 4, 4);
    
    // Vai giáp
    g2d.setColor(armorColor);
    g2d.fillOval(centerX - 10, centerY - 3, 6, 8); // Vai trái
    g2d.fillOval(centerX + 4, centerY - 3, 6, 8);  // Vai phải
    
    // Viền giáp
    g2d.setColor(new Color(192, 192, 192)); // Bạc
    g2d.setStroke(new BasicStroke(1));
    g2d.drawOval(centerX - 8, centerY - 2, 16, 12);
}

/**
 * ⚔️ Kiếm lửa
 */
private void drawFireSword(Graphics2D g2d, int centerX, int centerY) {
    // Cán kiếm
    g2d.setColor(new Color(139, 69, 19)); // Nâu gỗ
    g2d.setStroke(new BasicStroke(3));
    g2d.drawLine(centerX + 8, centerY + 5, centerX + 12, centerY + 12);
    
    // Lưỡi kiếm
    g2d.setColor(new Color(192, 192, 192)); // Bạc
    g2d.setStroke(new BasicStroke(2));
    g2d.drawLine(centerX + 12, centerY + 12, centerX + 16, centerY + 4);
    
    // Hiệu ứng lửa trên kiếm
    int fireAlpha = 120 + (int)(80 * Math.sin(animationFrame * 0.5));
    g2d.setColor(new Color(255, 69, 0, fireAlpha));
    g2d.setStroke(new BasicStroke(1));
    g2d.drawLine(centerX + 13, centerY + 10, centerX + 15, centerY + 6);
    g2d.drawLine(centerX + 14, centerY + 9, centerX + 16, centerY + 5);
}

/**
 * 🔱 Đinh ba nước
 */
private void drawWaterTrident(Graphics2D g2d, int centerX, int centerY) {
    // Cán đinh ba
    g2d.setColor(new Color(70, 130, 180)); // Xanh thép
    g2d.setStroke(new BasicStroke(3));
    g2d.drawLine(centerX + 8, centerY + 5, centerX + 12, centerY + 12);
    
    // 3 ngạnh đinh ba
    g2d.setColor(new Color(192, 192, 192)); // Bạc
    g2d.setStroke(new BasicStroke(2));
    g2d.drawLine(centerX + 12, centerY + 12, centerX + 14, centerY + 4); // Giữa
    g2d.drawLine(centerX + 12, centerY + 12, centerX + 12, centerY + 5); // Trái
    g2d.drawLine(centerX + 12, centerY + 12, centerX + 16, centerY + 5); // Phải
    
    // Hiệu ứng nước trên đinh ba
    int waterAlpha = 100 + (int)(60 * Math.sin(animationFrame * 0.4));
    g2d.setColor(new Color(0, 191, 255, waterAlpha));
    g2d.setStroke(new BasicStroke(1));
    for (int i = 0; i < 3; i++) {
        g2d.drawOval(centerX + 11 + i, centerY + 7 + i, 2, 2);
    }
}

/**
 * 🔥 Mũ chiến binh lửa
 */
private void drawFireHelmet(Graphics2D g2d, int centerX, int centerY) {
    // Mũ chính
    g2d.setColor(new Color(178, 34, 34)); // Đỏ thẫm
    g2d.fillArc(centerX - 7, centerY - 12, 14, 12, 0, 180);
    
    // Lông vũ đỏ
    g2d.setColor(new Color(255, 69, 0));
    g2d.setStroke(new BasicStroke(2));
    g2d.drawLine(centerX - 2, centerY - 12, centerX - 4, centerY - 16);
    g2d.drawLine(centerX + 2, centerY - 12, centerX + 4, centerY - 16);
    
    // Viền mũ vàng
    g2d.setColor(new Color(255, 215, 0));
    g2d.setStroke(new BasicStroke(1));
    g2d.drawArc(centerX - 7, centerY - 12, 14, 12, 0, 180);
}

/**
 * 🌊 Mũ chiến binh nước
 */
private void drawWaterHelmet(Graphics2D g2d, int centerX, int centerY) {
    // Mũ chính
    g2d.setColor(new Color(25, 25, 112)); // Xanh navy
    g2d.fillArc(centerX - 7, centerY - 12, 14, 12, 0, 180);
    
    // Sừng mũ
    g2d.setColor(new Color(70, 130, 180));
    g2d.setStroke(new BasicStroke(2));
    g2d.drawLine(centerX - 5, centerY - 10, centerX - 7, centerY - 15);
    g2d.drawLine(centerX + 5, centerY - 10, centerX + 7, centerY - 15);
    
    // Viền mũ bạc
    g2d.setColor(new Color(192, 192, 192));
    g2d.setStroke(new BasicStroke(1));
    g2d.drawArc(centerX - 7, centerY - 12, 14, 12, 0, 180);
}

/**
 * 👁️ Mắt chiến binh
 */
private void drawWarriorEyes(Graphics2D g2d, int centerX, int centerY, Color eyeColor) {
    // Hiệu ứng mắt sáng
    int glowAlpha = 180 + (int)(75 * Math.sin(animationFrame * 0.2));
    g2d.setColor(new Color(eyeColor.getRed(), eyeColor.getGreen(), 
                          eyeColor.getBlue(), glowAlpha));
    
    // Mắt trái
    g2d.fillOval(centerX - 4, centerY - 8, 3, 3);
    // Mắt phải  
    g2d.fillOval(centerX + 1, centerY - 8, 3, 3);
    
    // Tia sáng từ mắt
    g2d.setStroke(new BasicStroke(1));
    g2d.drawLine(centerX - 3, centerY - 7, centerX - 6, centerY - 9);
    g2d.drawLine(centerX + 2, centerY - 7, centerX + 5, centerY - 9);
}

    /**
     * Vẽ status
     */
    private void drawStatus(Graphics2D g2d, GamePanel panel) {
        String status = panel.getStatus();
        if (status == null || status.isEmpty()) return;
        
        g2d.setColor(STATUS_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(status, 10, panel.getHeight() - 10);
    }
    
    /**
     * Vẽ animation chiến thắng
     */
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
