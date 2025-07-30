package GameLobby;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import client.GameClient;
import client.NetworkManager;
import client.SoundManager;
import utils.Constants;

public class LobbyFrame extends JFrame {
    private GameClient gameClient;
    private SoundManager soundManager;
    private NetworkManager networkManager; // NetworkManager cho lobby
    private boolean isConnectedToServer = false;

    public LobbyFrame() {
        setTitle("Màn Hình Chính Game"); 
        setSize(500, 350);          
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);  
        setResizable(false);    
        soundManager = new SoundManager(); 

        // Kết nối đến server ngay khi khởi tạo lobby
        connectToServerForLobby();

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1, 10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        JButton joinGameButton = new JButton("Vào Game");
        styleButton(joinGameButton);
        joinGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openGameClient(); 
            }
        });
        panel.add(joinGameButton);

        JButton leaderboardButton = new JButton("Bảng Xếp Hạng");
        styleButton(leaderboardButton);
        leaderboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isConnectedToServer && networkManager != null) {
                    LeaderboardFrame leaderboardFrame = new LeaderboardFrame(networkManager);
                    leaderboardFrame.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(LobbyFrame.this, 
                        "Chưa kết nối tới server!\nVui lòng kiểm tra server đã chạy chưa.", 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        panel.add(leaderboardButton);

        JButton exitButton = new JButton("Thoát");
        styleButton(exitButton); 
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(LobbyFrame.this, 
                                "Bạn có chắc chắn muốn thoát khỏi game?", "Xác nhận Thoát", 
                                JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    cleanup();
                    System.exit(0);
                }
            }
        });
        panel.add(exitButton);

        // Thêm WindowListener để cleanup khi đóng cửa sổ
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanup();
                System.exit(0);
            }
        });

        add(panel);
        setVisible(true);
    }

    private void connectToServerForLobby() {
        try {
            networkManager = new NetworkManager(); // Sử dụng constructor không có handler
            if (networkManager.connect()) {
                isConnectedToServer = true;
                System.out.println("[LobbyFrame] Đã kết nối đến server thành công");
            } else {
                isConnectedToServer = false;
                System.err.println("[LobbyFrame] Không thể kết nối đến server");
                // Hiển thị thông báo nhưng không thoát app
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Không thể kết nối đến server!\n" +
                        "Bảng xếp hạng sẽ không khả dụng.\n" +
                        "Vui lòng kiểm tra server đã chạy chưa.", 
                        "Cảnh báo kết nối", 
                        JOptionPane.WARNING_MESSAGE);
                });
            }
        } catch (Exception e) {
            isConnectedToServer = false;
            System.err.println("[LobbyFrame] Lỗi khi kết nối server: " + e.getMessage());
        }
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 22));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 100, 150), 3),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
    }

    private void openGameClient() {
        System.out.println("[LobbyFrame] Bấm Vào Game");
        if (gameClient == null || !gameClient.isConnected()) {
            System.out.println("[LobbyFrame] Tạo GameClient mới");
            gameClient = new GameClient(this);
        } else {
            System.out.println("[LobbyFrame] GameClient đã tồn tại, reset trạng thái game và join lại phòng mới");

            // Reset trạng thái gamePanel (xóa dữ liệu cũ)
            gameClient.getGamePanel().resetGameState();

            // Reset mã phòng cũ
            gameClient.setCurrentRoomId("");

            // Gửi lệnh tham gia phòng mới (rỗng)
            gameClient.vaoPhong("");

            // Hiển thị lại cửa sổ gameClient
            gameClient.setVisible(true);
            gameClient.requestFocus();
        }

        // Luôn hiển thị hướng dẫn chơi mỗi lần bấm vào game
        gameClient.showInstructions();

        // Ẩn Lobby
        this.setVisible(false);
    }

    private void cleanup() {
        if (soundManager != null) {
            soundManager.stopBackgroundMusic();
        }
        if (gameClient != null) {
            gameClient.disconnectAndClose();
        }
        if (networkManager != null && isConnectedToServer) {
            networkManager.disconnect();
        }
    }

    public void showLobby() {
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LobbyFrame());
    }
}
