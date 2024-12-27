enum Direction {
    NORTH('N'),
    EAST('E'),
    SOUTH('S'),
    WEST('W'),
    NONE('X');

    private final char symbol;
    Direction(char s){symbol=s;}
    public char getSymbol(){return symbol;}

    public static Direction fromSymbol(char c){
        for(Direction d: values()){
            if(d.symbol== c) return d;
        }
        return NONE;
    }
}
