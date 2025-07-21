package GameLobby;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class LeaderboardFrame extends JFrame {

    private JTextArea leaderboardArea;

    public LeaderboardFrame() {
        setTitle("Bảng Xếp Hạng");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

       leaderboardArea = new JTextArea();
    leaderboardArea.setEditable(false);
    leaderboardArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    add(new JScrollPane(leaderboardArea), BorderLayout.CENTER);

    // Đọc bảng xếp hạng từ file
    Map<String, Integer> rankings = LeaderboardUtils.loadLeaderboardFromFile();
    updateLeaderboard(rankings);
    }

    public void updateLeaderboard(Map<String, Integer> rankings) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s %s\n", "Người chơi", "Thời gian (giây)"));
        sb.append("--------------------------------\n");
        rankings.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> sb.append(String.format("%-15s %d\n", entry.getKey(), entry.getValue())));
        leaderboardArea.setText(sb.toString());
    }
}
