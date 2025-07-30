package server;

import game.Item;
import game.Monster;
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;

public class Level {
    private String name;
    private int width, height; // kích thước 
    private List<Point> walls; 
    private Point waterStart, fireStart, door; // vị trí của người chơi 
    private List<Monster> monsters;
    private List<Item> items; // Danh sách items
    private int totalPoints; // Tổng điểm có thể thu thập
    
    // constructor
    public Level(String name, int width, int height, List<Point> walls, 
                Point waterStart, Point fireStart, Point door) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.walls = walls != null ? new ArrayList<>(walls) : new ArrayList<>();
        this.waterStart = waterStart;
        this.fireStart = fireStart;
        this.door = door;
        this.monsters = new ArrayList<>();
        this.items = new ArrayList<>();
        this.totalPoints = 0;
    }
    
       public Level(String name, int width, int height, List<Point> walls, 
                Point waterStart, Point fireStart, Point door, List<Monster> monsters) {
        this(name, width, height, walls, waterStart, fireStart, door);
        this.monsters = monsters != null ? new ArrayList<>(monsters) : new ArrayList<>();
    }
    
    // Methods cho items
    public void addItem(Item item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        totalPoints += item.getPoints();
    }
    
    public void removeItem(int itemId) {
        if (items != null) {
            items.removeIf(item -> item.getId() == itemId);
        }
    }
    
    public Item getItemAt(Point position) {
        if (items == null) {
            return null;
        }
        return items.stream()
                   .filter(item -> !item.isCollected() && item.getPosition().equals(position))
                   .findFirst()
                   .orElse(null);
    }
    
    public List<Item> getActiveItems() {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                   .filter(item -> !item.isCollected())
                   .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    public List<Item> getItems() {
        if (items == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(items);
    }
    
    public int getTotalPoints() { 
        return totalPoints; 
    }
    
    // get set
    public String getName() { return name; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Point> getWalls() { return new ArrayList<>(walls); }
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
        return new ArrayList<>(monsters);
    }
    
    // Thêm một quái vật vào level
    public void addMonster(Monster monster) {
        this.monsters.add(monster);
    }
    
    // Xóa một quái vật khỏi level
    public void removeMonster(Monster monster) {
        this.monsters.remove(monster);
    }
    
    public void removeMonster(int monsterId) {
        monsters.removeIf(monster -> monster.getId() == monsterId);
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
