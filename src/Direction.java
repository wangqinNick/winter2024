/**
 * 枚举类：方向
 */
enum Direction {
    NORTH('N'),
    EAST('E'),
    SOUTH('S'),
    WEST('W'),
    NONE('X');

    private final char symbol;

    Direction(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public static Direction fromSymbol(char c) {
        for (Direction d : values()) {
            if (d.symbol == c) {
                return d;
            }
        }
        return NONE;
    }
}
