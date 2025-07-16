package GameLobby;

import javax.swing.*;

import client.GameClient;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class lobby extends JFrame {

    public lobby() {
        setTitle("Màn Hình Chính Game"); 
        setSize(500, 350);          
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);  
        setResizable(false);     

        // Tạo một JPanel để sắp xếp các nút
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1, 10, 20)); // GridLayout: 3 hàng, 1 cột, khoảng cách 10px ngang, 20px dọc
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100)); // Đặt padding cho panel

        // --- Tạo nút "Vào Game" ---
        JButton joinGameButton = new JButton("Vào Game");
        styleButton(joinGameButton); // Áp dụng style chung cho nút
        joinGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new GameClient();
                setVisible(false);
            }
        });
        panel.add(joinGameButton);

        // --- Tạo nút "Bảng Xếp Hạng" ---
        JButton leaderboardButton = new JButton("Bảng Xếp Hạng");
        styleButton(leaderboardButton);
        leaderboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(lobby.this , "Đang hiển thị Bảng Xếp Hạng...");
            }
        });
        panel.add(leaderboardButton);

        // --- Tạo nút "Thoát" ---
        JButton exitButton = new JButton("Thoát");
        styleButton(exitButton); 
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Logic khi nhấn nút "Thoát"
                int confirm = JOptionPane.showConfirmDialog(lobby.this, 
                                "Bạn có chắc chắn muốn thoát khỏi game?", "Xác nhận Thoát", 
                                JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0); // Thoát ứng dụng
                }
            }
        });
        panel.add(exitButton);

        // Thêm panel chứa các nút vào JFrame
        add(panel);

        // Hiển thị cửa sổ
        setVisible(true);
    }

    /**
     * Hàm trợ giúp để thiết lập kiểu cho các nút.
     */
    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 22)); // Font và kích thước chữ
        button.setBackground(new Color(70, 130, 180));   // Màu nền (Steel Blue)
        button.setForeground(Color.WHITE);               // Màu chữ
        button.setFocusPainted(false);                   // Tắt viền focus khi click
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 100, 150), 3), // Viền ngoài
            BorderFactory.createEmptyBorder(10, 20, 10, 20) // Padding bên trong nút
        ));
    }

    public static void main(String[] args) {
        // Đảm bảo rằng việc tạo và hiển thị GUI diễn ra trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new lobby();
            }
        });
    }
}