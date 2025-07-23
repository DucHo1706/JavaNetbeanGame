package GameLobby;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import client.GameClient;

public class LobbyFrame extends JFrame {
    private GameClient gameClient;

    public LobbyFrame() {
        setTitle("Màn Hình Chính Game"); 
        setSize(500, 350);          
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);  
        setResizable(false);     

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
                if (gameClient != null && gameClient.isConnected()) {
                    LeaderboardFrame leaderboardFrame = new LeaderboardFrame(gameClient.getIn(), gameClient.getOut());
                    leaderboardFrame.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(LobbyFrame.this, "Chưa kết nối tới server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
                    if (gameClient != null) {
                        gameClient.disconnectAndClose();
                    }
                    System.exit(0);
                }
            }
        });
        panel.add(exitButton);

        add(panel);
        setVisible(true);
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





    public void showLobby() {
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LobbyFrame());
    }
}
    