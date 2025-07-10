package client;

import utils.Constants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameClient extends JFrame implements KeyListener {
    private NetworkManager networkManager;          // Quan ly ket noi mang
    private GameMessageProcessor messageProcessor;  // Xu ly tin nhan game
    private GamePanel gamePanel;                   // Bang hien thi game
    private String playerId;                       // ID nguoi choi
    
    public GameClient() {
        playerId = "Player_" + System.currentTimeMillis();
        initializeComponents();
        initializeGUI();
        connectToServer();
    }
    
    /**
     * Khoi tao cac thanh phan chinh
     */
    private void initializeComponents() {
        gamePanel = new GamePanel();
        messageProcessor = new GameMessageProcessor(gamePanel, this);
        networkManager = new NetworkManager(messageProcessor);
    }
    
    /**
     * Khoi tao giao dien nguoi dung
     */
    private void initializeGUI() {
        setTitle("Tro choi Lua va Nuoc - " + playerId);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        
        // Xu ly khi dong cua so
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                networkManager.disconnect();
                System.exit(0);
            }
        });
        
        add(gamePanel);
        addKeyListener(this);
        setFocusable(true);
        
        pack();
        setLocationRelativeTo(null);  // Can giua man hinh
        setVisible(true);
        
        showInstructions();
    }
    
    /**
     * Hien thi huong dan choi game
     */
    private void showInstructions() {
        String instructions = "HUONG DAN CHOI:\n\n" +
                "• Su dung WASD hoac phim mui ten de di chuyen\n" +
                "• Lua (Do) va Nuoc (Xanh) phai hop tac\n" +
                "• Ca hai phai den cua (Vang) cung luc de qua man\n" +
                "• Tranh cac buc tuong (Nau)\n" +
                "• Co 3 man choi voi do kho tang dan\n\n" +
                "Nhan OK de bat dau!";
        
        JOptionPane.showMessageDialog(this, instructions, 
            "Tro choi Lua va Nuoc", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Ket noi den server
     */
    private void connectToServer() {
        if (networkManager.connect()) {
            gamePanel.setStatus("Da ket noi! Dang tham gia game...");
            networkManager.sendMessage(Constants.THAM_GIA + ":" + playerId);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Khong the ket noi den server!\nVui long kiem tra server da chay chua.", 
                "Loi ket noi", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        // Kiem tra ket noi va loai nguoi choi truoc khi di chuyen
        if (!networkManager.isConnected() || messageProcessor.getPlayerType() == null) {
            return;
        }
        
        String direction = getDirectionFromKey(e.getKeyCode());
        if (!direction.isEmpty()) {
            boolean moved = gamePanel.movePlayer(direction);
            if (moved) {
                // Gui thong tin di chuyen den server
                Point playerPos = gamePanel.getPlayerPosition();
                String moveMessage = Constants.DI_CHUYEN + ":" + playerPos.x + ":" + playerPos.y + ":" + direction;
                networkManager.sendMessage(moveMessage);
            }
        }
    }
    
    /**
     * Chuyen doi ma phim thanh huong di chuyen
     */
    private String getDirectionFromKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return  Constants.UP;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return  Constants.DOWN;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return  Constants.LEFT;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return  Constants.RIGHT;
            default:
                return "";
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient());
    }
}