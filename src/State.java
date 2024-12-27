import java.util.ArrayList;
import java.util.List;

/**
 * State 类：存储当前回合的状态信息（地图实体、蛋白质库存等）。
 */
public class State implements Cloneable {

    public int width;
    public int height;

    // 蛋白质库存（我方、对手）
    public int myA, myB, myC, myD;
    public int oppA, oppB, oppC, oppD;

    // 全部实体
    public List<Entity> allEntities = new ArrayList<>();

    // 我方器官
    public List<Entity> myOrgans = new ArrayList<>();
    // 对手器官
    public List<Entity> oppOrgans = new ArrayList<>();

    // 我方收集器、触手等的分类
    public List<Entity> ownHarvesters = new ArrayList<>();
    public List<Entity> ownTentacles = new ArrayList<>();

    // 对手收集器、触手
    public List<Entity> enemyHarvesters = new ArrayList<>();
    public List<Entity> enemyTentacles = new ArrayList<>();

    // 墙
    public List<Entity> walls = new ArrayList<>();

    // (此关无蛋白质源，但保留结构)
    public List<Entity> proteins = new ArrayList<>();

    /**
     * 构造函数：指定地图大小
     */
    public State(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 拷贝构造：深拷贝
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

        // 深拷贝实体
        for (Entity e : other.allEntities) {
            Entity cloned = e.clone();
            this.allEntities.add(cloned);

            switch (cloned.type) {
                case WALL:
                    walls.add(cloned);
                    break;
                case ROOT:
                case BASIC:
                case HARVESTER:
                case TENTACLE:
                    if (cloned.owner == Entity.Owner.SELF) {
                        myOrgans.add(cloned);
                        if (cloned.type == Entity.EntityType.HARVESTER) {
                            ownHarvesters.add(cloned);
                        } else if (cloned.type == Entity.EntityType.TENTACLE) {
                            ownTentacles.add(cloned);
                        }
                    } else if (cloned.owner == Entity.Owner.OPPONENT) {
                        oppOrgans.add(cloned);
                        if (cloned.type == Entity.EntityType.HARVESTER) {
                            enemyHarvesters.add(cloned);
                        } else if (cloned.type == Entity.EntityType.TENTACLE) {
                            enemyTentacles.add(cloned);
                        }
                    }
                    break;
                case A:
                case B:
                case C:
                case D:
                    proteins.add(cloned);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 添加实体到当前 State 中
     */
    public void addEntity(int x, int y, String typeStr, int ownerInt,
                          int organId, char organDir, int organParentId, int organRootId) {
        // 确定所有者
        Entity.Owner owner;
        if (ownerInt == 1) {
            owner = Entity.Owner.SELF;
        } else if (ownerInt == 0) {
            owner = Entity.Owner.OPPONENT;
        } else {
            owner = Entity.Owner.NONE;
        }

        // 实体类型
        Entity.EntityType eType;
        try {
            eType = Entity.EntityType.valueOf(typeStr); // 例如 "BASIC", "TENTACLE"
        } catch (Exception e) {
            eType = Entity.EntityType.WALL; // fallback
        }

        // 方向
        Direction direction = Direction.fromSymbol(organDir);

        // 构造实体
        Entity entity = new Entity(x, y, eType, owner, organId, direction, organParentId, organRootId);

        // 加入 allEntities
        allEntities.add(entity);

        // 根据类型分类
        switch (eType) {
            case WALL:
                walls.add(entity);
                break;
            case ROOT:
            case BASIC:
            case HARVESTER:
            case TENTACLE:
                if (owner == Entity.Owner.SELF) {
                    myOrgans.add(entity);
                    if (eType == Entity.EntityType.HARVESTER) {
                        ownHarvesters.add(entity);
                    } else if (eType == Entity.EntityType.TENTACLE) {
                        ownTentacles.add(entity);
                    }
                } else if (owner == Entity.Owner.OPPONENT) {
                    oppOrgans.add(entity);
                    if (eType == Entity.EntityType.HARVESTER) {
                        enemyHarvesters.add(entity);
                    } else if (eType == Entity.EntityType.TENTACLE) {
                        enemyTentacles.add(entity);
                    }
                }
                break;
            case A:
            case B:
            case C:
            case D:
                proteins.add(entity);
                break;
            default:
                break;
        }
    }

    public boolean isOutOfBounds(int x, int y) {
        return (x < 0 || x >= width || y < 0 || y >= height);
    }

    public boolean isWall(int x, int y) {
        for (Entity w : walls) {
            if (w.x == x && w.y == y) {
                return true;
            }
        }
        return false;
    }

    public boolean isMyOrgan(int x, int y) {
        for (Entity e : myOrgans) {
            if (e.x == x && e.y == y) return true;
        }
        return false;
    }

    public boolean isOppOrgan(int x, int y) {
        for (Entity e : oppOrgans) {
            if (e.x == x && e.y == y) return true;
        }
        return false;
    }

    public boolean isProteinTile(int x, int y) {
        for (Entity p : proteins) {
            if (p.x == x && p.y == y) return true;
        }
        return false;
    }

    /**
     * 如果要克隆，使用拷贝构造
     */
    @Override
    public State clone() {
        return new State(this);
    }
}
