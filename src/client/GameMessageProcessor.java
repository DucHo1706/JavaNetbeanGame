package client;

import javax.swing.*;

/**
 * Xử lý các tin nhắn liên quan đến game từ server
 */
public class GameMessageProcessor implements MessageHandler {
    private GamePanel gamePanel;        // Bảng hiển thị game
    private JFrame parentFrame;         // Cửa sổ chính
    private String playerType;          // Loại người chơi (FIRE/WATER)
    
    public GameMessageProcessor(GamePanel gamePanel, JFrame parentFrame) {
        this.gamePanel = gamePanel;
        this.parentFrame = parentFrame;
    }
    
    @Override
    public void handleMessage(String message) {
        // Xử lý tin nhắn trên EDT thread để đảm bảo thread-safe với Swing
        SwingUtilities.invokeLater(() -> processMessage(message));
    }
    
    /**
     * Phân tích và xử lý tin nhắn từ server
     */
    private void processMessage(String message) {
        String[] parts = message.split(":");
        String command = parts[0];
        
        switch (command) {
            case "ROOM_JOINED":
                handleRoomJoined(parts);    // Xử lý khi vào phòng thành công
                break;
            case "WAITING_FOR_PLAYER":
                gamePanel.setStatus("Đang chờ người chơi khác...");
                break;
            case "GAME_START":
                handleGameStart(parts);     // Xử lý khi game bắt đầu
                break;
            case "LEVEL_DATA":
                handleLevelData(parts);     // Xử lý dữ liệu màn chơi
                break;
            case "PLAYER_MOVE":
                handlePlayerMove(parts);    // Xử lý di chuyển của người chơi khác
                break;
            case "NEXT_LEVEL":
                handleNextLevel(parts);     // Xử lý chuyển màn
                break;
            case "GAME_COMPLETE":
                handleGameComplete();       // Xử lý hoàn thành game
                break;
            case "PLAYER_DISCONNECTED":
                handlePlayerDisconnected(); // Xử lý người chơi thoát
                break;
            default:
                System.out.println("Lệnh không xác định: " + command);
        }
    }
    
    /**
     * Xử lý khi vào phòng thành công
     */
    private void handleRoomJoined(String[] parts) {
        if (parts.length > 2) {
            playerType = parts[2];
            gamePanel.setPlayerType(playerType);
            String displayType = playerType.equals("FIRE") ? "Lửa" : "Nước";
            gamePanel.setStatus("Đã vào phòng! Bạn là: " + displayType);
        }
    }
    
    /**
     * Xử lý khi game bắt đầu
     */
    private void handleGameStart(String[] parts) {
        if (parts.length > 2) {
            gamePanel.setStatus("Game bắt đầu! Màn " + parts[2]);
        }
    }
    
    /**
     * Xử lý dữ liệu màn chơi từ server
     */
    private void handleLevelData(String[] parts) {
        if (parts.length > 1) {
            // Ghép lại toàn bộ dữ liệu màn chơi (có thể chứa dấu ":")
            StringBuilder levelData = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (i > 1) levelData.append(":");
                levelData.append(parts[i]);
            }
            gamePanel.loadLevel(levelData.toString());
        }
    }
    
    /**
     * Xử lý di chuyển của người chơi khác
     */
    private void handlePlayerMove(String[] parts) {
        if (parts.length > 4) {
            gamePanel.updateOtherPlayer(parts[1], 
                Integer.parseInt(parts[2]), 
                Integer.parseInt(parts[3]), 
                parts[4]);
        }
    }
    
    /**
     * Xử lý chuyển sang màn tiếp theo
     */
    private void handleNextLevel(String[] parts) {
        if (parts.length > 1) {
            int nextLevel = Integer.parseInt(parts[1]);
            gamePanel.setCurrentLevel(nextLevel);
            gamePanel.setStatus("Chuyển sang Màn " + nextLevel + "!");
        }
    }
    
    /**
     * Xử lý khi hoàn thành toàn bộ game
     */
    private void handleGameComplete() {
        gamePanel.setStatus("Chúc mừng! Hoàn thành tất cả màn chơi!");
        JOptionPane.showMessageDialog(parentFrame, 
            "Chúc mừng!\nBạn đã hoàn thành tất cả màn chơi!", 
            "Chiến thắng!", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Xử lý khi người chơi khác thoát game
     */
    private void handlePlayerDisconnected() {
        gamePanel.setStatus("Người chơi khác đã thoát");
        JOptionPane.showMessageDialog(parentFrame, 
            "Người chơi khác đã thoát khỏi game!", 
            "Thông báo", 
            JOptionPane.WARNING_MESSAGE);
    }
    
    @Override
    public void handleConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            gamePanel.setStatus("Mất kết nối với server");
            JOptionPane.showMessageDialog(parentFrame, 
                "Mất kết nối với server!", 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    public String getPlayerType() { 
        return playerType; 
    }
}
