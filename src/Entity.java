/**
 * Entity 类：表示地图上的一个实体。
 */
public class Entity implements Cloneable {

    public enum EntityType {
        WALL,
        ROOT,
        BASIC,
        HARVESTER,
        TENTACLE,
        A, B, C, D // 虽然本关用不到，但保留
    }

    public enum Owner {
        SELF, OPPONENT, NONE
    }

    public int x, y;
    public EntityType type;
    public Owner owner;
    public int organId;
    public Direction direction;
    public int organParentId;
    public int organRootId;

    // 采集器容量或其他属性(可选)
    public int capacityC;
    public int capacityD;

    /**
     * 构造
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

        // 若是HARVESTER默认容量
        if (type == EntityType.HARVESTER) {
            this.capacityC = 1;
            this.capacityD = 1;
        } else {
            this.capacityC = 0;
            this.capacityD = 0;
        }
    }

    /**
     * 拷贝构造
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

    @Override
    public Entity clone() {
        return new Entity(this);
    }

    @Override
    public String toString() {
        return "Entity{x=" + x + ",y=" + y + ",type=" + type + ",owner=" + owner
                + ",id=" + organId + ",dir=" + direction + "}";
    }

    public static Direction parseDirection(char c) {
        return Direction.fromSymbol(c);
    }
}
