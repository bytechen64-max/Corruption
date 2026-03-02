package corruption.world.data;

public enum DifficultyLevel {
    EASY(1, "easy"),
    NORMAL(2, "normal"),
    HARD(4, "hard"),
    IMPOSSIBLE(8, "impossible");

    private final int value;
    private final String name;

    DifficultyLevel(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() { return value; }  // 返回固定值（1,2,4,8）
    public String getName() { return name; }

    public static DifficultyLevel fromValue(int value) {
        for (DifficultyLevel level : values()) {
            if (level.value == value) return level;
        }
        return NORMAL; // 默认
    }

    // 如果需要，也可以保留 fromName
    public static DifficultyLevel fromName(String name) {
        for (DifficultyLevel level : values()) {
            if (level.name.equals(name)) return level;
        }
        return NORMAL;
    }
}