package client;

import utils.Constants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;

public class GameClient extends JFrame implements KeyListener {
    private Socket socket; // kết nối đến server
    private PrintWriter out; // gửi dữ liệu đến sever
    private BufferedReader in; //  nhận dữ liệu từ server
    private GamePanel gamePanel; // hiển thị trò chơi 
    private String playerId;
    private String playerType;
    private boolean connected = false; // trạng thái kết nối
    
    public GameClient() {
        playerId = "Player_" + System.currentTimeMillis();
        initializeGUI(); //giao diện cửa sổ game , thiếc lập các nút sự kiện
        connectToServer(); // tạo socket kết nối đến sever
    }
    
    private void initializeGUI() {
        setTitle(" Trò chơi lửa và nước - " + playerId);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        
        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        addKeyListener(this);
        setFocusable(true);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        // Show instructions
        showInstructions();
    }
    
    private void showInstructions() {
        String instructions = " HƯỚNG DẪN CHƠI:\n\n" +
                "• Sử dụng WASD hoặc phím mũi tên để di chuyển\n" +
                "•  Fire (Đỏ) và  Water (Xanh) phải hợp tác\n" +
                "• Cả hai phải đến cửa (Vàng) cùng lúc để qua màn\n" +
                "• Tránh các bức tường (Nâu)\n" +
                "• Có 3 màn chơi với độ khó tăng dần\n\n" +
                "Nhấn OK để bắt đầu!";
        
        JOptionPane.showMessageDialog(this, instructions, "Trò chơi lửa nước", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            gamePanel.setStatus("Đã kết nối! Đang tham gia game...");
            
            // Gửi yêu cầu join game
            out.println("JOIN:" + playerId); // "JOIN" là thông tin cần thiết để sử dụng
            
            // Lắng nghe tin nhắn từ server
            new Thread(this::listenToServer).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Không thể kết nối đến server!\nVui lòng kiểm tra server đã chạy chưa.", 
                "Lỗi kết nối", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    // lắng nghe từ server 
    private void listenToServer() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println(" DEBUG: Received message: " + message);
                
                final String finalMessage = message;
                final String[] parts = finalMessage.split(":");
                final String command = parts[0];
                
                SwingUtilities.invokeLater(() -> {
                    switch (command) {
                        case "ROOM_JOINED":
                            if (parts.length > 2) {
                                playerType = parts[2];
                                gamePanel.setPlayerType(playerType);
                                gamePanel.setStatus("Đã vào phòng! Bạn là: " + 
                                    (playerType.equals("FIRE") ? " Fire" : " Water"));
                                System.out.println(" Player type set: " + playerType);
                            }
                            break;
                            
                        case "WAITING_FOR_PLAYER":
                            gamePanel.setStatus(" Đang chờ người chơi khác...");
                            System.out.println(" Waiting for another player...");
                            break;
                            
                        case "GAME_START":
                            if (parts.length > 2) {
                                gamePanel.setStatus(" Game bắt đầu! Level " + parts[2]);
                                System.out.println(" Game started - Level " + parts[2]);
                            }
                            break;
                            
                        case "LEVEL_DATA":
                            System.out.println(" DEBUG: Processing LEVEL_DATA");
                            if (parts.length > 1) {
                                // GỬI TOÀN BỘ DATA CHỨ KHÔNG CHỈ parts[1]
                                StringBuilder levelData = new StringBuilder();
                                for (int i = 1; i < parts.length; i++) {
                                    if (i > 1) levelData.append(":");
                                    levelData.append(parts[i]);
                                }
                                String fullLevelData = levelData.toString();
                                System.out.println(" Full level data: " + fullLevelData);
                                gamePanel.loadLevel(fullLevelData);
                                System.out.println(" loadLevel() called successfully");
                            } else {
                                System.out.println(" LEVEL_DATA message malformed: " + finalMessage);
                            }
                            break;
                            
                        case "PLAYER_MOVE":
                            if (parts.length > 4) {
                                gamePanel.updateOtherPlayer(parts[1], 
                                    Integer.parseInt(parts[2]), 
                                    Integer.parseInt(parts[3]), 
                                    parts[4]);
                            }
                            break;
                            
                    case "NEXT_LEVEL":
                        if (parts.length > 1) {
                            int nextLevel = Integer.parseInt(parts[1]);
                            gamePanel.setCurrentLevel(nextLevel);
                            gamePanel.setStatus(" Chuyển sang Level " + nextLevel + "!");
                            System.out.println(" Moving to Level " + nextLevel);
                        }
                        break;
                            
                        case "GAME_COMPLETE":
                            gamePanel.setStatus(" Chúc mừng! Hoàn thành tất cả màn chơi!");
                            JOptionPane.showMessageDialog(this, 
                                " Chúc mừng!\nBạn đã hoàn thành tất cả màn chơi!", 
                                "Thắng rồi!", 
                                JOptionPane.INFORMATION_MESSAGE);
                            break;
                            
                        case "PLAYER_DISCONNECTED":
                            gamePanel.setStatus(" Người chơi khác đã thoát");
                            JOptionPane.showMessageDialog(this, 
                                "Người chơi khác đã thoát khỏi game!", 
                                "Thông báo", 
                                JOptionPane.WARNING_MESSAGE);
                            break;
                            
                            
                        default:
                            System.out.println("️ Unknown command: " + command);
                            break;
                            
                    }
                });
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    gamePanel.setStatus(" Mất kết nối với server");
                    JOptionPane.showMessageDialog(this, 
                        "Mất kết nối với server!", 
                        "Lỗi", 
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }
    }
    
    private void disconnect() {
        connected = false;
        if (out != null) {
            out.println("DISCONNECT");
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (!connected || playerType == null) {
            System.out.println(" Cannot move: connected=" + connected + ", playerType=" + playerType);
            return;
        }
        
        int keyCode = e.getKeyCode();
        String direction = "";
        
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                direction = "UP";
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                direction = "DOWN";
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                direction = "LEFT";
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                direction = "RIGHT";
                break;
        }
        
        if (!direction.isEmpty()) {
            System.out.println(" Key pressed: " + direction);
            boolean moved = gamePanel.movePlayer(direction);
            if (moved) {
                Point playerPos = gamePanel.getPlayerPosition();
                String moveMessage = "MOVE:" + playerPos.x + ":" + playerPos.y + ":" + direction;
                System.out.println(" Sending: " + moveMessage);
                out.println(moveMessage);
            } else {
                System.out.println(" Move blocked");
            }
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
