public class Action {

    public enum ActionType {
        BASIC,
        HARVESTER,
        TENTACLE,
        SPORER,
        SPORE,
        WAIT
    }

    private ActionType type;
    private int organId;
    private int x,y;
    private char direction;

    // GROW构造
    public Action(int organId,int x,int y, ActionType t,char dir){
        this.type= t;
        this.organId= organId;
        this.x= x;
        this.y= y;
        this.direction= dir;
    }

    // SPORE 构造
    public Action(int sporerId,int x,int y){
        this.type= ActionType.SPORE;
        this.organId= sporerId;
        this.x= x;
        this.y= y;
        this.direction= 'X';
    }

    // WAIT
    public Action(){
        this.type= ActionType.WAIT;
        this.organId= 0;
        this.x=0;
        this.y=0;
        this.direction='X';
    }

    @Override
    public String toString(){
        switch(type){
            case WAIT:
                return "WAIT";
            case BASIC:
                return String.format("GROW %d %d %d BASIC %c", organId,x,y,direction);
            case HARVESTER:
                return String.format("GROW %d %d %d HARVESTER %c", organId,x,y,direction);
            case TENTACLE:
                return String.format("GROW %d %d %d TENTACLE %c", organId,x,y,direction);
            case SPORER:
                return String.format("GROW %d %d %d SPORER %c", organId,x,y,direction);
            case SPORE:
                return String.format("SPORE %d %d %d", organId,x,y);
            default:
                return "WAIT";
        }
    }

    public ActionType getActionType(){return type;}
    public int getOrganId(){return organId;}
    public int getX(){return x;}
    public int getY(){return y;}
    public char getDirection(){return direction;}
}
