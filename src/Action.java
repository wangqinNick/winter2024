/**
 * Action 类：表示我要下达的一条指令 (GROW 或 WAIT)。
 */
public class Action {

    // 动作类型
    public enum ActionType {
        BASIC,
        HARVESTER,
        TENTACLE,
        WAIT
    }

    private ActionType actionType;
    private int organId; // 父器官ID
    private int x;
    private int y;
    private char direction; // N, E, S, W, X

    /**
     * GROW 动作构造
     */
    public Action(int organId, int x, int y, ActionType actionType, char direction) {
        this.organId = organId;
        this.x = x;
        this.y = y;
        this.actionType = actionType;
        this.direction = direction;
    }

    /**
     * WAIT 动作构造
     */
    public Action() {
        this.actionType = ActionType.WAIT;
        this.organId = 0;
        this.x = 0;
        this.y = 0;
        this.direction = 'X';
    }

    @Override
    public String toString() {
        // 输出格式： GROW id x y type direction
        // 或 WAIT
        switch (actionType) {
            case WAIT:
                return "WAIT";
            case BASIC:
                return String.format("GROW %d %d %d BASIC %c", organId, x, y, direction);
            case HARVESTER:
                return String.format("GROW %d %d %d HARVESTER %c", organId, x, y, direction);
            case TENTACLE:
                return String.format("GROW %d %d %d TENTACLE %c", organId, x, y, direction);
            default:
                return "WAIT";
        }
    }

    // Getter
    public ActionType getActionType() {
        return actionType;
    }

    public int getOrganId() {
        return organId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public char getDirection() {
        return direction;
    }
}
