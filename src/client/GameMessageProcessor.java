package client;

import javax.swing.*;
import utils.Constants;

public class GameMessageProcessor implements IMessageHandler {
    private GamePanel gamePanel;
    private JFrame parentFrame;
    private String playerType;
    private GameClient gameClient;

    public GameMessageProcessor(GamePanel gamePanel, JFrame parentFrame) {
        this.gamePanel = gamePanel;
        this.parentFrame = parentFrame;
        if (parentFrame instanceof GameClient) {
            this.gameClient = (GameClient) parentFrame;
        }
    }

    @Override
    public void handleMessage(String message) {
        SwingUtilities.invokeLater(() -> processMessage(message));
    }

    private void processMessage(String message) {
        String[] parts = message.split(":");
        String command = parts[0];

        switch (command) {
            case Constants.ROOM_DA_THAM_GIA:
                handleRoomJoined(parts);
                break;
            case Constants.CHO_DOI_NGUOI_CHOI:
                gamePanel.setStatus("Đang chờ người chơi khác...");
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
                gamePanel.batDauVongMoi();
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
                break;
            case "YEU_CAU_SAN_SANG":
                gamePanel.showReadyButton(true);
                break;
            case "NGUOI_CHOI_SAN_SANG":
                if (parts.length > 1) {
                    String playerId = parts[1];
                    gamePanel.updateReadyStatus(playerId);
                }
                break;
            case Constants.NUT_BAT_DAU:
                handleRoomJoined(parts);
                break;
                case "PHONG_KHONG_HOAT_DONG":
            JOptionPane.showMessageDialog(parentFrame,
                "Phòng đã thiếu người chơi, bạn sẽ trở về Lobby.",
                "Thông báo",
                JOptionPane.WARNING_MESSAGE);
            if (gameClient != null) {
                gameClient.leaveRoomAndBackToLobby();
            }
            break;
            default:
                System.out.println("Lệnh không xác định: " + command);
        }
    }

   private void handleRoomJoined(String[] parts) {
    gamePanel.resetGameState();
    if (parts.length > 2) {
        String roomId = parts[1];
        playerType = parts[2];
        gamePanel.setPlayerType(playerType);
        String displayType = playerType.equals("FIRE") ? "Lửa" : "Nước";
        gamePanel.setStatus("Đã vào phòng: " + roomId + " - Bạn là: " + displayType);

        // Lưu mã phòng vào GameClient để có thể vào lại phòng cũ
        if (gameClient != null) {
            gameClient.setCurrentRoomId(roomId);  // Gọi hàm setCurrentRoomId để cập nhật title
        }
    } else {
        gamePanel.setStatus("Đã vào phòng, chờ người chơi khác...");
    }
}


    private void handleGameStart(String[] parts) {
        if (parts.length > 2) {
            gamePanel.setStatus("Game bắt đầu! Màn " + parts[2]);
        }
    }

    private void handleLevelData(String[] parts) {
        if (parts.length > 1) {
            StringBuilder levelData = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (i > 1) levelData.append(":");
                levelData.append(parts[i]);
            }
            gamePanel.loadLevel(levelData.toString());
        }
    }

    private void handlePlayerMove(String[] parts) {
        if (parts.length > 4) {
            gamePanel.updateOtherPlayer(parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    parts[4]);
        }
    }

    private void handleNextLevel(String[] parts) {
        if (parts.length > 1) {
            int nextLevel = Integer.parseInt(parts[1]);
            gamePanel.setCurrentLevel(nextLevel);
            gamePanel.setStatus("Chuyển sang Màn " + nextLevel + "!");
        }
    }

    private void handleGameComplete() {
        gamePanel.setStatus("Chúc mừng! Hoàn thành tất cả màn chơi!");
        JOptionPane.showMessageDialog(parentFrame,
                "Chúc mừng!\nBạn đã hoàn thành tất cả màn chơi!",
                "Chiến thắng!",
                JOptionPane.INFORMATION_MESSAGE);
    }

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
