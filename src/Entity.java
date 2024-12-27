public class Entity implements Cloneable {

    public enum EntityType {
        WALL,
        ROOT,
        BASIC,
        HARVESTER,
        TENTACLE,
        SPORER,
        A, B, C, D
    }

    public enum Owner {
        SELF, OPPONENT, NONE
    }

    public int x, y;
    public EntityType type;
    public Owner owner;
    public int organId;         // 器官ID
    public Direction direction; // 朝向
    public int organParentId;   // 父器官ID
    public int organRootId;     // 该器官所属的 ROOT ID

    // 采集器容量 (根据需要，也可给 SPORER/TENTACLE 等不同属性)
    public int capacityC;
    public int capacityD;

    public Entity(int x, int y, EntityType type, Owner owner,
                  int organId, Direction direction,
                  int organParentId, int organRootId) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
        this.organId = organId;
        this.direction = direction;
        this.organParentId = organParentId;
        this.organRootId = organRootId;

        if (type == EntityType.HARVESTER) {
            this.capacityC = 1;
            this.capacityD = 1;
        } else {
            this.capacityC = 0;
            this.capacityD = 0;
        }
    }

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
        return "Entity{" + type + "@" + x + "," + y
                + ",owner=" + owner
                + ",id=" + organId
                + ",dir=" + direction
                + ",rootId=" + organRootId
                + "}";
    }
}
