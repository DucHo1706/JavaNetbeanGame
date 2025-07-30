package GameLobby;

import javax.swing.*;
import java.awt.*;
import client.NetworkManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LeaderboardFrame extends JFrame {
    private JTextArea leaderboardArea;
    private NetworkManager networkManager;
    private volatile String receivedData = null;

    public LeaderboardFrame(NetworkManager networkManager) {
        this.networkManager = networkManager;

        setTitle("Bảng Xếp Hạng");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initializeComponents();
        fetchLeaderboard();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // Tiêu đề
        JLabel titleLabel = new JLabel("BẢNG XẾP HẠNG THỜI GIAN CHƠI");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Khu vực hiển thị bảng xếp hạng
        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        leaderboardArea.setBackground(Color.WHITE);
        leaderboardArea.setText("Đang tải bảng xếp hạng...");
        
        JScrollPane scrollPane = new JScrollPane(leaderboardArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Danh sách"));
        add(scrollPane, BorderLayout.CENTER);

        // Nút làm mới
        JPanel buttonPanel = new JPanel();
        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(e -> fetchLeaderboard());
        buttonPanel.add(refreshButton);
        
        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void fetchLeaderboard() {
        leaderboardArea.setText("Đang tải bảng xếp hạng...");
        receivedData = null;
        
        // Thiết lập message listener để nhận phản hồi
        networkManager.setMessageListener(this::handleServerMessage);
        
        // Gửi yêu cầu lấy bảng xếp hạng
        networkManager.sendMessage("LAY_BANG_XEP_HANG");
        
        // Đợi phản hồi trong 5 giây
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            if (receivedData == null) {
                SwingUtilities.invokeLater(() -> {
                    leaderboardArea.setText("Timeout: Không nhận được phản hồi từ server trong 5 giây.\n\n" +
                                          "Vui lòng thử lại hoặc kiểm tra kết nối server.");
                });
            }
        });
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("BANG_XEP_HANG:")) {
            receivedData = message;
            SwingUtilities.invokeLater(() -> {
                displayLeaderboard(message.substring("BANG_XEP_HANG:".length()));
            });
        }
    }

    private void displayLeaderboard(String data) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-5s %-20s %s\n", "Hạng", "Người chơi", "Thời gian (giây)"));
            sb.append("─".repeat(50)).append("\n");

            if (data.trim().isEmpty()) {
                sb.append("Chưa có dữ liệu bảng xếp hạng.");
            } else {
                String[] entries = data.split(";");
                int rank = 1;
                
                for (String entry : entries) {
                    if (!entry.trim().isEmpty()) {
                        String[] parts = entry.split(",");
                        if (parts.length == 2) {
                            String player = parts[0];
                            String time = parts[1];
                            sb.append(String.format("%-5d %-20s %s\n", rank++, player, time));
                        }
                    }
                }
            }
            
            leaderboardArea.setText(sb.toString());
            
        } catch (Exception e) {
            leaderboardArea.setText("Lỗi khi hiển thị bảng xếp hạng: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        // Reset message listener khi đóng cửa sổ
        if (networkManager != null) {
            networkManager.setMessageListener(null);
        }
        super.dispose();
    }
}
