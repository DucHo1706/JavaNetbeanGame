package client;

import utils.Constants;
import java.io.*;
import java.net.*;

public class NetworkManager {
  private Socket socket;              
    private PrintWriter out;            
    private BufferedReader in;          
    private boolean connected = false;  
    private IMessageHandler messageHandler; 
    private MessageListener messageListener; 

    // Interface cho lobby sử dụng
    public interface MessageListener {
        void onMessageReceived(String message);
    }

    // Constructor cho GameClient (có IMessageHandler)
    public NetworkManager(IMessageHandler handler) {
        this.messageHandler = handler;
    }
    
    // Constructor cho LobbyFrame (không có handler)
    public NetworkManager() {
        this.messageHandler = null;
    }
    
    public boolean connect() {
        try {
            socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            new Thread(this::listenToServer).start();
            return true;
            
        } catch (IOException e) {
            System.err.println("Loi ket noi: " + e.getMessage());
            return false;
        }
    }
    
    public void sendMessage(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }
    
    public void disconnect() {
        connected = false;
        if (out != null && connected) {
            sendMessage(Constants.NGAT_KET_NOI); 
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Loi dong ket noi: " + e.getMessage());
        }
    }
    
    private void listenToServer() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                if (messageHandler != null) {
                    messageHandler.handleMessage(message);
                }
                else if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            }
        } catch (IOException e) {
            if (connected && messageHandler != null) {
                messageHandler.handleConnectionLost();
            }
        }
    }
    
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    
    public void setMessageHandler(IMessageHandler handler) {
        this.messageHandler = handler;
    }
    
    public boolean isConnected() { 
        return connected; 
    }
    
    public BufferedReader getBufferedReader() {
        return in;
    }

    public PrintWriter getPrintWriter() {
        return out;
    }
}