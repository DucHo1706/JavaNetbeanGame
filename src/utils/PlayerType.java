package utils;

public enum PlayerType {
    FIRE("FIRE"),
    WATER("WATER");
    
    private final String name;
    
    PlayerType(String name) {
        this.name = name;
    }
    
    public String getName() { 
        return name; 
    }
    
    public static PlayerType fromString(String type) {
        for (PlayerType pt : values()) {
            if (pt.name.equals(type)) {
                return pt;
            }
        }
        throw new IllegalArgumentException("Unknown player type: " + type);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
