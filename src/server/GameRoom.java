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
                   "8,1;9,1;11,1;12,1;14,1;15,1;17,1;18,1;" +
                   "3,2;6,2;9,2;12,2;15,2;18,2;" +
                   "1,3;3,3;4,3;6,3;9,3;10,3;12,3;13,3;15,3;16,3;18,3;19,3;22,3;" +
                   "1,4;4,4;10,4;13,4;22,4;" +
                   "1,5;2,5;4,5;5,5;8,5;10,5;11,5;13,5;14,5;16,5;17,5;19,5;20,5;22,5;" +
                   "2,6;5,6;8,6;11,6;14,6;17,6;20,6;" +
                   "2,7;3,7;5,7;6,7;8,7;9,7;11,7;12,7;14,7;15,7;17,7;18,7;20,7;21,7;" +
                   "3,8;6,8;9,8;12,8;15,8;18,8;21,8;" +
                   "1,9;3,9;4,9;6,9;7,9;9,9;10,9;12,9;13,9;15,9;16,9;18,9;19,9;21,9;22,9;" +
                   "1,10;4,10;10,10;13,10;16,10;19,10;22,10;" +
                   "1,11;2,11;4,11;5,11;8,11;10,11;11,11;13,11;14,11;16,11;17,11;19,11;20,11;22,11;" +
                   "2,12;5,12;8,12;11,12;14,12;17,12;20,12;" +
                   "2,13;3,13;5,13;6,13;8,13;11,13;12,13;14,13;15,13;17,13;18,13;20,13;21,13" +
                   ":WATER:1,1:FIRE:22,1:DOOR:11,15" +
                   ":MONSTERS:4,2,LENXUONG;19,2,TRAIPHAI;7,4,TRAIPHAI;16,4,TRAIPHAI;3,6,TRAIPHAI;20,4,LENXUONG;11,8,TINH;6,10,TRAIPHAI;17,10,LENXUONG;9,12,TINH;15,12,LENXUONG";
                   
        case 3:
            // LEVEL 3: Cực kỳ phức tạp
          return "HARD:24x16:WALLS:" +
    // Outer walls (top, left, right, bottom excluding door)
                    "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" + // Top
                    "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" + // Left
                    "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" + // Right
                    "0,15;1,15;2,15;3,15;4,15;5,15;6,15;7,15;8,15;9,15;10,15;12,15;13,15;14,15;15,15;16,15;17,15;18,15;19,15;20,15;21,15;22,15;23,15;" + // Bottom (excluding door 11,15)

                    // Inner maze walls - Mapped meticulously row by row based on the image
                    // Row 1 (Empty except for players)
                    // Row 2
                    "2,2;3,2;4,2;5,2;6,2;7,2;8,2;9,2;10,2;11,2;12,2;13,2;14,2;15,2;16,2;17,2;18,2;19,2;20,2;21,2;" +
                    // Row 3 (Monsters here, no walls)
                    // Row 4
                    "1,4;3,4;4,4;6,4;7,4;9,4;10,4;12,4;13,4;15,4;16,4;18,4;19,4;21,4;22,4;" +
                    // Row 5 (Monsters here, no walls)
                    // Row 6
                    "1,6;2,6;3,6;5,6;6,6;8,6;9,6;11,6;12,6;14,6;15,6;17,6;18,6;20,6;21,6;22,6;" +
                    // Row 7 (Monsters here, no walls)
                    // Row 8
                    "1,8;2,8;3,8;5,8;6,8;8,8;9,8;11,8;12,8;14,8;15,8;17,8;18,8;20,8;21,8;22,8;" +
                    // Row 9 (Monsters here, no walls)
                    // Row 10
                    "1,10;3,10;4,10;6,10;7,10;9,10;10,10;12,10;13,10;15,10;16,10;18,10;19,10;21,10;22,10;" +
                    // Row 11 (Monsters here, no walls)
                    // Row 12
                    "1,12;2,12;3,12;5,12;6,12;8,12;9,12;11,12;12,12;14,12;15,12;17,12;18,12;20,12;21,12;22,12;" +
                    // Row 13 (Monsters here, no walls)
                    // Row 14
                    "1,14;3,14;4,14;6,14;7,14;9,14;10,14;12,14;13,14;15,14;16,14;18,14;19,14;21,14;22,14" +

                    ":WATER:22,1:FIRE:1,1:DOOR:11,15" + // Player start and Door positions

                    ":MONSTERS:" +
                    // Red Slime (Horizontal - TRAIPHAI)
                    "3,3,TRAIPHAI;6,3,TRAIPHAI;9,3,TRAIPHAI;12,3,TRAIPHAI;15,3,TRAIPHAI;18,3,TRAIPHAI;21,3,TRAIPHAI;" +
                    "3,9,TRAIPHAI;6,9,TRAIPHAI;9,9,TRAIPHAI;12,9,TRAIPHAI;15,9,TRAIPHAI;18,9,TRAIPHAI;21,9,TRAIPHAI;" +
                    // Green Slime (Vertical - LENXUONG)
                    "2,4,LENXUONG;2,7,LENXUONG;2,10,LENXUONG;2,13,LENXUONG;" +
                    "5,1,LENXUONG;5,4,LENXUONG;5,7,LENXUONG;5,10,LENXUONG;5,13,LENXUONG;" +
                    "8,1,LENXUONG;8,4,LENXUONG;8,7,LENXUONG;8,10,LENXUONG;8,13,LENXUONG;" +
                    "11,1,LENXUONG;11,4,LENXUONG;11,7,LENXUONG;11,10,LENXUONG;11,13,LENXUONG;" +
                    "14,1,LENXUONG;14,4,LENXUONG;14,7,LENXUONG;14,10,LENXUONG;14,13,LENXUONG;" +
                    "17,1,LENXUONG;17,4,LENXUONG;17,7,LENXUONG;17,10,LENXUONG;17,13,LENXUONG;" +
                    "20,1,LENXUONG;20,4,LENXUONG;20,7,LENXUONG;20,10,LENXUONG;20,13,LENXUONG;" +
                    // Spider (Stationary - TINH)
                    "4,5,TINH;7,5,TINH;10,5,TINH;13,5,TINH;16,5,TINH;19,5,TINH;" +
                    "4,7,TINH;7,7,TINH;10,7,TINH;13,7,TINH;16,7,TINH;19,7,TINH;" +
                    "4,11,TINH;7,11,TINH;10,11,TINH;13,11,TINH;16,11,TINH;19,11,TINH;" ;
        default:
            return generateLevelData(3);
    }
}
    

    
 // Tạo Level object bằng cách parse string từ generateLevelData()
private Level createLevel(int level) {
    String levelData = generateLevelData(level);
    return parseLevelData(levelData);
}

// Method parse string thành Level object
private Level parseLevelData(String levelData) {
    String[] parts = levelData.split(":");
    
 
    String difficulty = parts[0]; // "SIMPLE", "MEDIUM", "HARD"
    

    String[] sizeParts = parts[1].split("x");
    int width = Integer.parseInt(sizeParts[0]);
    int height = Integer.parseInt(sizeParts[1]);
    

    List<Point> walls = new ArrayList<>();
    if (parts.length > 3 && parts[2].equals("WALLS")) {
        String[] wallCoords = parts[3].split(";");
        for (String coord : wallCoords) {
            if (!coord.trim().isEmpty()) {
                String[] xy = coord.split(",");
                walls.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
    }
    

    Point waterStart = null;
    for (int i = 4; i < parts.length - 1; i++) {
        if (parts[i].equals("WATER")) {
            String[] coords = parts[i + 1].split(",");
            waterStart = new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
            break;
        }
    }
    

    Point fireStart = null;
    for (int i = 4; i < parts.length - 1; i++) {
        if (parts[i].equals("FIRE")) {
            String[] coords = parts[i + 1].split(",");
            fireStart = new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
            break;
        }
    }
    

    Point door = null;
    for (int i = 4; i < parts.length - 1; i++) {
        if (parts[i].equals("DOOR")) {
            String[] coords = parts[i + 1].split(",");
            door = new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
            break;
        }
    }
    
    return new Level(difficulty, width, height, walls, waterStart, fireStart, door);
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
