package client;

import javax.swing.*;
import utils.Constants;

/**
 * Xu ly cac tin nhan lien quan den game tu server
 */
public class GameMessageProcessor implements IMessageHandler {
    private GamePanel gamePanel;        // Bang hien thi game
    private JFrame parentFrame;         // Cua so chinh
    private String playerType;          // Loai nguoi choi (FIRE/WATER)
    
    public GameMessageProcessor(GamePanel gamePanel, JFrame parentFrame) {
        this.gamePanel = gamePanel;
        this.parentFrame = parentFrame;
    }
    
    @Override
    public void handleMessage(String message) {
        SwingUtilities.invokeLater(() -> processMessage(message));
    }
    
    /**
     * Phan tich va xu ly tin nhan tu server
     */
    private void processMessage(String message) {
        String[] parts = message.split(":");
        String command = parts[0];
        
        switch (command) {
         case Constants.ROOM_DA_THAM_GIA:
             handleRoomJoined(parts);
             break;
         case Constants.CHO_DOI_NGUOI_CHOI:
             gamePanel.setStatus("Dang cho nguoi choi khac...");
             break;
         case Constants.THAM_GIA:
             handleGameStart(parts);
             break;
         case "LEVEL_DATA": 
             handleLevelData(parts);
             break;
         case Constants.DI_CHUYEN:
             handlePlayerMove(parts);
             break;
         case Constants.CAP_NHAT_CAP_DO_TIEP_THEO:
             handleNextLevel(parts);
             break;
         case Constants.GAME_HOAN_THANH:
             handleGameComplete();
             break;
         case Constants.NGAT_KET_NOI:
             handlePlayerDisconnected();
             break;
               case Constants.BAT_DAU_TRO_CHOI:
            handleGameStart(parts);
            break;
        case Constants.DU_LIEU_CAP_DO:
            handleLevelData(parts);
            break;
        case Constants.NGUOI_CHOI_DI_CHUYEN:
            handlePlayerMove(parts);
            break;
        case Constants.NGUOI_CHOI_NGAT_KET_NOI:
            handlePlayerDisconnected();
            break;
             case "THOI_GIAN_CON_LAI":
                if (parts.length > 1) {
                    try {
                        int giayConLai = Integer.parseInt(parts[1]);
                        gamePanel.updateTimeRemaining(giayConLai);
                    } catch (NumberFormatException e) {
                        System.err.println("Dữ liệu thời gian không hợp lệ: " + parts[1]);
                    }
                }
                break;

            case "MAN_KET_THUC":
                gamePanel.setStatus("Màn chơi đã kết thúc!");
                JOptionPane.showMessageDialog(parentFrame,
                        "Màn chơi đã kết thúc do hết giờ!",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                break;
        
        // Xử lý logic ở game lobby
        // Khi người dùng ấn bắt đầu thì sẽ cho join phòng

        case Constants.NUT_BAT_DAU:
                handleRoomJoined(parts);
                break;
         default:
             System.out.println("Lenh khong xac dinh: " + command);
        }
    }
    
    /**
     * Xu ly khi vao phong thanh cong
     */
    private void handleRoomJoined(String[] parts) {
        String messageString = "Đang đợi người chơi khác kết nối";
        gamePanel.setStatus(messageString);
        if (parts.length > 2) {
            playerType = parts[2];
            gamePanel.setPlayerType(playerType);
            String displayType = playerType.equals("FIRE") ? "Lua" : "Nuoc";
            gamePanel.setStatus("Da vao phong! Ban la: " + displayType);
        }
    }
    
    /**
     * Xu ly khi game bat dau
     */
    private void handleGameStart(String[] parts) {
        if (parts.length > 2) {
            gamePanel.setStatus("Game bat dau! Man " + parts[2]);
        }
    }
    
    /**
     * Xu ly du lieu man choi tu server
     */
    private void handleLevelData(String[] parts) {
        if (parts.length > 1) {
            // Ghep lai toan bo du lieu man choi (co the chua dau ":")
            StringBuilder levelData = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (i > 1) levelData.append(":");
                levelData.append(parts[i]);
            }
            gamePanel.loadLevel(levelData.toString());
        }
    }
    
    /**
     * Xu ly di chuyen cua nguoi choi khac
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
     * Xu ly chuyen sang man tiep theo
     */
    private void handleNextLevel(String[] parts) {
        if (parts.length > 1) {
            int nextLevel = Integer.parseInt(parts[1]);
            gamePanel.setCurrentLevel(nextLevel);
            gamePanel.setStatus("Chuyen sang Man " + nextLevel + "!");
        }
    }
    
    /**
     * Xu ly khi hoan thanh toan bo game
     */
    private void handleGameComplete() {
        gamePanel.setStatus("Chuc mung! Hoan thanh tat ca man choi!");
        JOptionPane.showMessageDialog(parentFrame, 
            "Chuc mung!\nBan da hoan thanh tat ca man choi!", 
            "Chien thang!", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Xu ly khi nguoi choi khac thoat game
     */
    private void handlePlayerDisconnected() {
        gamePanel.setStatus("Nguoi choi khac da thoat");
        JOptionPane.showMessageDialog(parentFrame, 
            "Nguoi choi khac da thoat khoi game!", 
            "Thong bao", 
            JOptionPane.WARNING_MESSAGE);
    }
    
    @Override
    public void handleConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            gamePanel.setStatus("Mat ket noi voi server");
            JOptionPane.showMessageDialog(parentFrame, 
                "Mat ket noi voi server!", 
                "Loi", 
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    public String getPlayerType() { 
        return playerType; 
    }
}