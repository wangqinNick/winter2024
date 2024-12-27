// Action.java
public class Action {
    // 动作类型枚举
    public enum ActionType {
        BASIC,
        HARVESTER,
        WAIT // 特殊的等待动作
    }

    private ActionType actionType;

    // GROW 动作相关属性
    private int organId;
    private int x;
    private int y;
    private char direction;   // N, S, W, E 或 X

    /**
     * 构造 GROW 动作。
     *
     * @param organId    父器官 ID
     * @param x          目标 X 坐标
     * @param y          目标 Y 坐标
     * @param actionType 器官类型（BASIC 或 HARVESTER）
     * @param direction  方向（N, S, W, E 或 X）
     */
    public Action(int organId, int x, int y, ActionType actionType, char direction) {
        this.actionType = actionType;
        this.organId = organId;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    /**
     * 构造 WAIT 动作。
     */
    public Action() {
        this.actionType = ActionType.WAIT;
        this.direction = 'X';
        this.organId = 0;
        this.x = 0;
        this.y = 0;
    }

    /**
     * 将动作转为字符串输出。
     *
     * @return 动作命令字符串
     */
    @Override
    public String toString() {
        switch (actionType) {
            case WAIT:
                return "WAIT";
            case HARVESTER:
                return String.format("GROW %d %d %d HARVESTER %c", organId, x, y, direction);
            case BASIC:
                return String.format("GROW %d %d %d BASIC %c", organId, x, y, direction);
            default:
                return "WAIT"; // 默认返回 WAIT
        }
    }

    // Getter 和 Setter 方法

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
