package client;

import utils.Constants;
import java.io.*;
import java.net.*;

public class NetworkManager {
    private Socket socket;              // Ket noi socket
    private PrintWriter out;            // Gui du lieu
    private BufferedReader in;          // Nhan du lieu
    private boolean connected = false;  // Trang thai ket noi
    private MessageHandler messageHandler; // Bo xu ly tin nhan
    
    public NetworkManager(MessageHandler handler) {
        this.messageHandler = handler;
    }
    
    /**
     * Tao ket noi den server
     * @return true neu ket noi thanh cong
     */
    public boolean connect() {
        try {
            socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            // Bat dau luong lang nghe tin nhan tu server
            new Thread(this::listenToServer).start();
            return true;
            
        } catch (IOException e) {
            System.err.println("Loi ket noi: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gui tin nhan den server
     */
    public void sendMessage(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }
    
    /**
     * Ngat ket noi voi server
     */
    public void disconnect() {
        connected = false;
        sendMessage(Constants.NGAT_KET_NOI); 
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Loi dong ket noi: " + e.getMessage());
        }
    }
    
    /**
     * Lang nghe tin nhan tu server
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