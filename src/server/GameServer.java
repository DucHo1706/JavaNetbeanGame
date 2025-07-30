package server;

import GameLobby.LeaderboardUtils;
import client.NetworkManager;
import utils.Constants;
import utils.PlayerType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;
import client.SoundManager;

public class GameServer {
    
    //  THUỘC TÍNH 
    
    // Server core
    private ServerSocket serverSocket;
    private boolean dangChay;
    private NetworkManager networkManager;
    
    // Quản lý phòng và người chơi
    private Map<String, GameRoom> danhSachPhongChoi;
    private Map<Socket, String> danhSachSocketNguoiChoi;
    
    // Bảng xếp hạng và thống kê
    private Map<String, Integer> tongThoiGianChoi = new ConcurrentHashMap<>();
    private Map<String, Integer> capDoCaoNhat = new ConcurrentHashMap<>();
    private static final String CAP_DO_FILE = "data/capdo_caonhat.txt";
private Map<String, Integer> playerScores = new ConcurrentHashMap<>();
    //  CONSTRUCTOR 
    
    public GameServer() {
        danhSachPhongChoi = new ConcurrentHashMap<>();
        danhSachSocketNguoiChoi = new ConcurrentHashMap<>();
        dangChay = false;
        
        // Load bảng xếp hạng từ file khi khởi động server
        Map<String, Integer> loadedRankings = LeaderboardUtils.loadLeaderboardFromFile();
        if (loadedRankings != null) {
            tongThoiGianChoi.putAll(loadedRankings);
            System.out.println("Đã tải bảng xếp hạng từ file khi khởi động server.");
        }
        taiCapDoCaoNhatTuFile();
    }

    //  GETTER/SETTER 
    
    public Map<String, Integer> getTongThoiGianChoi() {
        return tongThoiGianChoi;
    }

    public Map<String, Integer> getCapDoCaoNhat() {
        return capDoCaoNhat;
    }

    //  QUẢN LÝ BẢNG XẾP HẠNG 
    
    public synchronized void luuBangXepHangRaFile() {
        LeaderboardUtils.saveLeaderboardToFile(tongThoiGianChoi);
    }

    public synchronized void capNhatThoiGianChoi(String maNguoiChoi, int thoiGianVongMoi) {
        int thoiGianCu = tongThoiGianChoi.getOrDefault(maNguoiChoi, 0);
        tongThoiGianChoi.put(maNguoiChoi, thoiGianCu + thoiGianVongMoi);
        System.out.println("Cập nhật tổng thời gian chơi của " + maNguoiChoi + ": " + tongThoiGianChoi.get(maNguoiChoi) + " giây");
    }

    public void guiBangXepHangCapDo(PrintWriter out) {
        List<Map.Entry<String, Integer>> bangXepHang = new ArrayList<>(capDoCaoNhat.entrySet());
        bangXepHang.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        StringBuilder sb = new StringBuilder("BANG_XEP_HANG_CAP_DO:");
        for (int i = 0; i < Math.min(10, bangXepHang.size()); i++) {
            Map.Entry<String, Integer> entry = bangXepHang.get(i);
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        out.println(sb.toString());
    }

    //  QUẢN LÝ CẤP ĐỘ CAO NHẤT 
    
    public synchronized void luuCapDoCaoNhatRaFile() {
        try {
            // Tạo thư mục data nếu chưa có
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CAP_DO_FILE))) {
                for (Map.Entry<String, Integer> entry : capDoCaoNhat.entrySet()) {
                    writer.write(entry.getKey() + ":" + entry.getValue());
                    writer.newLine();
                }
                System.out.println("Đã lưu cấp độ cao nhất vào file: " + CAP_DO_FILE);
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu cấp độ cao nhất: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void taiCapDoCaoNhatTuFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CAP_DO_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String playerName = parts[0];
                    int level = Integer.parseInt(parts[1]);
                    capDoCaoNhat.put(playerName, level);
                }
            }
            System.out.println("Đã tải " + capDoCaoNhat.size() + " record cấp độ cao nhất từ file");
        } catch (FileNotFoundException e) {
            System.out.println("File cấp độ cao nhất chưa tồn tại, sẽ tạo mới khi cần");
        } catch (IOException | NumberFormatException e) {
            System.err.println("Lỗi khi đọc file cấp độ cao nhất: " + e.getMessage());
        }
    }

    public synchronized void capNhatCapDoCaoNhat(String playerName, int newLevel) {
        int currentHighest = capDoCaoNhat.getOrDefault(playerName, 0);
        if (newLevel > currentHighest) {
            capDoCaoNhat.put(playerName, newLevel);
            luuCapDoCaoNhatRaFile(); // Lưu ngay khi có cập nhật
            System.out.println(playerName + " đạt cấp độ cao nhất mới: " + newLevel);
        }
    }

    //  UTILITY METHODS 
    
    private String taoMaPhongMoi() {
        return "ROOM_" + UUID.randomUUID().toString().substring(0, 8);
    }

    //  XỬ LÝ CLIENT 
    
    private void xuLyClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String tinNhan;
            while ((tinNhan = in.readLine()) != null) {
                System.out.println("Nhận: " + tinNhan);
                String[] phan = tinNhan.split(":");
                if (phan.length < 1) {
                    out.println("LOI: Định dạng lệnh không hợp lệ");
                    continue;
                }
                String lenh = phan[0];

                switch (lenh) {
                    case Constants.THAM_GIA:
                        xuLyLenhThamGia(clientSocket, phan, out);
                        break;

                    case Constants.DI_CHUYEN:
                        xuLyLenhDiChuyen(clientSocket, phan, out);
                        break;

                    case Constants.HOAN_THANH_CAP_DO:
                        xuLyHoanThanhCapDo(clientSocket);
                        break;

                    case Constants.NGAT_KET_NOI:
                        xuLyNgatKetNoi(clientSocket);
                        return;

                    case "SAN_SANG":
                        xuLyLenhSanSang(clientSocket);
                        break;

                    case "CAP_NHAT_THOI_GIAN":
                        xuLyCapNhatThoiGian(clientSocket, phan);
                        break;

                    case "LAY_BANG_XEP_HANG":
                        xuLyLayBangXepHang(out);
                        break;

                    case Constants.RROI_PHONG:
                        xuLyLenhRoiPhong(clientSocket, phan, out);
                        break;

                    case "LAY_BANG_XEP_HANG_CAP_DO":
                        guiBangXepHangCapDo(out);
                        break;

                    case "TIME_UPDATE":
                        // Xử lý cập nhật thời gian nếu cần
                        break;
                         case "COLLECT_ITEM":
        if (phan.length == 3) {
            String maNguoiChoi = danhSachSocketNguoiChoi.get(clientSocket);
            if (maNguoiChoi != null) {
                try {
                    int diemThem = Integer.parseInt(phan[2]);
                    capNhatDiemNguoiChoi(maNguoiChoi, diemThem);
                } catch (NumberFormatException e) {
                    System.err.println("Lỗi parse điểm item: " + phan[2]);
                }
            }
        }
        break;

                    default:
                        if (!"TIME_UPDATE".equals(lenh)) {
                            System.out.println("Lệnh không xác định: " + lenh);
                            out.println("LOI: Lệnh không xác định: " + lenh);
                        }
                        
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client ngắt kết nối: " + e.getMessage());
        } finally {
            xuLyNgatKetNoi(clientSocket);
        }
    }
public synchronized void capNhatDiemNguoiChoi(String maNguoiChoi, int diemThem) {
    int diemHienTai = playerScores.getOrDefault(maNguoiChoi, 0);
    diemHienTai += diemThem;
    playerScores.put(maNguoiChoi, diemHienTai);

    // Gửi điểm mới về client để cập nhật giao diện
    for (Map.Entry<String, GameRoom> entry : danhSachPhongChoi.entrySet()) {
        GameRoom phong = entry.getValue();
        PrintWriter out = phong.getWriterByPlayerId(maNguoiChoi);
        if (out != null) {
            out.println("UPDATE_SCORE:" + diemHienTai);
            break;
        }
    }

    System.out.println("Cập nhật điểm " + maNguoiChoi + ": " + diemHienTai);
}
    //  XỬ LÝ CÁC LỆNH CỤ THỂ 
    
    private void xuLyLenhThamGia(Socket clientSocket, String[] phan, PrintWriter out) {
        if (phan.length > 2) {
            String maNguoiChoi = phan[1];
            String maPhong = phan[2];
            xuLyVaoPhong(clientSocket, maNguoiChoi, maPhong, out);
        } else if (phan.length == 2) {
            // Trường hợp client chưa có mã phòng, tạo phòng mới
            String maNguoiChoi = phan[1];
            xuLyVaoPhong(clientSocket, maNguoiChoi, "", out);
        } else {
            out.println("LOI: Lệnh THAM_GIA cần playerId và optional roomId");
        }
    }

    private void xuLyLenhDiChuyen(Socket clientSocket, String[] phan, PrintWriter out) {
        if (phan.length > 3) {
            try {
                int x = Integer.parseInt(phan[1]);
                int y = Integer.parseInt(phan[2]);
                String huong = phan[3];
                xuLyDiChuyen(clientSocket, x, y, huong);
            } catch (NumberFormatException nfe) {
                out.println("LOI: Lệnh DI_CHUYEN cần x,y là số nguyên");
            }
        } else {
            out.println("LOI: Lệnh DI_CHUYEN cần x,y, hướng");
        }
    }

    private void xuLyLenhSanSang(Socket clientSocket) {
        String maNguoiChoi = danhSachSocketNguoiChoi.get(clientSocket);
        if (maNguoiChoi != null) {
            for (GameRoom phong : danhSachPhongChoi.values()) {
                if (phong.coNguoiChoi(maNguoiChoi)) {
                    phong.nguoiChoiSanSang(maNguoiChoi);
                    break;
                }
            }
        }
    }

    private void xuLyCapNhatThoiGian(Socket clientSocket, String[] phan) {
        if (phan.length == 2) {
            String maNguoiChoi = danhSachSocketNguoiChoi.get(clientSocket);
            if (maNguoiChoi != null) {
                try {
                    int thoiGianMoi = Integer.parseInt(phan[1]);
                    capNhatThoiGianChoi(maNguoiChoi, thoiGianMoi);
                } catch (NumberFormatException e) {
                    System.err.println("Lỗi định dạng thời gian chơi từ client: " + phan[1]);
                }
            }
        }
    }

    private void xuLyLayBangXepHang(PrintWriter out) {
        List<Map.Entry<String, Integer>> bangXepHang = new ArrayList<>(tongThoiGianChoi.entrySet());
        bangXepHang.sort(Comparator.comparingInt(Map.Entry::getValue));
        StringBuilder sb = new StringBuilder("BANG_XEP_HANG:");
        for (Map.Entry<String, Integer> entry : bangXepHang) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        out.println(sb.toString());
    }

    private void xuLyLenhRoiPhong(Socket clientSocket, String[] phan, PrintWriter out) {
        if (phan.length > 1) {
            String maNguoiChoi = phan[1];
            xuLyNguoiChoiRoiPhong(clientSocket, maNguoiChoi);
        } else {
            out.println("LOI: Lệnh RROI_PHONG cần playerId");
        }
    }

    //  XỬ LÝ PHÒNG CHƠI 
    
    private void xuLyVaoPhong(Socket socket, String maNguoiChoi, String maPhong, PrintWriter out) {
        System.out.println("[GameServer] Người chơi " + maNguoiChoi + " yêu cầu vào phòng: '" + maPhong + "'");
        try {
            if (maPhong != null && !maPhong.isEmpty()) {
                // Kiểm tra phòng có tồn tại và còn đủ người chơi không
                GameRoom phongCu = danhSachPhongChoi.get(maPhong);
                if (phongCu == null || phongCu.laySoNguoiChoi() >= Constants.MAX_PLAYERS_PER_ROOM) {
                    // Phòng không tồn tại hoặc đã đầy => tạo phòng mới
                    maPhong = null;
                }
            }

            if (maPhong == null || maPhong.isEmpty()) {
                // Tìm phòng chưa đủ người chơi để join
                for (Map.Entry<String, GameRoom> entry : danhSachPhongChoi.entrySet()) {
                    if (entry.getValue().laySoNguoiChoi() < Constants.MAX_PLAYERS_PER_ROOM) {
                        maPhong = entry.getKey();
                        break;
                    }
                }
                // Nếu không tìm thấy phòng trống hoặc maPhong vẫn rỗng thì tạo phòng mới
                if (maPhong == null || maPhong.isEmpty()) {
                    maPhong = taoMaPhongMoi();
                    System.out.println("Tạo phòng mới: " + maPhong);
                }
            }

            GameRoom phong = danhSachPhongChoi.get(maPhong);

            if (phong == null) {
                phong = new GameRoom(maPhong, this);
                danhSachPhongChoi.put(maPhong, phong);
                System.out.println("Tạo phòng mới: " + maPhong);
            }

            if (phong.laySoNguoiChoi() < Constants.MAX_PLAYERS_PER_ROOM) {
                PlayerType loai = phong.themNguoiChoi(socket, maNguoiChoi);
                danhSachSocketNguoiChoi.put(socket, maNguoiChoi);

                System.out.println("[GameServer] Gửi ROOM_DA_THAM_GIA với mã phòng: '" + maPhong + "'");
                out.println(Constants.ROOM_DA_THAM_GIA + ":" + maPhong + ":" + loai.name());
                System.out.println("Người chơi " + maNguoiChoi + " vào phòng " + maPhong + " với vai trò " + loai);

                if (phong.laySoNguoiChoi() == Constants.MAX_PLAYERS_PER_ROOM) {
                    System.out.println("Phòng " + maPhong + " đầy người, bắt đầu chơi...");
                    phong.batDauTroChoi();
                } else {
                    out.println(Constants.CHO_DOI_NGUOI_CHOI);
                    System.out.println("Người chơi " + maNguoiChoi + " đang chờ đối thủ...");
                }
            } else {
                out.println("PHONG_DAY");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void xuLyNguoiChoiRoiPhong(Socket socket, String maNguoiChoi) {
        System.out.println("Người chơi " + maNguoiChoi + " rời phòng.");
        for (GameRoom phong : danhSachPhongChoi.values()) {
            if (phong.coNguoiChoi(maNguoiChoi)) {
                // Xóa người chơi khỏi phòng
                phong.xoaNguoiChoi(maNguoiChoi);

                // Thông báo người chơi đã rời
                phong.phatTinNhanChoTatCa(Constants.NGUOI_CHOI_NGAT_KET_NOI + ":" + maNguoiChoi);

                // Kết thúc phòng ngay lập tức, đá người chơi còn lại về lobby
                phong.phatTinNhanChoTatCa("PHONG_KHONG_HOAT_DONG");

                // Xóa phòng khỏi danh sách server
                danhSachPhongChoi.remove(phong.layMaPhong());
                System.out.println("Phòng " + phong.layMaPhong() + " đã kết thúc và bị xóa do người chơi rời.");

                break;
            }
        }
    }

    //  XỬ LÝ GAME LOGIC 
    
    private void xuLyDiChuyen(Socket socket, int x, int y, String huong) {
        String maNguoiChoi = danhSachSocketNguoiChoi.get(socket);
        if (maNguoiChoi != null) {
            for (GameRoom phong : danhSachPhongChoi.values()) {
                if (phong.coNguoiChoi(maNguoiChoi)) {
                    phong.capNhatViTriNguoiChoi(maNguoiChoi, x, y, huong);
                    kiemTraDieuKienThang(phong);
                    break;
                }
            }
        }
    }

    private void kiemTraDieuKienThang(GameRoom phong) {
        System.out.println("Kiểm tra điều kiện thắng...");

        if (phong.laySoNguoiChoi() != Constants.MAX_PLAYERS_PER_ROOM) {
            System.out.println("Chưa đủ người chơi");
            return;
        }

        Point viTriCua = phong.layCapDoHienTai().getDoor();
        System.out.println("Vị trí cửa: " + viTriCua);

        boolean nuocTaiCua = false;
        boolean luaTaiCua = false;

        for (Map.Entry<String, Point> entry : phong.layViTriNguoiChoi().entrySet()) {
            String maNguoiChoi = entry.getKey();
            Point viTri = entry.getValue();
            PlayerType loai = phong.layLoaiNguoiChoi(maNguoiChoi);

            System.out.println("Người chơi " + loai + " tại: " + viTri);

            if (viTri.equals(viTriCua)) {
                if (PlayerType.WATER.equals(loai)) {
                    nuocTaiCua = true;
                    System.out.println("Nước ở cửa!");
                } else if (PlayerType.FIRE.equals(loai)) {
                    luaTaiCua = true;
                    System.out.println("Lửa ở cửa!");
                }
            }
        }

        if (nuocTaiCua && luaTaiCua) {
            System.out.println("Cả hai người chơi đã đến cửa - hoàn thành cấp độ!");

            if (phong.layChiSoCapDoHienTai() < Constants.TOTAL_LEVELS - 1) {
                phong.chuyenSangCapDoTiepTheo();
                phong.phatTinNhanChoTatCa(Constants.CAP_NHAT_CAP_DO_TIEP_THEO + ":" + (phong.layChiSoCapDoHienTai() + 1));

                Level capDoMoi = phong.layCapDoHienTai();
                String duLieuCapDo = "LEVEL_DATA:" + capDoMoi.getName() + ":" +
                        capDoMoi.getWidth() + "x" + capDoMoi.getHeight() + ":" +
                        "WALLS:" + capDoMoi.getWallsString() + ":" +
                        "WATER:" + capDoMoi.getWaterStart().x + "," + capDoMoi.getWaterStart().y + ":" +
                        "FIRE:" + capDoMoi.getFireStart().x + "," + capDoMoi.getFireStart().y + ":" +
                        "DOOR:" + capDoMoi.getDoor().x + "," + capDoMoi.getDoor().y;

                phong.phatTinNhanChoTatCa(duLieuCapDo);
                phong.datLaiViTriNguoiChoi();

            } else {
                phong.phatTinNhanChoTatCa(Constants.GAME_HOAN_THANH);
                System.out.println("Trò chơi đã hoàn thành!");
            }
        } else {
            System.out.println("Đang chờ cả hai người chơi đến cửa...");
        }
    }

    private void xuLyHoanThanhCapDo(Socket socket) {
        String maNguoiChoi = danhSachSocketNguoiChoi.get(socket);
        if (maNguoiChoi != null) {
            for (GameRoom phong : danhSachPhongChoi.values()) {
                if (phong.coNguoiChoi(maNguoiChoi)) {
                    phong.chuyenSangCapDoTiepTheo();
                    break;
                }
            }
        }
    }

    private void xuLyNgatKetNoi(Socket socket) {
        String maNguoiChoi = danhSachSocketNguoiChoi.remove(socket);
        if (maNguoiChoi != null) {
            System.out.println("Người chơi " + maNguoiChoi + " đã ngắt kết nối");
            for (GameRoom phong : danhSachPhongChoi.values()) {
                if (phong.coNguoiChoi(maNguoiChoi)) {
                    phong.xoaNguoiChoi(maNguoiChoi);
                    if (phong.laySoNguoiChoi() == 0) {
                        danhSachPhongChoi.remove(phong.layMaPhong());
                        System.out.println("Phòng " + phong.layMaPhong() + " đã được xóa");
                    }
                    break;
                }
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng socket: " + e.getMessage());
        }
    }

    //  SERVER  
    
    public void batDau() throws IOException {
        serverSocket = new ServerSocket(Constants.SERVER_PORT);
        dangChay = true;
        while (dangChay) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> xuLyClient(clientSocket)).start();
            } catch (IOException e) {
                if (dangChay) {
                    System.err.println("Lỗi khi chấp nhận kết nối client: " + e.getMessage());
                }
            }
        }
    }

    public void dungServer() {
        dangChay = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi dừng server: " + e.getMessage());
        }
    }

    //  MAIN  
    
    public static void main(String[] args) {
        GameServer server = new GameServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nĐang tắt server...");
            server.dungServer();
        }));

        try {
            server.batDau();
        } catch (IOException e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
