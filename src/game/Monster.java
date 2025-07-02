package game;
import java.awt.Point;
import server.Level;

public class Monster {
    private int id;
    private Point position;
    private String type; // "TINH", "LENXUONG", "TRAIPHAI"
    private int moveInterval;
    private long lastMoveTime;
    private String direction; // "UP", "DOWN", "LEFT", "RIGHT"
    private Point startPosition; // Luu vi tri ban dau
    private int moveRange; // Pham vi di chuyen
    
    public Monster(int id, Point position, String type) {
        this.id = id;
        this.position = new Point(position);
        this.startPosition = new Point(position); // Luu vi tri ban dau
        this.type = type;
        this.moveRange = 3; // Mac dinh di chuyen toi da 3 o
        
        // Thiet lap tham so theo loai quai vat
        if ("TINH".equals(type)) {
            moveInterval = 0; // Khong di chuyen
        } else if ("LENXUONG".equals(type)) {
            moveInterval = 1500; // Di chuyen moi 1.5 giay
            direction = "UP"; // Bat dau di len
        } else if ("TRAIPHAI".equals(type)) {
            moveInterval = 1200; // Di chuyen moi 1.2 giay
            direction = "LEFT"; // Bat dau di trai
        }
        
        this.lastMoveTime = System.currentTimeMillis();
    }
    
    // Getters co ban
    public int getId() { 
        return id; 
    }
    
    public Point getPosition() { 
        return new Point(position); 
    }
    
    public int getX() { 
        return position.x; 
    }
    
    public int getY() { 
        return position.y; 
    }
    
    public String getType() { 
        return type; 
    }
    
    public String getDirection() { 
        return direction; 
    }
    
    // Setters
    public void setPosition(Point position) {
        this.position.setLocation(position);
    }
    
    public void setMoveRange(int range) {
        this.moveRange = range;
    }
    
    public void updatePosition(Level level) {
    if ("TINH".equals(type) || moveInterval == 0) {
        return; // Quai vat tinh khong di chuyen
    }
    
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastMoveTime >= moveInterval) {
        move(level);
        lastMoveTime = currentTime;
    }
}

    private void move(Level level) {
        Point newPosition = new Point(position);

        if ("LENXUONG".equals(type)) {
            moveLenXuong(newPosition, level);
        } else if ("TRAIPHAI".equals(type)) {
            moveTraiPhai(newPosition, level);
        }

        // Kiem tra vi tri moi co hop le khong
        if (isValidPosition(newPosition, level)) {
            position.setLocation(newPosition);
        } else {
            // Neu khong hop le, doi huong
            reverseDirection();
        }
    }

    private void moveLenXuong(Point newPos, Level level) {
        if ("UP".equals(direction)) {
            newPos.y--;
            // Kiem tra co vuot qua pham vi khong
            if (newPos.y < startPosition.y - moveRange || !isValidPosition(newPos, level)) {
                direction = "DOWN";
                newPos.y = position.y + 1;
            }
        } else { // DOWN
            newPos.y++;
            // Kiem tra co vuot qua pham vi khong
            if (newPos.y > startPosition.y + moveRange || !isValidPosition(newPos, level)) {
                direction = "UP";
                newPos.y = position.y - 1;
            }
        }
    }
    private void moveTraiPhai(Point newPos, Level level) {
    if ("LEFT".equals(direction)) {
        newPos.x--;
        // Kiem tra co vuot qua pham vi khong
        if (newPos.x < startPosition.x - moveRange || !isValidPosition(newPos, level)) {
            direction = "RIGHT";
            newPos.x = position.x + 1;
        }
    } else { // RIGHT
        newPos.x++;
        // Kiem tra co vuot qua pham vi khong
        if (newPos.x > startPosition.x + moveRange || !isValidPosition(newPos, level)) {
            direction = "LEFT";
            newPos.x = position.x - 1;
        }
    }
}

    private void reverseDirection() {
        switch (direction) {
            case "UP":
                direction = "DOWN";
                break;
            case "DOWN":
                direction = "UP";
                break;
            case "LEFT":
                direction = "RIGHT";
                break;
            case "RIGHT":
                direction = "LEFT";
                break;
        }
    }

    private boolean isValidPosition(Point pos, Level level) {
        // Kiem tra co trong pham vi map khong
        if (pos.x < 0 || pos.x >= level.getWidth() || 
            pos.y < 0 || pos.y >= level.getHeight()) {
            return false;
        }

        // Kiem tra co phai tuong khong
        return !level.getWalls().contains(pos);
    }
}
