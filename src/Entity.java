// Entity.java
public class Entity implements Cloneable {
    public enum EntityType {
        WALL, ROOT, BASIC, HARVESTER, A, B, C, D
    }

    public enum Owner {
        SELF, OPPONENT, NONE
    }

    public int x, y;
    public EntityType type;
    public Owner owner;
    public int organId;         // 器官ID (非器官则为0)
    public Direction direction; // 方向属性
    public int organParentId;
    public int organRootId;

    // 新增属性：资源容量
    public int capacityC; // 表示当前采集器的 C 资源容量
    public int capacityD; // 表示当前采集器的 D 资源容量

    /**
     * 构造函数，初始化实体属性。
     *
     * @param x              X 坐标
     * @param y              Y 坐标
     * @param type           实体类型
     * @param owner          所有者
     * @param organId        器官 ID
     * @param direction      器官方向（使用 Direction 枚举）
     * @param organParentId  父器官 ID
     * @param organRootId    ROOT 器官 ID
     */
    public Entity(int x, int y, EntityType type, Owner owner, int organId,
                  Direction direction, int organParentId, int organRootId) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
        this.organId = organId;
        this.direction = direction;
        this.organParentId = organParentId;
        this.organRootId = organRootId;

        // 初始化资源容量
        if (type == EntityType.HARVESTER) {
            this.capacityC = 1; // 初始资源，根据游戏规则调整
            this.capacityD = 1;
        } else {
            this.capacityC = 0;
            this.capacityD = 0;
        }
    }

    /**
     * 深拷贝构造函数。
     *
     * @param other 需要被拷贝的 Entity 对象
     */
    public Entity(Entity other) {
        this.x = other.x;
        this.y = other.y;
        this.type = other.type;
        this.owner = other.owner;
        this.organId = other.organId;
        this.direction = other.direction;
        this.organParentId = other.organParentId;
        this.organRootId = other.organRootId;
        this.capacityC = other.capacityC;
        this.capacityD = other.capacityD;
    }

    /**
     * 克隆方法。
     */
    @Override
    public Entity clone() {
        return new Entity(this);
    }

    /**
     * 将实体的方向符号转换为枚举值。
     *
     * @param dirSymbol 方向字符（'N', 'E', 'S', 'W', 'X'）
     * @return 对应的 Direction 枚举值
     */
    public static Direction parseDirection(char dirSymbol) {
        return Direction.fromSymbol(dirSymbol);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "x=" + x +
                ", y=" + y +
                ", type=" + type +
                ", organId=" + organId +
                '}';
    }
}
