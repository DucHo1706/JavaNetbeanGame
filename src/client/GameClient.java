package client;

import utils.Constants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import GameLobby.LobbyFrame;
import client.SoundManager;

public class GameClient extends JFrame implements KeyListener {
    private NetworkManager networkManager;
    private GameMessageProcessor messageProcessor;
    private GamePanel gamePanel;
    private String playerId;
    private LobbyFrame lobbyFrame;
    private String currentRoomId = "";
    private SoundManager soundManager;

    public GameClient(LobbyFrame lobbyFrame) {
        this.lobbyFrame = lobbyFrame;
        // playerId nên là duy nhất và có thể được xác định (ví dụ: từ một màn hình đăng nhập)
        // Hiện tại dùng System.currentTimeMillis() để tạo ID,
        // nếu muốn chỉ 1 client/player, bạn cần một cơ chế ID bền vững hơn
        playerId = "Player_" + System.currentTimeMillis(); 
        soundManager = new SoundManager();
        initializeComponents();
        initializeGUI();
        connectToServer(); 
    }

    private void initializeComponents() {
        gamePanel = new GamePanel();
        messageProcessor = new GameMessageProcessor(gamePanel, this);
        networkManager = new NetworkManager(messageProcessor);
        gamePanel.setNetworkManager(networkManager);
    }

    private void initializeGUI() {
        setTitle("Trò chơi Lửa và Nước - " + playerId);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectAndClose();
            }
        });

        JButton btnBack = new JButton("Quay lại");
        btnBack.addActionListener(e -> {
               int confirm = JOptionPane.showConfirmDialog(this,
               "Bạn có chắc chắn muốn rời phòng và quay lại Lobby?",
               "Xác nhận", JOptionPane.YES_NO_OPTION);
           if (confirm == JOptionPane.YES_OPTION) {
               leaveRoomAndBackToLobby();
           }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnBack);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        addKeyListener(this);
        setFocusable(true);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

       // showInstructions();
    }

    public void showInstructions() {
        String instructions = "HƯỚNG DẪN CHƠI:\n\n" +
                "• Sử dụng WASD hoặc phím mũi tên để di chuyển\n" +
                "• Lửa (Đỏ) và Nước (Xanh) phải hợp tác\n" +
                "• Cả hai phải đến cửa (Vàng) cùng lúc để qua màn\n" +
                "• Tránh các bức tường (Nâu)\n" +
                "• Có 3 màn chơi với độ khó tăng dần\n\n" +
                "Nhấn OK để bắt đầu!";

        JOptionPane.showMessageDialog(this, instructions, "Trò chơi Lửa và Nước", JOptionPane.INFORMATION_MESSAGE);
    }

    private void connectToServer() {
        if (networkManager.connect()) {
            gamePanel.setStatus("Đã kết nối! Đang tham gia game...");
            currentRoomId = ""; // Reset phòng khi kết nối mới
            System.out.println("[GameClient] Kết nối server thành công, gửi tham gia phòng rỗng");
            networkManager.sendMessage(Constants.THAM_GIA + ":" + playerId + ":" + currentRoomId);
        } else {
            JOptionPane.showMessageDialog(this,
                "Không thể kết nối đến server!\nVui lòng kiểm tra server đã chạy chưa.",
                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public void vaoPhong(String maPhong) {
        currentRoomId = maPhong == null ? "" : maPhong;
        updateWindowTitle();
        System.out.println("Đang gửi lệnh THAM_GIA với mã phòng: '" + currentRoomId + "'");
        networkManager.sendMessage(Constants.THAM_GIA + ":" + playerId + ":" + currentRoomId);
    }

    public void leaveRoomAndBackToLobby() {
        if (networkManager.isConnected()) {
            networkManager.sendMessage(Constants.RROI_PHONG + ":" + playerId);
        }
        currentRoomId = "";
        setVisible(false);
        if (lobbyFrame != null) {
            lobbyFrame.showLobby();
        }   
    }

    public void disconnectAndClose() {
        if (networkManager.isConnected()) {
            networkManager.sendMessage(Constants.RROI_PHONG + ":" + playerId);
            networkManager.disconnect();
        }
        currentRoomId = "";
        dispose();
        if (lobbyFrame != null) {
            lobbyFrame.showLobby();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Đảm bảo đã kết nối và loại người chơi đã được xác định
        if (!networkManager.isConnected() || messageProcessor.getPlayerType() == null) {
            return;
        }

        String direction = getDirectionFromKey(e.getKeyCode());
        if (!direction.isEmpty()) {
            boolean moved = gamePanel.movePlayer(direction);
            if (moved) {
                Point playerPos = gamePanel.getPlayerPosition();
                String moveMessage = Constants.DI_CHUYEN + ":" + playerPos.x + ":" + playerPos.y + ":" + direction;
                networkManager.sendMessage(moveMessage);

                // Phát âm thanh di chuyển chung cho cả hai nhân vật
                // Đảm bảo bạn có file 'move_sound.wav' trong thư mục /sounds/
            }
        }
    }

    private String getDirectionFromKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return Constants.UP;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return Constants.DOWN;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return Constants.LEFT;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return Constants.RIGHT;
            default:
                return "";
        }
    }

    public void updateWindowTitle() {
        String roomId = currentRoomId == null || currentRoomId.isEmpty() ? "Chưa vào phòng" : currentRoomId;
        setTitle("Trò chơi Lửa và Nước - " + playerId + " | Phòng: " + roomId);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}

    public BufferedReader getIn() {
        return networkManager.getBufferedReader();
    }

    public PrintWriter getOut() {
        return networkManager.getPrintWriter();
    }

    public boolean isConnected() {
        return networkManager.isConnected();
    }

    public void setCurrentRoomId(String roomId) {
        this.currentRoomId = roomId;
        updateWindowTitle();
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }
}