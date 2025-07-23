package GameLobby;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;

public class LeaderboardUtils {

    private static final String LEADERBOARD_FILE = "data/leaderboard.txt";

    // Ghi bảng xếp hạng vào file
    public static void saveLeaderboardToFile(Map<String, Integer> rankings) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LEADERBOARD_FILE))) {
            for (Map.Entry<String, Integer> entry : rankings.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
            System.out.println("Đã lưu bảng xếp hạng vào file " + LEADERBOARD_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Đọc bảng xếp hạng từ file
    public static Map<String, Integer> loadLeaderboardFromFile() {
        Map<String, Integer> rankings = new TreeMap<>();
        try (Scanner scanner = new Scanner(new File(LEADERBOARD_FILE))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String player = parts[0];
                    int score = Integer.parseInt(parts[1]);
                    rankings.put(player, score);
                }
            }
            System.out.println("Đã đọc bảng xếp hạng từ file " + LEADERBOARD_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("Không tìm thấy file bảng xếp hạng, sẽ tạo mới.");
        }
        return rankings;
    }
}
