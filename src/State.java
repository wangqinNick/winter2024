// State.java
import java.util.*;

/**
 * State 类用于存储和管理游戏状态及实体信息。
 */
public class State implements Cloneable {

    public int width;
    public int height;

    // 蛋白质库存
    public int myA, myB, myC, myD;
    public int oppA, oppB, oppC, oppD;

    // 实体列表
    public List<Entity> allEntities = new ArrayList<>();
    public List<Entity> myOrgans = new ArrayList<>();
    public List<Entity> oppOrgans = new ArrayList<>();
    public List<Entity> walls = new ArrayList<>();
    public List<Entity> proteins = new ArrayList<>(); // A, B, C, D

    // 采集器列表
    public List<Entity> ownHarvesters = new ArrayList<>();
    public List<Entity> enemyHarvesters = new ArrayList<>();

    /**
     * 构造函数，初始化地图宽度和高度。
     *
     * @param width  地图宽度
     * @param height 地图高度
     */
    public State(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 深拷贝构造函数。
     *
     * @param other 需要被拷贝的 State 对象
     */
    public State(State other) {
        this.width = other.width;
        this.height = other.height;
        this.myA = other.myA;
        this.myB = other.myB;
        this.myC = other.myC;
        this.myD = other.myD;
        this.oppA = other.oppA;
        this.oppB = other.oppB;
        this.oppC = other.oppC;
        this.oppD = other.oppD;

        // 深拷贝实体列表
        for (Entity e : other.allEntities) {
            Entity newEntity = e.clone();
            this.allEntities.add(newEntity);
            switch (newEntity.type) {
                case WALL:
                    this.walls.add(newEntity);
                    break;
                case ROOT:
                case BASIC:
                case HARVESTER:
                    if (newEntity.owner == Entity.Owner.SELF) {
                        this.myOrgans.add(newEntity);
                        if (newEntity.type == Entity.EntityType.HARVESTER) {
                            this.ownHarvesters.add(newEntity);
                        }
                    } else if (newEntity.owner == Entity.Owner.OPPONENT) {
                        this.oppOrgans.add(newEntity);
                        if (newEntity.type == Entity.EntityType.HARVESTER) {
                            this.enemyHarvesters.add(newEntity);
                        }
                    }
                    break;
                case A:
                case B:
                case C:
                case D:
                    this.proteins.add(newEntity);
                    break;
                default:
                    // 其他类型暂不处理
                    break;
            }
        }
    }

    /**
     * 添加实体到 State 中，并根据类型进行分类存储。
     *
     * @param x              X 坐标
     * @param y              Y 坐标
     * @param type           实体类型（WALL, ROOT, BASIC, HARVESTER, A, B, C, D）
     * @param ownerInt       所有者（1: 我方, 0: 对手, -1: 无归属）
     * @param organId        器官 ID（非器官则为0）
     * @param organDirSymbol 器官方向符号（'N', 'W', 'S', 'E' 或 'X'）
     * @param organParentId  父器官 ID（ROOT 类型器官为0）
     * @param organRootId    ROOT 器官 ID（非器官则为0）
     */
    public void addEntity(int x, int y, String type, int ownerInt, int organId,
                          char organDirSymbol, int organParentId, int organRootId) {
        Entity.Owner owner;
        if (ownerInt == 1) {
            owner = Entity.Owner.SELF;
        } else if (ownerInt == 0) {
            owner = Entity.Owner.OPPONENT;
        } else {
            owner = Entity.Owner.NONE;
        }

        Entity.EntityType entityType;
        try {
            entityType = Entity.EntityType.valueOf(type);
        } catch (IllegalArgumentException e) {
            // 未知类型，默认为 WALL
            entityType = Entity.EntityType.WALL;
        }

        // 解析方向符号为 Direction 枚举
        Direction direction = Entity.parseDirection(organDirSymbol);

        Entity e = new Entity(x, y, entityType, owner, organId, direction, organParentId, organRootId);
        allEntities.add(e);

        switch (e.type) {
            case WALL:
                walls.add(e);
                break;
            case ROOT:
            case BASIC:
            case HARVESTER:
                if (e.owner == Entity.Owner.SELF) {
                    myOrgans.add(e);
                    if (e.type == Entity.EntityType.HARVESTER) {
                        ownHarvesters.add(e);
                    }
                } else if (e.owner == Entity.Owner.OPPONENT) {
                    oppOrgans.add(e);
                    if (e.type == Entity.EntityType.HARVESTER) {
                        enemyHarvesters.add(e);
                    }
                }
                break;
            case A:
            case B:
            case C:
            case D:
                proteins.add(e);
                break;
            default:
                // 其他类型暂不处理
                break;
        }
    }

    // --- 判断方法 ---

    /**
     * 判断某坐标是否越界。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果越界则返回 true，否则返回 false
     */
    public boolean isOutOfBounds(int x, int y) {
        return x < 0 || x >= width || y < 0 || y >= height;
    }

    /**
     * 判断某坐标是否为墙。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果是墙则返回 true，否则返回 false
     */
    public boolean isWall(int x, int y) {
        for (Entity e : walls) {
            if (e.x == x && e.y == y) return true;
        }
        return false;
    }

    /**
     * 判断某坐标是否为我方器官。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果是我方器官则返回 true，否则返回 false
     */
    public boolean isMyOrgan(int x, int y) {
        for (Entity e : allEntities) {
            if (e.x == x && e.y == y) return true;
        }
        return false;
    }

    /**
     * 判断某坐标是否为Root。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果是Root则返回 true，否则返回 false
     */
    public boolean isRoot(int x, int y) {
        for (Entity e : myOrgans) {
            if (e.x == x && e.y == y && e.type == Entity.EntityType.ROOT) return true;
        }
        return false;
    }

    /**
     * 判断某坐标是否为对方器官。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果是对方器官则返回 true，否则返回 false
     */
    public boolean isOppOrgan(int x, int y) {
        for (Entity e : oppOrgans) {
            if (e.x == x && e.y == y) return true;
        }
        return false;
    }

    /**
     * 判断某坐标是否为蛋白质源（A/B/C/D）。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果是蛋白质源则返回 true，否则返回 false
     */
    public boolean isProteinTile(int x, int y) {
        for (Entity e : proteins) {
            if (e.x == x && e.y == y) return true;
        }
        return false;
    }

    /**
     * 判断某坐标是否为纯空地（非墙、非器官、非蛋白质）。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果是空地则返回 true，否则返回 false
     */
    public boolean isCellEmpty(int x, int y) {
        if (isOutOfBounds(x, y)) return false;
        if (isWall(x, y)) return false;
        if (isMyOrgan(x, y)) return false;
        if (isOppOrgan(x, y)) return false;
        if (isProteinTile(x, y)) return false;
        return true;
    }

    /**
     * 判断蛋白质位置上是否已经放置器官。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 如果已经放置器官，则返回 true，否则返回 false
     */
    public boolean isOrganPlaced(int x, int y) {
        for (Entity organ : myOrgans) {
            if (organ.x == x && organ.y == y) {
                return true;
            }
        }
        return false;
    }
}
