package client;

import server.Level;
import game.Monster;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.io.PrintWriter;
import utils.Constants;

/**
 * Panel chinh hien thi game - da duoc refactor
 */
public class GamePanel extends JPanel {
    // Kich thuoc grid
    private int gridWidth = 24;
    private int gridHeight = 16;
    
    // Cac thanh phan chinh
    private GameRenderer renderer;
    private GameLogic gameLogic;
    
    // Du lieu game
    private Point firePlayer;
    private Point waterPlayer;
    private Point myPlayer;
    private Point otherPlayer;
    private Set<Point> walls;
    private Point door;
    private String status;
    private String playerType; // Bo enum, dung String
    private int currentLevelNumber = 1;
    private Level currentLevelData;
    private JLabel timeLabel;
    // Animation
    private Timer animationTimer;
    private Timer monsterTimer;
    private boolean showWinAnimation = false;
    
    private JButton readyButton;
    private Set<String> nguoiChoiDaSanSang = new HashSet<>();
    private JTextArea readyStatusArea; // Hiển thị người chơi đã sẵn sàng
    
    
        // 1. Thêm biến toàn cục ở đầu class GamePanel
    private int tongThoiGianChoi = 0;      // Tổng thời gian chơi tích lũy (giây)
    private int thoiGianVongHienTai = 0;   // Thời gian vòng hiện tại (giây)
    private final int THOI_GIAN_VONG = 60; // Thời gian giới hạn mỗi vòng (60 giây)
    private Timer timerThoiGianChoi;        // Timer đếm thời gian chơi
    private JLabel labelThoiGian;
        // 2. Hàm bắt đầu đếm thời gian vòng chơi mới
   public void batDauVongMoi() {
    if (timerThoiGianChoi != null && timerThoiGianChoi.isRunning()) {
        timerThoiGianChoi.stop();
    }
    thoiGianVongHienTai = 0;

    timerThoiGianChoi = new Timer(1000, e -> {
        thoiGianVongHienTai++;
        tongThoiGianChoi++;

        capNhatHienThiThoiGian();

        if (thoiGianVongHienTai >= THOI_GIAN_VONG) {
            timerThoiGianChoi.stop();
            xuLyHetThoiGianVong();
        }
    });
    timerThoiGianChoi.start();
    readyButton.setEnabled(false);
    setStatus("Vòng chơi bắt đầu!");
}

    // 4. Xử lý khi hết thời gian vòng chơi
    private void xuLyHetThoiGianVong() {
      JOptionPane.showMessageDialog(this, "Hết thời gian vòng chơi! Thời gian vòng: " + thoiGianVongHienTai + " giây");
    readyButton.setEnabled(true);
    if (networkManager != null) {
        // Gửi thời gian vòng chơi (không phải tổng thời gian) đến server
        networkManager.sendMessage("CAP_NHAT_THOI_GIAN:" + thoiGianVongHienTai);
        System.out.println("Đã gửi thời gian vòng chơi đến server: " + thoiGianVongHienTai);
    }
    }
    
    // 3. Hàm cập nhật hiển thị thời gian (bạn có thể chỉnh sửa theo UI hiện tại)
private void capNhatHienThiThoiGian() {
    // Giả sử bạn có label hiển thị thời gian, ví dụ labelThoiGian
    if (labelThoiGian != null) {
        labelThoiGian.setText("Tong thoi gian choi: " + tongThoiGianChoi + " giây");
    }
}
 public void resetGameState() {
    // Xóa dữ liệu bản đồ
    walls.clear();

    // Reset vị trí nhân vật
    firePlayer = new Point(1, 1);
    waterPlayer = new Point(1, 1);
    myPlayer = new Point(1, 1);
    otherPlayer = null;

    // Reset cửa
    door = null;

    // Reset level hiện tại
    currentLevelNumber = 1;
    currentLevelData = null;

    // Reset trạng thái
    status = "Chưa vào phòng";
    playerType = null;

    // Reset thời gian chơi
    tongThoiGianChoi = 0;
    thoiGianVongHienTai = 0;
    if (timerThoiGianChoi != null && timerThoiGianChoi.isRunning()) {
        timerThoiGianChoi.stop();
    }
    if (labelThoiGian != null) {
        labelThoiGian.setText("Tổng thời gian chơi: 0 giây");
    }

    // Reset trạng thái sẵn sàng
    nguoiChoiDaSanSang.clear();
    

    // Tắt animation chiến thắng nếu đang bật
    showWinAnimation = false;

    // Yêu cầu vẽ lại
    repaint();

    System.out.println("Đã reset trạng thái gamePanel về mặc định");
}

    private NetworkManager networkManager;

    public void setNetworkManager(NetworkManager nm) {
        this.networkManager = nm;
        System.out.println("NetworkManager đã được thiết lập cho GamePanel");
    }

    public GamePanel() {
        initializeComponents();
        initializeData();
        setupTimers();
        updatePanelSize();

        timeLabel = new JLabel("Thời gian: 00:00");
        this.add(timeLabel);

        readyButton = new JButton("Sẵn sàng");
        readyButton.setVisible(false);
        readyButton.addActionListener(e -> {
            // Gửi lệnh SAN_SANG lên server
            if (networkManager != null) {
                networkManager.sendMessage("SAN_SANG");
                System.out.println("Đã gửi lệnh SAN_SANG lên server");
            } else {
                System.out.println("NetworkManager chưa được thiết lập, không thể gửi lệnh SAN_SANG");
            }
            readyButton.setEnabled(false);
             
        });
        this.add(readyButton);

        // Trong hàm khởi tạo GamePanel hoặc hàm initUI(), thêm:
  labelThoiGian = new JLabel("Tổng thời gian chơi: 0 giây");
  labelThoiGian.setFont(new Font("Arial", Font.BOLD, 14));
  labelThoiGian.setForeground(Color.WHITE); // hoặc màu phù hợp
  this.add(labelThoiGian);
    }

    public void showReadyButton(boolean show) {
        SwingUtilities.invokeLater(() -> {
            readyButton.setVisible(show);
            readyButton.setEnabled(true);
            System.out.println("Nút 'Sẵn sàng' đã được " + (show ? "hiển thị" : "ẩn đi"));
            if (show) {
                resetReadyStatus(); // Xóa trạng thái cũ khi hiện nút
                System.out.println("Đã reset trạng thái người chơi đã sẵn sàng");
            }
        });
    }

    public void updateReadyStatus(String playerId) {
        nguoiChoiDaSanSang.add(playerId);
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder("Người chơi đã sẵn sàng:\n");
            for (String id : nguoiChoiDaSanSang) {
                sb.append("- ").append(id).append("\n");
            }
            readyStatusArea.setText(sb.toString());
            System.out.println("Cập nhật trạng thái sẵn sàng: Người chơi " + playerId + " đã sẵn sàng");
        });
    }

    public void resetReadyStatus() {
        nguoiChoiDaSanSang.clear();
        SwingUtilities.invokeLater(() -> {
            readyStatusArea.setText("");
            System.out.println("Đã xóa danh sách người chơi đã sẵn sàng");
        });
    }

    public void updateTimeRemaining(int giayConLai) {
        int phut = giayConLai / 60;
        int giay = giayConLai % 60;
        String timeText = String.format("Thời gian: %02d:%02d", phut, giay);
        timeLabel.setText(timeText);
        timeLabel.setForeground(Color.WHITE);                    // Màu chữ trắng
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));  // Font đẹp hơn, size 18

        // Thêm hiệu ứng khi thời gian sắp hết 
        if (giayConLai <= 30) {
            timeLabel.setForeground(Color.RED);     // Đổi màu đỏ khi < 30 giây
        } else if (giayConLai <= 60) {
            timeLabel.setForeground(Color.YELLOW);  // Màu vàng khi < 1 phút
        } else {
            timeLabel.setForeground(Color.WHITE);   // Màu trắng bình thường
        }
        System.out.println("Cập nhật thời gian còn lại: " + timeText);
    }

    /**
     * Khoi tao cac components
     */
    private void initializeComponents() {
        renderer = new GameRenderer();
        gameLogic = new GameLogic(this);

        setBackground(new Color(34, 34, 34));
        setFocusable(true);

        System.out.println("Khởi tạo các components GamePanel");
    }

    /**
     * Khoi tao du lieu ban dau
     */
    private void initializeData() {
        walls = new HashSet<>();
        firePlayer = new Point(1, 1);
        waterPlayer = new Point(1, 1);
        myPlayer = new Point(1, 1);
        status = "Dang ket noi...";
        System.out.println("Khởi tạo dữ liệu ban đầu của GamePanel");
    }

    /**
     * Thiet lap cac timer
     */
    private void setupTimers() {
        // Animation timer
        animationTimer = new Timer(100, e -> {
            renderer.updateAnimation();
            repaint();
        });
        animationTimer.start();

        // Monster movement timer
        monsterTimer = new Timer(500, e -> {
            gameLogic.updateMonsters();
            repaint();
        });
        monsterTimer.start();

        System.out.println("Đã thiết lập các timer animation và monster");
    }

    /**
     * Cap nhat kich thuoc panel
     */
    private void updatePanelSize() {
        int newWidth = gridWidth * Constants.CELL_SIZE;
        int newHeight = gridHeight * Constants.CELL_SIZE;

        setPreferredSize(new Dimension(newWidth, newHeight));
        setSize(new Dimension(newWidth, newHeight));

        // Cap nhat parent containers
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent.repaint();

            if (parent instanceof JFrame) {
                ((JFrame) parent).pack();
                break;
            }
            parent = parent.getParent();
        }

        revalidate();
        repaint();

        System.out.println("Cập nhật kích thước panel: " + newWidth + "x" + newHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g, this);
    }

    /**
     * Set ket noi mang
     */
    public void setConnection(PrintWriter out) {
        gameLogic.setConnection(out);
        System.out.println("Đã thiết lập kết nối mạng cho GameLogic");
    }

    /**
     * Set loai nguoi choi
     * Nhan String truc tiep
     */
    public void setPlayerType(String playerTypeStr) {
        this.playerType = playerTypeStr;
        if ("FIRE".equals(playerType)) {
            myPlayer = firePlayer;
        } else if ("WATER".equals(playerType)) {
            myPlayer = waterPlayer;
        }
        repaint();
        System.out.println("Đã thiết lập loại người chơi: " + playerType);
    }

    /**
     * Load level moi
     */
    public void loadLevel(String levelData) {
        System.out.println("DEBUG: loadLevel called with: " + levelData);

        showWinAnimation = false;
        parseLevelData(levelData);
        updatePanelSize();
        gameLogic.sendPlayerUpdate();
        repaint();

        System.out.println("Đã tải level mới: " + currentLevelNumber);
    }

    /**
     * Di chuyen nguoi choi
     */
    public boolean movePlayer(String direction) {
        boolean moved = gameLogic.movePlayer(direction);
        if (moved) {
            System.out.println("Người chơi di chuyển theo hướng: " + direction);
        } else {
            System.out.println("Người chơi không thể di chuyển theo hướng: " + direction);
        }
        return moved;
    }

    /**
     * Cap nhat nguoi choi khac
     */
    public void updateOtherPlayer(String playerId, int x, int y, String direction) {
        System.out.println("Cập nhật vị trí người chơi khác: " + playerId + " tại (" + x + "," + y + ") hướng " + direction);
        if ("FIRE".equals(playerType)) {
            if (waterPlayer != null) waterPlayer.setLocation(x, y);
        } else if ("WATER".equals(playerType)) {
            if (firePlayer != null) firePlayer.setLocation(x, y);
        }
        repaint();
    }

    /**
     * Phan tich du lieu level tu server
     */
    private void parseLevelData(String levelData) {
        String[] parts = levelData.split(":");
        if (parts.length < 6) {
            System.err.println("Dữ liệu level không hợp lệ: " + levelData);
            return;
        }

        // Parse basic info
        String levelName = parts[0];
        String[] dimensions = parts[1].split("x");
        gridWidth = Integer.parseInt(dimensions[0]);
        gridHeight = Integer.parseInt(dimensions[1]);

        // Parse walls
        parseWalls(parts);

        // Parse spawn points va door
        Point fireSpawn = parseSpawnPoint(parts, "FIRE");
        Point waterSpawn = parseSpawnPoint(parts, "WATER");
        door = parseSpawnPoint(parts, "DOOR");

        if (firePlayer == null) firePlayer = new Point();
        if (waterPlayer == null) waterPlayer = new Point();
        firePlayer.setLocation(fireSpawn);
        waterPlayer.setLocation(waterSpawn);

        if ("FIRE".equals(playerType)) {
            myPlayer.setLocation(fireSpawn);
        } else if ("WATER".equals(playerType)) {
            myPlayer.setLocation(waterSpawn);
        }

        // Tao du lieu level
        List<Point> wallsList = new ArrayList<>(walls);
        currentLevelData = new Level(levelName, gridWidth, gridHeight, 
                                    wallsList, waterSpawn, fireSpawn, door);

        // Parse monsters
        parseMonsterData(parts);

        setStatus("Level " + currentLevelNumber + " - Di chuyen den cua vang!");
        System.out.println("Đã phân tích dữ liệu level: " + levelName + ", kích thước: " + gridWidth + "x" + gridHeight);
    }

    private void parseWalls(String[] parts) {
        walls.clear();
        if (parts.length > 3 && !parts[3].isEmpty()) {
            String[] wallCoords = parts[3].split(";");
            for (String coord : wallCoords) {
                String[] xy = coord.split(",");
                if (xy.length == 2) {
                    walls.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                }
            }
            System.out.println("Đã phân tích " + walls.size() + " bức tường");
        }
    }

    /**
     * Parse spawn point theo ten (String)
     */
    private Point parseSpawnPoint(String[] parts, String typeName) {
        for (int i = 0; i < parts.length; i++) {
            if (typeName.equals(parts[i]) && i + 1 < parts.length) {
                String[] coords = parts[i + 1].split(",");
                if (coords.length == 2) {
                    Point p = new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    System.out.println("Spawn point " + typeName + " tại: (" + p.x + "," + p.y + ")");
                    return p;
                }
            }
        }
        System.out.println("Không tìm thấy spawn point cho " + typeName + ", trả về mặc định (0,0)");
        return new Point(0, 0); // Default
    }

    /**
     * parse monster data
     */
    private void parseMonsterData(String[] parts) {
        if (currentLevelData == null) return;

        for (int i = 0; i < parts.length; i++) {
            if ("MONSTERS".equals(parts[i]) && i + 1 < parts.length) {
                String monsterData = parts[i + 1];
                if (!monsterData.isEmpty()) {
                    String[] monsters = monsterData.split(";");
                    int monsterId = 1;

                    for (String monster : monsters) {
                        String[] monsterInfo = monster.split(",");
                        if (monsterInfo.length >= 3) {
                            try {
                                int x = Integer.parseInt(monsterInfo[0]);
                                int y = Integer.parseInt(monsterInfo[1]);
                                String type = monsterInfo[2];

                                Monster newMonster = new Monster(monsterId++, new Point(x, y), type);
                                currentLevelData.addMonster(newMonster);

                                System.out.println("Added monster: " + type + " at (" + x + "," + y + ")");
                            } catch (NumberFormatException e) {
                                System.err.println("Error parsing monster data: " + monster);
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    public Point getFirePlayer() { return firePlayer; }
    public Point getWaterPlayer() { return waterPlayer; }
    public Point getMyPlayer() { return myPlayer; }
    public Point getOtherPlayer() { return otherPlayer; }
    public Set<Point> getWalls() { return walls; }
    public Point getDoor() { return door; }
    public String getStatus() { return status; }
    public String getPlayerType() { return playerType; }
    public Level getCurrentLevelData() { return currentLevelData; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public boolean isShowWinAnimation() { return showWinAnimation; }

    public void setStatus(String status) { 
        this.status = status; 
        repaint();
        System.out.println("Trạng thái game cập nhật: " + status);
    }

    public void setCurrentLevel(int level) { 
        this.currentLevelNumber = level; 
        System.out.println("Cấp độ hiện tại được đặt thành: " + level);
    }

    public void setShowWinAnimation(boolean show) { 
        this.showWinAnimation = show; 
        System.out.println("Hiển thị animation chiến thắng: " + show);
    }

    public Point getPlayerPosition() {
        return myPlayer != null ? new Point(myPlayer) : null;
    }
    
}
