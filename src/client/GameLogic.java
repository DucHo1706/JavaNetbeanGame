package client;

import game.Monster;
import server.Level;
import java.awt.Point;
import java.io.PrintWriter;
import utils.Constants;

/**
 * Xu ly logic chinh cua game
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
     * Xu ly di chuyen nguoi choi
     */
    public boolean movePlayer(String direction) {
        Point myPlayer = gamePanel.getMyPlayer();
        if (myPlayer == null) return false;
        
        Point newPosition = calculateNewPosition(myPlayer, direction);
        
        // Kiem tra co the di chuyen khong
        if (!canMoveTo(newPosition)) {
            return false;
        }
        // âm thanh di chuyển 
         gamePanel.playMoveSound();
        // Cap nhat vi tri
        myPlayer.setLocation(newPosition);
        
        // Cap nhat vi tri trong fire/water player tuong ung
        updatePlayerPosition(newPosition);
        
        // Kiem tra va cham voi monster
        checkMonsterCollision();
        
        // Kiem tra dieu kien thang
        checkWinCondition();
        
        return true;
    }
    
    /**
     * Tinh toan vi tri moi
     */
    private Point calculateNewPosition(Point current, String direction) {
        Point newPos = new Point(current);
        
        switch (direction) {
            case Constants.UP:
                newPos.y--;
                break;
            case Constants.DOWN:
                newPos.y++;
                break;
            case Constants.LEFT:
                newPos.x--;
                break;
            case Constants.RIGHT:
                newPos.x++;
                break;
        }
        
        return newPos;
    }
    
    /**
     * Kiem tra co the di chuyen den vi tri nay khong
     */
    private boolean canMoveTo(Point position) {
        // Kiem tra bien
        if (position.x < 0 || position.x >= gamePanel.getGridWidth() ||
            position.y < 0 || position.y >= gamePanel.getGridHeight()) {
            return false;
        }
        
        // Kiem tra tuong
        if (gamePanel.getWalls().contains(position)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Cap nhat vi tri player 
     */
    private void updatePlayerPosition(Point newPosition) {
        String playerType = gamePanel.getPlayerType();
        if ("FIRE".equals(playerType)) {
            if (gamePanel.getFirePlayer() != null) {
                gamePanel.getFirePlayer().setLocation(newPosition);
            }
        } else if ("WATER".equals(playerType)) {
            if (gamePanel.getWaterPlayer() != null) {
                gamePanel.getWaterPlayer().setLocation(newPosition);
            }
        }
    }
    
    /**
     * Kiem tra va cham voi monster
     */
    public void checkMonsterCollision() {
        Level currentLevel = gamePanel.getCurrentLevelData();
        Point myPlayer = gamePanel.getMyPlayer();
        
        if (currentLevel == null || myPlayer == null) return;
        
        Monster monsterAtMyPos = currentLevel.getMonsterAt(myPlayer.x, myPlayer.y);
        if (monsterAtMyPos != null) {
            gamePanel.playMonsterCollisionSound();
            System.out.println("VA CHAM! Player cham monster tai " + myPlayer);
            resetPlayerToStart();
        }
    }
    
    /**
     * Reset player ve vi tri ban dau
     */
    private void resetPlayerToStart() {
        Level currentLevel = gamePanel.getCurrentLevelData();
        if (currentLevel == null) return;
        
        String playerType = gamePanel.getPlayerType();
        
        Point waterStart = currentLevel.getWaterStart();
        Point fireStart = currentLevel.getFireStart();
        
        if ("FIRE".equals(playerType)) {
            if (gamePanel.getFirePlayer() != null && fireStart != null) {
                gamePanel.getFirePlayer().setLocation(fireStart);
                gamePanel.getMyPlayer().setLocation(fireStart);
            }
        } else if ("WATER".equals(playerType)) {
            if (gamePanel.getWaterPlayer() != null && waterStart != null) {
                gamePanel.getWaterPlayer().setLocation(waterStart);
                gamePanel.getMyPlayer().setLocation(waterStart);
            }
        }
        
        gamePanel.setStatus("Bi monster tan cong! Quay lai vi tri ban dau.");
        sendPlayerUpdate();
    }
    
    /**
     * Kiem tra dieu kien thang
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
                gamePanel.setStatus("Hoan thanh man choi! Chuyen sang man tiep theo...");
                
                // Gui tin hieu hoan thanh man
                if (out != null) {
                    out.println(Constants.HOAN_THANH_CAP_DO);
                }
            }
        }
    }
    
    /**
     * Gui cap nhat vi tri player
     */
    public void sendPlayerUpdate() {
        if (out == null) return;
        
        Point myPlayer = gamePanel.getMyPlayer();
        if (myPlayer != null) {
            String message = Constants.DI_CHUYEN + ":" + myPlayer.x + ":" + myPlayer.y;
            out.println(message);
        }
    }
    
    /**
     * Cap nhat monsters
     */
    public void updateMonsters() {
        Level currentLevel = gamePanel.getCurrentLevelData();
        if (currentLevel != null) {
            currentLevel.updateMonsters();
            checkMonsterCollision();
        }
    }
}