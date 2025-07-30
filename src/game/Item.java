package game;

import java.awt.Point;
import utils.Constants;

public class Item {
    private int id;
    private Point position;
    private String type; // Dùng String như Monster
    private int points;
    private boolean collected;
    
    public Item(int id, Point position, String type) {
        this.id = id;
        this.position = new Point(position);
        this.type = type;
        this.points = getPointsByType(type);
        this.collected = false;
    }
    
    public Item(int id, int x, int y, String type) {
        this(id, new Point(x, y), type);
    }
    
    private int getPointsByType(String type) {
        switch (type) {
            case Constants.ITEM_COIN:
                return Constants.COIN_VALUE;
            case Constants.ITEM_GEM:
                return Constants.GEM_VALUE;
            case Constants.ITEM_CHEST:
                return Constants.CHEST_VALUE;
            default:
                return 0;
        }
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public Point getPosition() { return new Point(position); }
    public void setPosition(Point position) { this.position = new Point(position); }
    public String getType() { return type; }
    public int getPoints() { return points; }
    public boolean isCollected() { return collected; }
    public void setCollected(boolean collected) { this.collected = collected; }
    public int getX() { return position.x; }
    public int getY() { return position.y; }
    
    @Override
    public String toString() {
        return String.format("Item[id=%d, pos=(%d,%d), type=%s, points=%d, collected=%s]", 
                           id, position.x, position.y, type, points, collected);
    }
}
