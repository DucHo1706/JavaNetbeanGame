package server;

import game.Monster;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;

public class Level {
    private String name; // tên của level
    private int width, height; // kích thước 
    private List<Point> walls; // danh sách tọa độ của bức tường 
    private Point waterStart, fireStart, door; // vị trí của người chơi 
    private List<Monster> monsters;
    // contructor
    public Level(String name, int width, int height, List<Point> walls, 
                Point waterStart, Point fireStart, Point door) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.walls = walls;
        this.waterStart = waterStart;
        this.fireStart = fireStart;
        this.door = door;
        this.monsters = new ArrayList<>();
       
    }
     // Constructor mới hỗ trợ quái vật
    public Level(String name, int width, int height, List<Point> walls, 
                Point waterStart, Point fireStart, Point door, List<Monster> monsters) {
        this(name, width, height, walls, waterStart, fireStart, door);
        this.monsters = monsters;
    }
    // get set
    public String getName() { return name; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Point> getWalls() { return walls; }
    public Point getWaterStart() { return waterStart; }
    public Point getFireStart() { return fireStart; }
    public Point getDoor() { return door; }
    
    public String getWallsString() {
        StringBuilder sb = new StringBuilder();
         // Duyệt qua từng tường
        for (int i = 0; i < walls.size(); i++) {
               // Thêm tọa độ: "x,y"
            Point wall = walls.get(i);
            sb.append(wall.x).append(",").append(wall.y);
            if (i < walls.size() - 1) sb.append(";");
        }
        return sb.toString();
    }
    
    public List<Monster> getMonsters() {
        return monsters;
    }
    
    // Thêm một quái vật vào level
    public void addMonster(Monster monster) {
        this.monsters.add(monster);
    }
    
    // Xóa một quái vật khỏi level
    public void removeMonster(Monster monster) {
        this.monsters.remove(monster);
    }
    
    // Kiểm tra xem có quái vật nào ở vị trí cụ thể không
    public Monster getMonsterAt(int x, int y) {
        for (Monster monster : monsters) {
            if (monster.getX() == x && monster.getY() == y) {
                return monster;
            }
        }
        return null;
    }
     // Cập nhật vị trí của tất cả quái vật
    public void updateMonsters() {
        for (Monster monster : monsters) {
            monster.updatePosition(this);
        }
    }
    
  
  
}
