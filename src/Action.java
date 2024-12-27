import java.util.Objects;

public class Action {

    public enum ActionType {
        BASIC,
        HARVESTER,
        TENTACLE,
        SPORER,
        SPORE,    // 新增：用于发射 Spores (生成新 ROOT)
        WAIT
    }

    private ActionType actionType;
    private int organId;  // 父器官ID (或SPORER的ID)
    private int x, y;
    private char direction; // N,E,S,W,X

    public Action(int organId, int x, int y, ActionType type, char direction) {
        this.organId = organId;
        this.x = x;
        this.y = y;
        this.actionType = type;
        this.direction = direction;
    }

    // SPORE 构造：SPORE id x y (direction 不适用)
    public Action(int sporerId, int x, int y) {
        this.actionType = ActionType.SPORE;
        this.organId = sporerId;
        this.x = x;
        this.y = y;
        this.direction = 'X';  // 不需要方向
    }

    // WAIT 构造
    public Action() {
        this.actionType = ActionType.WAIT;
        this.organId = 0;
        this.x = 0;
        this.y = 0;
        this.direction = 'X';
    }

    @Override
    public String toString() {
        // 多种指令格式
        switch (actionType) {
            case WAIT:
                return "WAIT";
            case BASIC:
                return String.format("GROW %d %d %d BASIC %c", organId, x, y, direction);
            case HARVESTER:
                return String.format("GROW %d %d %d HARVESTER %c", organId, x, y, direction);
            case TENTACLE:
                return String.format("GROW %d %d %d TENTACLE %c", organId, x, y, direction);
            case SPORER:
                return String.format("GROW %d %d %d SPORER %c", organId, x, y, direction);
            case SPORE:
                // SPORE id x y
                return String.format("SPORE %d %d %d", organId, x, y);
            default:
                return "WAIT";
        }
    }

    public ActionType getActionType() {
        return actionType;
    }
    public int getOrganId() { return organId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public char getDirection() { return direction; }
}
