// Direction.java
enum Direction {
    NORTH('N'),
    EAST('E'),
    SOUTH('S'),
    WEST('W'),
    NONE('X'); // 用于表示无方向或不适用的情况

    private final char symbol;

    Direction(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    /**
     * 根据字符返回对应的 Direction 枚举值。
     *
     * @param symbol 方向字符（'N', 'E', 'S', 'W', 'X'）
     * @return 对应的 Direction 枚举值
     */
    public static Direction fromSymbol(char symbol) {
        for (Direction dir : Direction.values()) {
            if (dir.symbol == symbol) {
                return dir;
            }
        }
        return NONE; // 默认返回 NONE
    }
}
