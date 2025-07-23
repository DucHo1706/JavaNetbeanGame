package GameLobby;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

public class LeaderboardFrame extends JFrame {
    private JTextArea leaderboardArea;
    private BufferedReader in;
    private PrintWriter out;

    public LeaderboardFrame(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;

        setTitle("Bảng Xếp Hạng");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JScrollPane(leaderboardArea), BorderLayout.CENTER);

        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        try {
            out.println("LAY_BANG_XEP_HANG");
            String response = in.readLine();
            if (response != null && response.startsWith("BANG_XEP_HANG:")) {
                String data = response.substring("BANG_XEP_HANG:".length());
                String[] entries = data.split(";");
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-15s %s\n", "Người chơi", "Thời gian (giây)"));
                sb.append("--------------------------------\n");
                for (String entry : entries) {
                    if (!entry.trim().isEmpty()) {
                        String[] parts = entry.split(",");
                        if (parts.length == 2) {
                            String player = parts[0];
                            String time = parts[1];
                            sb.append(String.format("%-15s %s\n", player, time));
                        }
                    }
                }
                leaderboardArea.setText(sb.toString());
            } else {
                leaderboardArea.setText("Không nhận được dữ liệu bảng xếp hạng từ server.");
            }
        } catch (IOException e) {
            leaderboardArea.setText("Lỗi khi lấy bảng xếp hạng: " + e.getMessage());
        }
    }
}
