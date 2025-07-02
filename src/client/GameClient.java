package client;

import utils.Constants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Client chính của trò chơi Lửa và Nước
 */
public class GameClient extends JFrame implements KeyListener {
    private NetworkManager networkManager;          // Quản lý kết nối mạng
    private GameMessageProcessor messageProcessor;  // Xử lý tin nhắn game
    private GamePanel gamePanel;                   // Bảng hiển thị game
    private String playerId;                       // ID người chơi
    
    public GameClient() {
        playerId = "Player_" + System.currentTimeMillis();
        initializeComponents();
        initializeGUI();
        connectToServer();
    }
    
    /**
     * Khởi tạo các thành phần chính
     */
    private void initializeComponents() {
        gamePanel = new GamePanel();
        messageProcessor = new GameMessageProcessor(gamePanel, this);
        networkManager = new NetworkManager(messageProcessor);
    }
    
    /**
     * Khởi tạo giao diện người dùng
     */
    private void initializeGUI() {
        setTitle("Trò chơi Lửa và Nước - " + playerId);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        
        // Xử lý khi đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                networkManager.disconnect();
                System.exit(0);
            }
        });
        
        add(gamePanel);
        addKeyListener(this);
        setFocusable(true);
        
        pack();
        setLocationRelativeTo(null);  // Căn giữa màn hình
        setVisible(true);
        
        showInstructions();
    }
    
    /**
     * Hiển thị hướng dẫn chơi game
     */
    private void showInstructions() {
        String instructions = "HƯỚNG DẪN CHƠI:\n\n" +
                "• Sử dụng WASD hoặc phím mũi tên để di chuyển\n" +
                "• Lửa (Đỏ) và Nước (Xanh) phải hợp tác\n" +
                "• Cả hai phải đến cửa (Vàng) cùng lúc để qua màn\n" +
                "• Tránh các bức tường (Nâu)\n" +
                "• Có 3 màn chơi với độ khó tăng dần\n\n" +
                "Nhấn OK để bắt đầu!";
        
        JOptionPane.showMessageDialog(this, instructions, 
            "Trò chơi Lửa và Nước", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Kết nối đến server
     */
    private void connectToServer() {
        if (networkManager.connect()) {
            gamePanel.setStatus("Đã kết nối! Đang tham gia game...");
            networkManager.sendMessage("JOIN:" + playerId);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Không thể kết nối đến server!\nVui lòng kiểm tra server đã chạy chưa.", 
                "Lỗi kết nối", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        // Kiểm tra kết nối và loại người chơi trước khi di chuyển
        if (!networkManager.isConnected() || messageProcessor.getPlayerType() == null) {
            return;
        }
        
        String direction = getDirectionFromKey(e.getKeyCode());
        if (!direction.isEmpty()) {
            boolean moved = gamePanel.movePlayer(direction);
            if (moved) {
                // Gửi thông tin di chuyển đến server
                Point playerPos = gamePanel.getPlayerPosition();
                String moveMessage = "MOVE:" + playerPos.x + ":" + playerPos.y + ":" + direction;
                networkManager.sendMessage(moveMessage);
            }
        }
    }
    
    /**
     * Chuyển đổi mã phím thành hướng di chuyển
     */
    private String getDirectionFromKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return "UP";
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return "DOWN";
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return "LEFT";
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return "RIGHT";
            default:
                return "";
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient());
    }
}
