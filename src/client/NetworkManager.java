package client;

import utils.Constants;
import java.io.*;
import java.net.*;

public class NetworkManager {
    private Socket socket;              // Kết nối socket
    private PrintWriter out;            // Gửi dữ liệu
    private BufferedReader in;          // Nhận dữ liệu
    private boolean connected = false;  // Trạng thái kết nối
    private MessageHandler messageHandler; // Bộ xử lý tin nhắn
    
    public NetworkManager(MessageHandler handler) {
        this.messageHandler = handler;
    }
    
    /**
     * Tạo kết nối đến server
     * @return true nếu kết nối thành công
     */
    public boolean connect() {
        try {
            socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            // Bắt đầu luồng lắng nghe tin nhắn từ server
            new Thread(this::listenToServer).start();
            return true;
            
        } catch (IOException e) {
            System.err.println("Lỗi kết nối: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gửi tin nhắn đến server
     */
    public void sendMessage(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }
    
    /**
     * Ngắt kết nối với server
     */
    public void disconnect() {
        connected = false;
        sendMessage("DISCONNECT");
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }
    
    /**
     * Lắng nghe tin nhắn từ server
     */
    private void listenToServer() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                messageHandler.handleMessage(message);
            }
        } catch (IOException e) {
            if (connected) {
                messageHandler.handleConnectionLost();
            }
        }
    }
    
    public boolean isConnected() { 
        return connected; 
    }
}
