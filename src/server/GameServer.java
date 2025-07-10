package server;

import utils.Constants;
import utils.PlayerType;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;

public class GameServer {
    private ServerSocket serverSocket;
    private Map<String, GameRoom> danhSachPhongChoi;
    private Map<Socket, String> danhSachSocketNguoiChoi;
    private boolean dangChay;

    public GameServer() {
        danhSachPhongChoi = new ConcurrentHashMap<>();
        danhSachSocketNguoiChoi = new ConcurrentHashMap<>();
        dangChay = false;
    }

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
                        if (phan.length > 1) {
                            String maNguoiChoi = phan[1];
                            thamGiaTroChoi(clientSocket, maNguoiChoi, out);
                        } else {
                            out.println("LOI: Lệnh THAM_GIA cần playerId");
                        }
                        break;

                    case Constants.DI_CHUYEN:
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
                        break;

                    case Constants.HOAN_THANH_CAP_DO:
                        xuLyHoanThanhCapDo(clientSocket);
                        break;

                    case Constants.NGAT_KET_NOI:
                        xuLyNgatKetNoi(clientSocket);
                        return;
                   
                    default:
                        out.println("LOI: Lệnh không xác định: " + lenh);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client ngắt kết nối: " + e.getMessage());
        } finally {
            xuLyNgatKetNoi(clientSocket);
        }
    }


    private void thamGiaTroChoi(Socket socket, String maNguoiChoi, PrintWriter out) {
        danhSachSocketNguoiChoi.put(socket, maNguoiChoi);

        GameRoom phongTrong = null;
        for (GameRoom phong : danhSachPhongChoi.values()) {
            if (phong.laySoNguoiChoi() < Constants.MAX_PLAYERS_PER_ROOM) {
                phongTrong = phong;
                break;
            }
        }

        if (phongTrong == null) {
            phongTrong = new GameRoom(UUID.randomUUID().toString());
            danhSachPhongChoi.put(phongTrong.layMaPhong(), phongTrong);
            System.out.println("Tạo phòng mới: " + phongTrong.layMaPhong());
        }

        PlayerType loai = phongTrong.themNguoiChoi(socket, maNguoiChoi);
        out.println(Constants.ROOM_DA_THAM_GIA + ":" + phongTrong.layMaPhong() + ":" + loai);

        if (phongTrong.laySoNguoiChoi() == Constants.MAX_PLAYERS_PER_ROOM) {
            System.out.println("Phòng " + phongTrong.layMaPhong() + " đầy người, bắt đầu chơi...");
            phongTrong.batDauTroChoi();
        } else {
            out.println(Constants.CHO_DOI_NGUOI_CHOI);
            System.out.println("Người chơi " + maNguoiChoi + " đang chờ đối thủ...");
        }
    }

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