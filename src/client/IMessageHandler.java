package client;

/**
 * Interface để xử lý các tin nhắn từ server
 */
public interface IMessageHandler {
    /**
     * Xử lý tin nhắn nhận được từ server
     */
    void handleMessage(String message);
    
    /**
     * Xử lý khi mất kết nối với server
     */
    void handleConnectionLost();
}
