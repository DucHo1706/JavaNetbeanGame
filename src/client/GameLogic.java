package client;

import game.Monster;
import server.Level;
import java.awt.Point;
import java.io.PrintWriter;

/**
 * Xử lý logic chính của game
 */
public class GameLogic {
    private GamePanel gamePanel;
    private PrintWriter out;
    
    public GameLogic(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }
    
    public void setConnection(PrintWriter out) {
        this.out = out;
    }
    
    /**
     * Xử lý di chuyển người chơi
     */
    public boolean movePlayer(String direction) {
        Point myPlayer = gamePanel.getMyPlayer();
        if (myPlayer == null) return false;
        
        Point newPosition = calculateNewPosition(myPlayer, direction);
        
        // Kiểm tra có thể di chuyển không
        if (!canMoveTo(newPosition)) {
            return false;
        }
        
        // Cập nhật vị trí
        myPlayer.setLocation(newPosition);
        
        // Cập nhật vị trí trong fire/water player tương ứng
        updatePlayerPosition(newPosition);
        
        // Kiểm tra va chạm với monster
        checkMonsterCollision();
        
        // Kiểm tra điều kiện thắng
        checkWinCondition();
        
        return true;
    }
    
    /**
     * Tính toán vị trí mới
     */
    private Point calculateNewPosition(Point current, String direction) {
        Point newPos = new Point(current);
        
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
        }
        
        return newPos;
    }
    
    /**
     * Kiểm tra có thể di chuyển đến vị trí này không
     */
    private boolean canMoveTo(Point position) {
        // Kiểm tra biên
        if (position.x < 0 || position.x >= gamePanel.getGridWidth() ||
            position.y < 0 || position.y >= gamePanel.getGridHeight()) {
            return false;
        }
        
        // Kiểm tra tường
        if (gamePanel.getWalls().contains(position)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Cập nhật vị trí player tương ứng
     */
    private void updatePlayerPosition(Point newPosition) {
        String playerType = gamePanel.getPlayerType();
        if ("FIRE".equals(playerType)) {
            gamePanel.getFirePlayer().setLocation(newPosition);
        } else if ("WATER".equals(playerType)) {
            gamePanel.getWaterPlayer().setLocation(newPosition);
        }
    }
    
    /**
     * Kiểm tra va chạm với monster
     */
    public void checkMonsterCollision() {
        Level currentLevel = gamePanel.getCurrentLevelData();
        Point myPlayer = gamePanel.getMyPlayer();
        
        if (currentLevel == null || myPlayer == null) return;
        
        Monster monsterAtMyPos = currentLevel.getMonsterAt(myPlayer.x, myPlayer.y);
        if (monsterAtMyPos != null) {
            System.out.println("VA CHẠM! Player chạm monster tại " + myPlayer);
            resetPlayerToStart();
        }
    }
    
    /**
     * Reset player về vị trí ban đầu
     */
    private void resetPlayerToStart() {
        Level currentLevel = gamePanel.getCurrentLevelData();
        if (currentLevel == null) return;
        
        String playerType = gamePanel.getPlayerType();
        Point myPlayer = gamePanel.getMyPlayer();

        if("FIRE".equals(playerType) || "WATER".equals(playerType))
        {
            Point waterStart = currentLevel.getWaterStart();
            Point fireStart = currentLevel.getFireStart();
            myPlayer.setLocation(waterStart);
            myPlayer.setLocation(fireStart);
            gamePanel.getWaterPlayer().setLocation(waterStart);
            gamePanel.getFirePlayer().setLocation(fireStart);
        }
        
        gamePanel.setStatus("Bị monster tấn công! Quay lại vị trí ban đầu.");
        sendPlayerUpdate();
    }
    
    /**
     * Kiểm tra điều kiện thắng
     */
    private void checkWinCondition() {
        Point door = gamePanel.getDoor();
        Point firePlayer = gamePanel.getFirePlayer();
        Point waterPlayer = gamePanel.getWaterPlayer();
        
        if (door != null && firePlayer != null && waterPlayer != null) {
            boolean fireAtDoor = firePlayer.equals(door);
            boolean waterAtDoor = waterPlayer.equals(door);
            
            if (fireAtDoor && waterAtDoor) {
                gamePanel.setShowWinAnimation(true);
                gamePanel.setStatus("Hoàn thành màn chơi! Chuyển sang màn tiếp theo...");
                
                // Gửi tín hiệu hoàn thành màn
                if (out != null) {
                    out.println("LEVEL_COMPLETE");
                }
            }
        }
    }
    
    /**
     * Gửi cập nhật vị trí player
     */
    public void sendPlayerUpdate() {
        if (out == null) return;
        
        Point myPlayer = gamePanel.getMyPlayer();
        if (myPlayer != null) {
            String message = "PLAYER_UPDATE:" + myPlayer.x + ":" + myPlayer.y;
            out.println(message);
        }
    }
    
    /**
     * Cập nhật monsters
     */
    public void updateMonsters() {
        Level currentLevel = gamePanel.getCurrentLevelData();
        if (currentLevel != null) {
            currentLevel.updateMonsters();
            checkMonsterCollision();
        }
    }
}
