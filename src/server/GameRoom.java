package server;

import game.Monster;
import utils.Constants;
import utils.PlayerType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;

public class GameRoom {
     private String maPhong;
    private Map<String, Socket> danhSachNguoiChoi;
    private Map<String, PrintWriter> luongGuiTinNhan;
    private Map<String, PlayerType> loaiNguoiChoi; // "FIRE" hoặc "WATER"
    private Map<String, Point> viTriNguoiChoi; // Lưu vị trí người chơi
    private int capDoHienTai;
    private boolean troChoiDaBatDau;
    private Set<String> nguoiChoiTaiCua;
    private Level duLieuCapDoHienTai;
    private Set<String> nguoiChoiSanSang;

    private Map<String, Integer> bangXepHang = new HashMap<>();

    private int thoiGianConLai; // tính bằng giây
    private Timer timer;
    private boolean manDaKetThuc = false;

    private GameServer server;

    public GameRoom(String maPhong) {
     this.maPhong = maPhong;
        this.server = server;
        this.danhSachNguoiChoi = new ConcurrentHashMap<>();
        this.luongGuiTinNhan = new ConcurrentHashMap<>();
        this.loaiNguoiChoi = new ConcurrentHashMap<>();
        this.viTriNguoiChoi = new ConcurrentHashMap<>();
        this.capDoHienTai = 1;
        this.troChoiDaBatDau = false;
        this.nguoiChoiTaiCua = new HashSet<>();
        this.nguoiChoiSanSang = new HashSet<>();
    }
    private void luuBangXepHangRaFile() {
        server.luuBangXepHangRaFile();
    }
     private void xuLyKetThucMan() {
        troChoiDaBatDau = false;
        manDaKetThuc = true;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        phatTinNhanChoTatCa("MAN_KET_THUC");
        System.out.println("Màn chơi trong phòng " + maPhong + " đã kết thúc do hết giờ.");

        nguoiChoiSanSang.clear();

        bangXepHang.clear();
        for (String nguoiChoi : danhSachNguoiChoi.keySet()) {
            int tongTG = server.getTongThoiGianChoi().getOrDefault(nguoiChoi, 0);
            bangXepHang.put(nguoiChoi, tongTG);
        }

        luuBangXepHangRaFile();

        phatTinNhanChoTatCa("YEU_CAU_SAN_SANG");
    }

    public void chuyenSangCapDoTiepTheo() {
        nguoiChoiTaiCua.clear();
        capDoHienTai++;

        if (capDoHienTai <= Constants.TOTAL_LEVELS) {
            duLieuCapDoHienTai = taoCapDo(capDoHienTai);
            System.out.println("Phòng " + maPhong + " chuyển sang cấp độ " + capDoHienTai);
            phatTinNhanChoTatCa(Constants.CAP_NHAT_CAP_DO_TIEP_THEO + ":" + capDoHienTai);
            guiDuLieuCapDo();
            datLaiViTriNguoiChoi();
            batDauDemThoiGian(60);
        } else {
            System.out.println("Phòng " + maPhong + " đã hoàn thành tất cả cấp độ!");
            phatTinNhanChoTatCa(Constants.GAME_HOAN_THANH);
            troChoiDaBatDau = false;

            bangXepHang.clear();
            for (String nguoiChoi : danhSachNguoiChoi.keySet()) {
                int tongTG = server.getTongThoiGianChoi().getOrDefault(nguoiChoi, 0);
                bangXepHang.put(nguoiChoi, tongTG);
            }
            luuBangXepHangRaFile();
        }
    }


   public PlayerType themNguoiChoi(Socket socket, String maNguoiChoi) {
    danhSachNguoiChoi.put(maNguoiChoi, socket);
    try {
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        luongGuiTinNhan.put(maNguoiChoi, writer);
    } catch (IOException e) {
        e.printStackTrace();
    }

    // Kiểm tra đã có FIRE hay WATER chưa
    boolean fireExists = loaiNguoiChoi.containsValue(PlayerType.FIRE);
    boolean waterExists = loaiNguoiChoi.containsValue(PlayerType.WATER);

    PlayerType loai;
    if (!fireExists) {
        loai = PlayerType.FIRE;
    } else if (!waterExists) {
        loai = PlayerType.WATER;
    } else {
        // Nếu phòng đầy, mặc định FIRE (hoặc có thể báo lỗi)
        loai = PlayerType.FIRE;
    }

    loaiNguoiChoi.put(maNguoiChoi, loai);

    if (duLieuCapDoHienTai != null) {
        if (PlayerType.WATER.equals(loai)) {
            viTriNguoiChoi.put(maNguoiChoi, new Point(duLieuCapDoHienTai.getWaterStart()));
        } else {
            viTriNguoiChoi.put(maNguoiChoi, new Point(duLieuCapDoHienTai.getFireStart()));
        }
    }

    System.out.println("Người chơi " + maNguoiChoi + " tham gia với vai trò " + loai);
      System.out.println("[GameRoom " + maPhong + "] Thêm người chơi " + maNguoiChoi + " với loại " + loai);
    return loai;
}

   public void xoaNguoiChoi(String maNguoiChoi) {
    danhSachNguoiChoi.remove(maNguoiChoi);
    luongGuiTinNhan.remove(maNguoiChoi);
    loaiNguoiChoi.remove(maNguoiChoi);
    viTriNguoiChoi.remove(maNguoiChoi);
    nguoiChoiTaiCua.remove(maNguoiChoi);
    nguoiChoiSanSang.remove(maNguoiChoi);

    if (danhSachNguoiChoi.size() < Constants.MAX_PLAYERS_PER_ROOM) {
        troChoiDaBatDau = false;
        manDaKetThuc = false;
        capDoHienTai = 1;  // Reset về cấp độ 1
        nguoiChoiTaiCua.clear();
        nguoiChoiSanSang.clear();
        System.out.println("Phòng " + maPhong + " đã reset trạng thái do thiếu người chơi.");
    }

    if (danhSachNguoiChoi.size() == 1) {
        // Gửi thông báo người chơi rời phòng
        phatTinNhanChoTatCa(Constants.NGUOI_CHOI_NGAT_KET_NOI + ":" + maNguoiChoi);
        // Gửi lệnh bắt client còn lại thoát về lobby
        phatTinNhanChoTatCa("PHONG_KHONG_HOAT_DONG");
    }
}

    public void batDauTroChoi() {
        troChoiDaBatDau = true;
        duLieuCapDoHienTai = taoCapDo(capDoHienTai);
        System.out.println("Bắt đầu trò chơi phòng " + maPhong + " - Cấp độ " + capDoHienTai);
        phatTinNhanChoTatCa("BAT_DAU_TRO_CHOI:CAP_DO:" + capDoHienTai);
        guiDuLieuCapDo();

        datLaiViTriNguoiChoi();
        batDauDemThoiGian(60);
    }
     // Hàm bắt đầu đếm ngược thời gian màn chơi
   public void batDauDemThoiGian(int thoiGianBatDau) {
    thoiGianConLai = thoiGianBatDau;
    manDaKetThuc = false;
    troChoiDaBatDau = true;  // Bắt đầu game

    // Hủy timer cũ nếu còn chạy
    if (timer != null) {
        timer.cancel();
    }

    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            if (manDaKetThuc || !troChoiDaBatDau) {
                timer.cancel();
                return;
            }

            if (thoiGianConLai > 0) {
                thoiGianConLai--;
                phatTinNhanChoTatCa("THOI_GIAN_CON_LAI:" + thoiGianConLai);
            } else {
                manDaKetThuc = true;
                troChoiDaBatDau = false;
                timer.cancel();
                xuLyKetThucMan();
            }
        }
    }, 0, 1000);
}



public void nguoiChoiSanSang(String maNguoiChoi) {
    nguoiChoiSanSang.add(maNguoiChoi); 

    if (nguoiChoiSanSang.size() == danhSachNguoiChoi.size()) {
        nguoiChoiSanSang.clear();  // reset trạng thái
        troChoiDaBatDau = true;
        manDaKetThuc = false;
        phatTinNhanChoTatCa("BAT_DAU_TRO_CHOI:CAP_DO:" + capDoHienTai);
        batDauDemThoiGian(60);
        guiDuLieuCapDo();
        datLaiViTriNguoiChoi();
        System.out.println("Phòng " + maPhong + " bắt đầu lại màn chơi sau khi cả 2 người chơi sẵn sàng.");
    }
}





  public void capNhatViTriNguoiChoi(String maNguoiChoi, int x, int y, String huong) {
    if (troChoiDaBatDau && !manDaKetThuc) {
        viTriNguoiChoi.put(maNguoiChoi, new Point(x, y));
        phatTinNhanChoNguoiKhac(maNguoiChoi, "NGUOI_CHOI_DI_CHUYEN:" + maNguoiChoi + ":" + x + ":" + y + ":" + huong);

        if (duLieuCapDoHienTai != null) {
            Point viTriCua = duLieuCapDoHienTai.getDoor();
            if (viTriCua != null && viTriCua.x == x && viTriCua.y == y) {
                nguoiChoiTaiCua.add(maNguoiChoi);
                if (nguoiChoiTaiCua.size() == Constants.MAX_PLAYERS_PER_ROOM) {
                    chuyenSangCapDoTiepTheo();
                }
            } else {
                nguoiChoiTaiCua.remove(maNguoiChoi);
            }
        }
    }
}





    private void guiDuLieuCapDo() {
        String duLieu = taoChuoiDuLieuCapDo(capDoHienTai);
        phatTinNhanChoTatCa("DU_LIEU_CAP_DO:" + duLieu);
        System.out.println("Đã gửi dữ liệu cấp độ " + capDoHienTai + " cho phòng " + maPhong);
    }

    private String taoChuoiDuLieuCapDo(int capDo) {
        switch (capDo) {
            case 1:
                return "SIMPLE:24x16:WALLS:" +
                        "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" +
                        "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" +
                        "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" +
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
                   
            case 3 :
                 // LEVEL 3: Mê cung thử thách cuối cùng
             return "HARD:24x16:WALLS:" +
                 // Outer walls (top, left, right, bottom excluding door)
                 "0,0;1,0;2,0;3,0;4,0;5,0;6,0;7,0;8,0;9,0;10,0;11,0;12,0;13,0;14,0;15,0;16,0;17,0;18,0;19,0;20,0;21,0;22,0;23,0;" + // Top
                 "0,1;0,2;0,3;0,4;0,5;0,6;0,7;0,8;0,9;0,10;0,11;0,12;0,13;0,14;0,15;" + // Left
                 "23,1;23,2;23,3;23,4;23,5;23,6;23,7;23,8;23,9;23,10;23,11;23,12;23,13;23,14;23,15;" + // Right
                 "0,15;1,15;2,15;3,15;4,15;5,15;6,15;7,15;8,15;9,15;10,15;12,15;13,15;14,15;15,15;16,15;17,15;18,15;19,15;20,15;21,15;22,15;23,15;" + // Bottom (door at 11,15)

                 // Inner maze - Tạo đường đi rõ ràng nhưng phức tạp
                 // Tạo các "phòng" nhỏ kết nối với nhau

                 // Phòng trên bên trái (1,1 -> 7,7)
                 "2,2;3,2;4,2;5,2;6,2;" +
                 "2,3;6,3;" +
                 "2,4;6,4;" +
                 "2,5;3,5;4,5;5,5;6,5;" +
                 "2,6;6,6;" +

                 // Phòng trên bên phải (9,1 -> 15,7)  
                 "9,2;10,2;11,2;12,2;13,2;" +
                 "9,3;13,3;" +
                 "9,4;13,4;" +
                 "9,5;10,5;11,5;12,5;13,5;" +
                 "9,6;13,6;" +

                 // Phòng trên cùng bên phải (17,1 -> 22,7)
                 "17,2;18,2;19,2;20,2;21,2;" +
                 "17,3;21,3;" +
                 "17,4;21,4;" +
                 "17,5;18,5;19,5;20,5;21,5;" +
                 "17,6;21,6;" +

                 // Hành lang ngang ở giữa (tạo đường đi chính)
                 "1,8;3,8;5,8;7,8;9,8;11,8;13,8;15,8;17,8;19,8;21,8;22,8;" +

                 // Phòng dưới bên trái (1,9 -> 7,14)
                 "2,9;6,9;" +
                 "2,10;3,10;4,10;5,10;6,10;" +
                 "2,11;6,11;" +
                 "2,12;6,12;" +
                 "2,13;3,13;4,13;5,13;6,13;" +

                 // Phòng dưới giữa (9,9 -> 15,14)
                 "9,9;13,9;" +
                 "9,10;10,10;11,10;12,10;13,10;" +
                 "9,11;13,11;" +
                 "9,12;13,12;" +
                 "9,13;10,13;11,13;12,13;13,13;" +

                 // Phòng dưới bên phải (17,9 -> 22,14)
                 "17,9;21,9;" +
                 "17,10;18,10;19,10;20,10;21,10;" +
                 "17,11;21,11;" +
                 "17,12;21,12;" +
                 "17,13;18,13;19,13;20,13;21,13" +

                 ":WATER:22,1:FIRE:1,1:DOOR:11,15" + // Player positions và Door

                 ":MONSTERS:" +
                 // Red Slime trong các phòng (di chuyển ngang)
                 "3,3,TRAIPHAI;4,3,TRAIPHAI;" +          // Phòng trên trái
                 "10,3,TRAIPHAI;11,3,TRAIPHAI;" +        // Phòng trên giữa  
                 "18,3,TRAIPHAI;19,3,TRAIPHAI;" +        // Phòng trên phải
                 "3,11,TRAIPHAI;4,11,TRAIPHAI;" +        // Phòng dưới trái
                 "10,11,TRAIPHAI;11,11,TRAIPHAI;" +      // Phòng dưới giữa
                 "18,11,TRAIPHAI;19,11,TRAIPHAI;" +      // Phòng dưới phải

                 // Green Slime ở hành lang (di chuyển dọc)
                 "8,2,LENXUONG;8,5,LENXUONG;" +          // Kết nối phòng trên
                 "16,2,LENXUONG;16,5,LENXUONG;" +        // Kết nối phòng trên
                 "8,10,LENXUONG;8,13,LENXUONG;" +        // Kết nối phòng dưới
                 "16,10,LENXUONG;16,13,LENXUONG;" +      // Kết nối phòng dưới

                 // Spider ở các điểm chiến lược (đứng yên)
                 "7,7,TINH;15,7,TINH;" +                 // Canh gác lối vào hành lang
                 "1,7,TINH;22,7,TINH;" +                 // Canh gác 2 đầu hành lang
                 "7,9,TINH;15,9,TINH;";

            default:
                return taoChuoiDuLieuCapDo(3);
        }
    }

    private Level taoCapDo(int capDo) {
        String duLieu = taoChuoiDuLieuCapDo(capDo);
        return phanTichDuLieuCapDo(duLieu);
    }

    private Level phanTichDuLieuCapDo(String duLieu) {
        String[] phan = duLieu.split(":");

        String doKho = phan[0];

        String[] kichThuoc = phan[1].split("x");
        int rong = Integer.parseInt(kichThuoc[0]);
        int cao = Integer.parseInt(kichThuoc[1]);

        List<Point> tuong = new ArrayList<>();
        if (phan.length > 3 && phan[2].equals("WALLS")) {
            String[] toaDoTuong = phan[3].split(";");
            for (String toaDo : toaDoTuong) {
                if (!toaDo.trim().isEmpty()) {
                    String[] xy = toaDo.split(",");
                    tuong.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                }
            }
        }

        Point viTriNuoc = null;
        for (int i = 4; i < phan.length - 1; i++) {
            if (phan[i].equals("WATER")) {
                String[] toaDo = phan[i + 1].split(",");
                viTriNuoc = new Point(Integer.parseInt(toaDo[0]), Integer.parseInt(toaDo[1]));
                break;
            }
        }

        Point viTriLua = null;
        for (int i = 4; i < phan.length - 1; i++) {
            if (phan[i].equals("FIRE")) {
                String[] toaDo = phan[i + 1].split(",");
                viTriLua = new Point(Integer.parseInt(toaDo[0]), Integer.parseInt(toaDo[1]));
                break;
            }
        }

        Point cua = null;
        for (int i = 4; i < phan.length - 1; i++) {
            if (phan[i].equals("DOOR")) {
                String[] toaDo = phan[i + 1].split(",");
                cua = new Point(Integer.parseInt(toaDo[0]), Integer.parseInt(toaDo[1]));
                break;
            }
        }

        return new Level(doKho, rong, cao, tuong, viTriNuoc, viTriLua, cua);
    }

    public Map<String, Point> layViTriNguoiChoi() {
        return viTriNguoiChoi;
    }

    public PlayerType layLoaiNguoiChoi(String maNguoiChoi) {
        return loaiNguoiChoi.get(maNguoiChoi);
    }

    public Level layCapDoHienTai() {
        return duLieuCapDoHienTai;
    }

    public int layChiSoCapDoHienTai() {
        return capDoHienTai - 1; // 0-based index
    }

    public int layTongSoCapDo() {
        return Constants.TOTAL_LEVELS;
    }

    public void datLaiViTriNguoiChoi() {
        if (duLieuCapDoHienTai != null) {
            for (Map.Entry<String, PlayerType> entry : loaiNguoiChoi.entrySet()) {
                String maNguoiChoi = entry.getKey();
                PlayerType loai = entry.getValue();

                if (PlayerType.WATER.equals(loai)) {
                    viTriNguoiChoi.put(maNguoiChoi, new Point(duLieuCapDoHienTai.getWaterStart()));
                } else if (PlayerType.FIRE.equals(loai)) {
                    viTriNguoiChoi.put(maNguoiChoi, new Point(duLieuCapDoHienTai.getFireStart()));
                }
            }
        }
    }

    public void phatTinNhanChoTatCa(String tinNhan) {
        System.out.println("Phát tới phòng " + maPhong + ": " + tinNhan);
        for (PrintWriter writer : luongGuiTinNhan.values()) {
            writer.println(tinNhan);
        }
    }

    private void phatTinNhanChoNguoiKhac(String maNguoiChoiTru, String tinNhan) {
        for (Map.Entry<String, PrintWriter> entry : luongGuiTinNhan.entrySet()) {
            if (!entry.getKey().equals(maNguoiChoiTru)) {
                entry.getValue().println(tinNhan);
            }
        }
    }

    public int laySoNguoiChoi() {
        return danhSachNguoiChoi.size();
    }

    public boolean coNguoiChoi(String maNguoiChoi) {
        return danhSachNguoiChoi.containsKey(maNguoiChoi);
    }

    public String layMaPhong() {
        return maPhong;
    }
}