package server;

import game.Monster;
import utils.Constants;
import java.io.*;
import utils.PlayerType;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;

public class GameRoom {
    private String roomId;
    private Map<String, Socket> players;
    private Map<String, PrintWriter> playerWriters;
    private Map<String, PlayerType> playerTypes; // "FIRE" hoặc "WATER"
    private Map<String, Point> playerPositions; //  Lưu vị trí players
    private int currentLevel;
    private boolean gameStarted;
    private Set<String> playersAtDoor;
    private Level currentLevelData; // Lưu data level hiện tại
    
    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.players = new ConcurrentHashMap<>();
        this.playerWriters = new ConcurrentHashMap<>();
        this.playerTypes = new ConcurrentHashMap<>();
        this.playerPositions = new ConcurrentHashMap<>(); // 
        this.currentLevel = 1;
        this.gameStarted = false;
        this.playersAtDoor = new HashSet<>();
    }
    
public PlayerType addPlayer(Socket socket, String playerId) { 
    players.put(playerId, socket);
    try {
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        playerWriters.put(playerId, writer);
    } catch (IOException e) {
        e.printStackTrace();
    }
    
    // SỬA: Dùng PlayerType enum thay vì hardcode string
    PlayerType playerType = players.size() == 1 ? PlayerType.FIRE : PlayerType.WATER;
    playerTypes.put(playerId, playerType);
    
    // Set vị trí ban đầu
    if (currentLevelData != null) {
        if (PlayerType.WATER.equals(playerType)) {
            playerPositions.put(playerId, new Point(currentLevelData.getWaterStart()));
        } else {
            playerPositions.put(playerId, new Point(currentLevelData.getFireStart()));
        }
    }
    
    System.out.println("Player " + playerId + " joined as " + playerType);
    return playerType;
}

    
    public void removePlayer(String playerId) {
        players.remove(playerId);
        playerWriters.remove(playerId);
        playerTypes.remove(playerId);
        playerPositions.remove(playerId); 
        playersAtDoor.remove(playerId);
        
        // Thông báo cho player còn lại
        if (players.size() == 1) {
            broadcastToAll("PLAYER_DISCONNECTED:" + playerId);
        }
    }
    
    public void startGame() {
        // bắt đầu trò chơi
        gameStarted = true;
        currentLevelData = createLevel(currentLevel);
        System.out.println("Starting game in room " + roomId + " - Level " + currentLevel);
        broadcastToAll("GAME_START:LEVEL:" + currentLevel);
        sendLevelData();
        
        // Set vị trí ban đầu cho players
        resetPlayerPositions();
    }
    
   public void updatePlayerPosition(String playerId, int x, int y, String direction) {
    if (gameStarted) {
        //  Lưu vị trí mới
        playerPositions.put(playerId, new Point(x, y));
        // cập nhật lại vị trí
        broadcastToOthers(playerId, "PLAYER_MOVE:" + playerId + ":" + x + ":" + y + ":" + direction);
        
        //  Kiểm tra door với currentLevelData
        if (currentLevelData != null) {
            Point doorPos = currentLevelData.getDoor();
            System.out.println("? Player " + playerId + " at (" + x + "," + y + "), Door at " + doorPos);
            
            if (doorPos != null && doorPos.x == x && doorPos.y == y) {
                playersAtDoor.add(playerId);
                System.out.println("? Player " + playerId + " reached the door! Players at door: " + playersAtDoor.size());
                
                // Kiểm tra xem cả 2 player đã ở door chưa
                if (playersAtDoor.size() == 2) {
                    System.out.println("? Both players at door! Level complete!");
                    nextLevel();
                }
            } else {
                playersAtDoor.remove(playerId);
            }
        }
    }
}

    public void nextLevel() {
        playersAtDoor.clear();
        currentLevel++;
        
        if (currentLevel <= Constants.TOTAL_LEVELS) {
            currentLevelData = createLevel(currentLevel); 
            System.out.println("Room " + roomId + " advancing to level " + currentLevel);
            broadcastToAll("NEXT_LEVEL:" + currentLevel);
            sendLevelData();
            resetPlayerPositions(); //  Reset vị trí
        } else {
            System.out.println("Room " + roomId + " completed all levels!");
            broadcastToAll("GAME_COMPLETE");
            gameStarted = false;
        }
    }
    // gửi dữ liệu level
    private void sendLevelData() {
        String levelData = generateLevelData(currentLevel);
        broadcastToAll("LEVEL_DATA:" + levelData);
        System.out.println("Sent level " + currentLevel + " data to room " + roomId);
    }
    
private String generateLevelData(int level) {
    switch (level) {
        case 1:
            // LEVEL 1: Maze thú vị hơn 24x16 với đường đi rõ ràng
            return "SIMPLE:24x16:WALLS:" +
                   // Outer walls (chỉ 3 mặt, để lại lối thoát ở dưới)
                   "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" +
                   "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" +
                   "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" +
                   // Inner maze - tạo đường đi thú vị
                   "2,2;3,2;4,2;5,2;6,2;8,2;9,2;10,2;12,2;13,2;14,2;16,2;17,2;18,2;20,2;21,2;" +
                   "2,3;6,3;10,3;14,3;18,3;21,3;" +
                   "2,4;3,4;6,4;7,4;10,4;11,4;14,4;15,4;18,4;19,4;21,4;" +
                   "6,5;11,5;15,5;19,5;" +
                   "1,6;2,6;4,6;5,6;6,6;8,6;9,6;11,6;12,6;15,6;16,6;19,6;20,6;22,6;" +
                   "4,7;8,7;12,7;16,7;20,7;" +
                   "4,8;5,8;8,8;9,8;12,8;13,8;16,8;17,8;20,8;21,8;" +
                   "1,9;5,9;9,9;17,9;21,9;" +
                   "1,10;2,10;5,10;6,10;9,10;10,10;13,10;14,10;17,10;18,10;21,10;22,10;" +
                   "2,11;6,11;10,11;14,11;18,11;22,11;" +
                   "2,12;3,12;6,12;7,12;10,12;11,12;14,12;15,12;18,12;19,12;22,12;" +
                   "3,13;7,13;11,13;15,13;19,13" +
                   ":WATER:1,14:FIRE:22,14:DOOR:11,1" +
                   ":MONSTERS:7,3,LENXUONG;15,5,TRAIPHAI;3,8,TINH;19,9,LENXUONG;11,11,TRAIPHAI;5,13,LENXUONG;17,13,TINH";
                   
        case 2:
            // LEVEL 2: Phức tạp hơn với nhiều lối đi
            return "MEDIUM:24x16:WALLS:" +
                   // Outer walls
                   "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" +
                   "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" +
                   "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" +
                   // Complex inner maze
                   "2,1;3,1;5,1;6,1;8,1;9,1;11,1;12,1;14,1;15,1;17,1;18,1;20,1;21,1;" +
                   "3,2;6,2;9,2;12,2;15,2;18,2;21,2;" +
                   "1,3;3,3;4,3;6,3;7,3;9,3;10,3;12,3;13,3;15,3;16,3;18,3;19,3;21,3;22,3;" +
                   "1,4;4,4;7,4;10,4;13,4;16,4;19,4;22,4;" +
                   "1,5;2,5;4,5;5,5;7,5;8,5;10,5;11,5;13,5;14,5;16,5;17,5;19,5;20,5;22,5;" +
                   "2,6;5,6;8,6;11,6;14,6;17,6;20,6;" +
                   "2,7;3,7;5,7;6,7;8,7;9,7;11,7;12,7;14,7;15,7;17,7;18,7;20,7;21,7;" +
                   "3,8;6,8;9,8;12,8;15,8;18,8;21,8;" +
                   "1,9;3,9;4,9;6,9;7,9;9,9;10,9;12,9;13,9;15,9;16,9;18,9;19,9;21,9;22,9;" +
                   "1,10;4,10;7,10;10,10;13,10;16,10;19,10;22,10;" +
                   "1,11;2,11;4,11;5,11;7,11;8,11;10,11;11,11;13,11;14,11;16,11;17,11;19,11;20,11;22,11;" +
                   "2,12;5,12;8,12;11,12;14,12;17,12;20,12;" +
                   "2,13;3,13;5,13;6,13;8,13;9,13;11,13;12,13;14,13;15,13;17,13;18,13;20,13;21,13" +
                   ":WATER:1,1:FIRE:22,1:DOOR:11,15" +
                   ":MONSTERS:4,2,LENXUONG;19,2,TRAIPHAI;7,4,TINH;16,4,LENXUONG;3,6,TRAIPHAI;20,6,LENXUONG;11,8,TINH;6,10,TRAIPHAI;17,10,LENXUONG;9,12,TINH;15,12,LENXUONG";
                   
        case 3:
            // LEVEL 3: Cực kỳ phức tạp
            return "HARD:24x16:WALLS:" +
                   // Outer walls
                   "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" +
                   "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" +
                   "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" +
                   // Very complex maze - nhiều ngõ cụt và đường vòng
                   "1,1;2,1;4,1;5,1;7,1;8,1;10,1;11,1;13,1;14,1;16,1;17,1;19,1;20,1;22,1;" +
                   "2,2;5,2;8,2;11,2;14,2;17,2;20,2;" +
                   "1,3;2,3;4,3;5,3;6,3;8,3;9,3;11,3;12,3;14,3;15,3;17,3;18,3;20,3;21,3;22,3;" +
                   "1,4;4,4;6,4;9,4;12,4;15,4;18,4;21,4;" +
                   "1,5;2,5;4,5;5,5;6,5;7,5;9,5;10,5;12,5;13,5;15,5;16,5;18,5;19,5;21,5;22,5;" +
                   "2,6;5,6;7,6;10,6;13,6;16,6;19,6;22,6;" +
                   "2,7;3,7;5,7;6,7;7,7;8,7;10,7;11,7;13,7;14,7;16,7;17,7;19,7;20,7;22,7;" +
                   "3,8;6,8;8,8;11,8;14,8;17,8;20,8;" +
                   "1,9;3,9;4,9;6,9;7,9;8,9;9,9;11,9;12,9;14,9;15,9;17,9;18,9;20,9;21,9;22,9;" +
                   "1,10;4,10;7,10;9,10;12,10;15,10;18,10;21,10;" +
                   "1,11;2,11;4,11;5,11;7,11;8,11;9,11;10,11;12,11;13,11;15,11;16,11;18,11;19,11;21,11;22,11;" +
                   "2,12;5,12;8,12;10,12;13,12;16,12;19,12;" +
                   "2,13;3,13;5,13;6,13;8,13;9,13;10,13;11,13;13,13;14,13;16,13;17,13;19,13;20,13;" +
                   "3,14;6,14;9,14;11,14;14,14;17,14;20,14" +
                   ":WATER:22,2:FIRE:1,2:DOOR:11,15" +
                   ":MONSTERS:3,3,TINH;20,3,TINH;6,5,LENXUONG;16,5,LENXUONG;9,7,TRAIPHAI;14,7,TRAIPHAI;4,9,LENXUONG;18,9,LENXUONG;8,11,TINH;15,11,TINH;11,13,TINH;5,2,LENXUONG;18,2,LENXUONG;11,8,LENXUONG;7,12,TRAIPHAI;16,12,TRAIPHAI";
                   
        default:
            return "SIMPLE:24x16:WALLS:" +
                   // Outer walls (chỉ 3 mặt, để lại lối thoát ở dưới)
                   "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" +
                   "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" +
                   "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" +
                   // Inner maze - tạo đường đi thú vị
                   "2,2;3,2;4,2;5,2;6,2;8,2;9,2;10,2;12,2;13,2;14,2;16,2;17,2;18,2;20,2;21,2;" +
                   "2,3;6,3;10,3;14,3;18,3;21,3;" +
                   "2,4;3,4;6,4;7,4;10,4;11,4;14,4;15,4;18,4;19,4;21,4;" +
                   "6,5;11,5;15,5;19,5;" +
                   "1,6;2,6;4,6;5,6;6,6;8,6;9,6;11,6;12,6;15,6;16,6;19,6;20,6;22,6;" +
                   "4,7;8,7;12,7;16,7;20,7;" +
                   "4,8;5,8;8,8;9,8;12,8;13,8;16,8;17,8;20,8;21,8;" +
                   "1,9;5,9;9,9;17,9;21,9;" +
                   "1,10;2,10;5,10;6,10;9,10;10,10;13,10;14,10;17,10;18,10;21,10;22,10;" +
                   "2,11;6,11;10,11;14,11;18,11;22,11;" +
                   "2,12;3,12;6,12;7,12;10,12;11,12;14,12;15,12;18,12;19,12;22,12;" +
                   "3,13;7,13;11,13;15,13;19,13" +
                   ":WATER:1,14:FIRE:22,14:DOOR:11,1" +
                   ":MONSTERS:7,3,LENXUONG;15,5,TRAIPHAI;3,8,TINH;19,9,LENXUONG;11,11,TRAIPHAI;5,13,LENXUONG;17,13,TINH";
    }
}
    

    
    // Tạo Level object
private Level createLevel(int level) {
    switch (level) {
        case 1:          
            return new Level("SIMPLE", 24, 16, 
                Arrays.asList(
                    // Outer walls
                    new Point(2,2), new Point(3,2), new Point(4,2), new Point(5,2), new Point(7,2), new Point(8,2),
                    new Point(10,2), new Point(11,2), new Point(13,2), new Point(14,2), new Point(16,2), new Point(17,2),
                    new Point(19,2), new Point(20,2), new Point(21,2),
                    // Inner maze walls (simplified list)
                    new Point(2,4), new Point(4,4), new Point(6,4), new Point(8,4), new Point(10,4), new Point(12,4),
                    new Point(14,4), new Point(16,4), new Point(18,4), new Point(20,4)
                ),
                new Point(1, 1), new Point(22, 14), new Point(11, 1));
                
        case 2:
            return new Level("MEDIUM", 24, 16,
                Arrays.asList(
                    // More complex walls for level 2
                    new Point(1,1), new Point(2,1), new Point(4,1), new Point(5,1), new Point(7,1), new Point(8,1),
                    new Point(10,1), new Point(12,1), new Point(14,1), new Point(15,1), new Point(17,1), new Point(18,1),
                    new Point(20,1), new Point(21,1)
                ),
                new Point(4, 14), new Point(18, 1), new Point(11, 15));
                
        case 3:
            return new Level("HARD", 24, 16,
                Arrays.asList(
                    // Very complex walls for level 3
                    new Point(1,1), new Point(2,1), new Point(4,1), new Point(5,1), new Point(7,1), new Point(8,1),
                    new Point(10,1), new Point(12,1), new Point(14,1), new Point(15,1), new Point(17,1), new Point(18,1),
                    new Point(20,1), new Point(21,1), new Point(2,2), new Point(4,2), new Point(7,2), new Point(10,2)
                ),
                new Point(22, 1), new Point(1, 14), new Point(11, 15));
                
        default:
            return createLevel(1);
    }
}
    

    public Map<String, Point> getPlayerPositions() {
        return playerPositions;
    }
    
    public PlayerType getPlayerType(String playerId) {
        return playerTypes.get(playerId);
    }
    
    public Level getCurrentLevel() {
        return currentLevelData;
    }
    
    public int getCurrentLevelIndex() {
        return currentLevel - 1; // 0-based index
    }
    
    public int getTotalLevels() {
        return Constants.TOTAL_LEVELS;
    }
    
   public void resetPlayerPositions() {
    if (currentLevelData != null) {
        for (Map.Entry<String, PlayerType> entry : playerTypes.entrySet()) { 
            String playerId = entry.getKey();
            PlayerType playerType = entry.getValue(); 
            
            if (PlayerType.WATER.equals(playerType)) { 
                playerPositions.put(playerId, new Point(currentLevelData.getWaterStart()));
            } else if (PlayerType.FIRE.equals(playerType)) { 
                playerPositions.put(playerId, new Point(currentLevelData.getFireStart()));
            }
        }
    }
}




    
    public void broadcastToAll(String message) {
        System.out.println("Broadcasting to room " + roomId + ": " + message);
        for (PrintWriter writer : playerWriters.values()) {
            writer.println(message);
        }
    }
    
    private void broadcastToOthers(String excludePlayerId, String message) {
        for (Map.Entry<String, PrintWriter> entry : playerWriters.entrySet()) {
            if (!entry.getKey().equals(excludePlayerId)) {
                entry.getValue().println(message);
            }
        }
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    public boolean hasPlayer(String playerId) {
        return players.containsKey(playerId);
    }
    
    public String getRoomId() {
        return roomId;
    }
    
  
}
