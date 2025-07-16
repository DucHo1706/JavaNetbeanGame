package utils;

import java.awt.Color;

public class Constants {
    public static final int SERVER_PORT = 8080; // port server
    public static final String SERVER_HOST = "localhost";
    public static final int CELL_SIZE = 40; // kích thước mỗi ô
    public static final int MAX_PLAYERS_PER_ROOM = 2;
    public static final int TOTAL_LEVELS = 3;
    public static final String MONSTER_TINH = "TINH"; // tạo quái vật tĩnh đứng yên
    public static final String MONSTER_DUYCHUYEN = "DuyChuyen"; // tạo quái vật duy chuyển qua lại
    
    
      // Các lệnh client gửi lên server
    public static final String THAM_GIA = "THAM_GIA";
    public static final String DI_CHUYEN = "DI_CHUYEN";
    public static final String HOAN_THANH_CAP_DO = "HOAN_THANH_CAP_DO";
    public static final String NGAT_KET_NOI = "NGAT_KET_NOI";

    
      // Các response server gửi về client 
    public static final String ROOM_DA_THAM_GIA = "PHONG_DA_THAM_GIA";
    public static final String CHO_DOI_NGUOI_CHOI = "CHO_DOI_NGUOI_CHOI";
    public static final String CAP_NHAT_CAP_DO_TIEP_THEO = "CAP_NHAT_CAP_DO_TIEP_THEO";
    public static final String GAME_HOAN_THANH = "GAME_HOAN_THANH";
    
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String FIRE = "FIRE";
    public static final String WATER = "WATER";
    
     // Các lệnh server gửi về client 
    public static final String BAT_DAU_TRO_CHOI = "BAT_DAU_TRO_CHOI";
    public static final String DU_LIEU_CAP_DO = "DU_LIEU_CAP_DO";
    public static final String NGUOI_CHOI_DI_CHUYEN = "NGUOI_CHOI_DI_CHUYEN";
    public static final String NGUOI_CHOI_NGAT_KET_NOI = "NGUOI_CHOI_NGAT_KET_NOI";

    public static final String NUT_BAT_DAU = "NUT_BAT_DAU";

    
}
