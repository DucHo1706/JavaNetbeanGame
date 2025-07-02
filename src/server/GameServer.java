package server;

import utils.Constants;
import java.io.*;
import java.net.*;
import utils.PlayerType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;

public class GameServer {
    private ServerSocket serverSocket; 
    private Map<String, GameRoom> gameRooms;
    private Map<Socket, String> playerSockets;
    private boolean isRunning;
    
    public GameServer() {
        gameRooms = new ConcurrentHashMap<>();
        playerSockets = new ConcurrentHashMap<>();
        isRunning = false;
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(Constants.SERVER_PORT);
        isRunning = true;
        System.out.println(" Fire Water Game Server started on port " + Constants.SERVER_PORT);
        System.out.println("Waiting for players to connect...");
        
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New player connected: " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                String[] parts = message.split(":");
                String command = parts[0];
                
                switch (command) {
                    case "JOIN": // nhận thông tin kết nối đến server
                        if (parts.length > 1) {
                            String playerId = parts[1];
                            joinGame(clientSocket, playerId, out);
                        }
                        break;
                    case "MOVE":
                        if (parts.length > 3) {
                            handlePlayerMove(clientSocket, parts[1], parts[2], parts[3]);
                        }
                        break;
                    case "LEVEL_COMPLETE":
                        handleLevelComplete(clientSocket);
                        break;
                    case "DISCONNECT":
                        handleDisconnect(clientSocket);
                        return;
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            handleDisconnect(clientSocket);
        }
    }
    
    private void joinGame(Socket socket, String playerId, PrintWriter out) {
        playerSockets.put(socket, playerId);
        
        // Tìm phòng có 1 người hoặc tạo phòng mới
        GameRoom availableRoom = null;
        for (GameRoom room : gameRooms.values()) {
            if (room.getPlayerCount() < Constants.MAX_PLAYERS_PER_ROOM) {
                availableRoom = room;
                break;
            }
        }
        
        if (availableRoom == null) {
            availableRoom = new GameRoom(UUID.randomUUID().toString());
            gameRooms.put(availableRoom.getRoomId(), availableRoom);
            System.out.println("Created new game room: " + availableRoom.getRoomId());
        }
        // tạo 1 phòng cho người chơi , ROOM_JOINED tham gia phòng 
       PlayerType playerType = availableRoom.addPlayer(socket, playerId);
        out.println("ROOM_JOINED:" + availableRoom.getRoomId() + ":" + playerType);
        
        if (availableRoom.getPlayerCount() == Constants.MAX_PLAYERS_PER_ROOM) {
            System.out.println("Room " + availableRoom.getRoomId() + " is full. Starting game...");
            availableRoom.startGame();
        } else {
            // đợi người chơi tham gia
            out.println("WAITING_FOR_PLAYER");
            System.out.println("Player " + playerId + " waiting for opponent...");
        }
    }
    
    private void handlePlayerMove(Socket socket, String x, String y, String direction) {
        String playerId = playerSockets.get(socket);
        if (playerId != null) {
            for (GameRoom room : gameRooms.values()) {
                if (room.hasPlayer(playerId)) {
                    room.updatePlayerPosition(playerId, Integer.parseInt(x), Integer.parseInt(y), direction);
                    
                    // Kiểm tra win condition sau mỗi lần di chuyển
                    checkWinCondition(room);
                    break;
                }
            }
        }
    }
    
    //  Kiểm tra điều kiện thắng
    private void checkWinCondition(GameRoom room) {
        System.out.println(" Checking win condition...");
        
        if (room.getPlayerCount() != 2) {
            System.out.println(" Not enough players");
            return;
        }
        
        Point doorPos = room.getCurrentLevel().getDoor();
        System.out.println(" Door position: " + doorPos);
        
        boolean waterAtDoor = false;
        boolean fireAtDoor = false;
        
        // Kiểm tra vị trí của từng player
        for (Map.Entry<String, Point> entry : room.getPlayerPositions().entrySet()) {
            String playerId = entry.getKey();
            Point playerPos = entry.getValue();
           PlayerType playerType = room.getPlayerType(playerId);
            
            System.out.println(" Player " + playerType + " at: " + playerPos);
            
            if (playerPos.equals(doorPos)) {
            if (PlayerType.WATER.equals(playerType)) { // SỬA: dùng enum
                waterAtDoor = true;
                System.out.println("Water at door!");
            } else if (PlayerType.FIRE.equals(playerType)) { // SỬA: dùng enum
                fireAtDoor = true;
                System.out.println("Fire at door!");
            }
        }
        }
        
        if (waterAtDoor && fireAtDoor) {
            System.out.println(" BOTH PLAYERS AT DOOR - LEVEL COMPLETE!");
            
            if (room.getCurrentLevelIndex() < room.getTotalLevels() - 1) {

                room.nextLevel();
                room.broadcastToAll("NEXT_LEVEL:" + (room.getCurrentLevelIndex() + 1));

                Level nextLevel = room.getCurrentLevel();
                String levelData = "LEVEL_DATA:" + nextLevel.getName() + ":" + 
                                 nextLevel.getWidth() + "x" + nextLevel.getHeight() + ":" +
                                 "WALLS:" + nextLevel.getWallsString() + ":" +
                                 "WATER:" + nextLevel.getWaterStart().x + "," + nextLevel.getWaterStart().y + ":" +
                                 "FIRE:" + nextLevel.getFireStart().x + "," + nextLevel.getFireStart().y + ":" +
                                 "DOOR:" + nextLevel.getDoor().x + "," + nextLevel.getDoor().y;
                
                room.broadcastToAll(levelData);
                
                // Reset player positions
                room.resetPlayerPositions();
                
            } else {
                // Game complete
                room.broadcastToAll("GAME_COMPLETE");
                System.out.println(" GAME COMPLETED!");
            }
        } else {
            System.out.println(" Waiting for both players at door...");
        }
    }
    
    private void handleLevelComplete(Socket socket) {
        String playerId = playerSockets.get(socket);
        if (playerId != null) {
            for (GameRoom room : gameRooms.values()) {
                if (room.hasPlayer(playerId)) {
                    room.nextLevel();
                    break;
                }
            }
        }
    }
    
    private void handleDisconnect(Socket socket) {
        String playerId = playerSockets.remove(socket);
        if (playerId != null) {
            System.out.println("Player " + playerId + " disconnected");
            // Thông báo cho room
            for (GameRoom room : gameRooms.values()) {
                if (room.hasPlayer(playerId)) {
                    room.removePlayer(playerId);
                    if (room.getPlayerCount() == 0) {
                        gameRooms.remove(room.getRoomId());
                        System.out.println("Room " + room.getRoomId() + " removed");
                    }
                    break;
                }
            }
        }
        
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer();
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
