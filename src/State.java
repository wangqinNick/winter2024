import java.util.ArrayList;
import java.util.List;

public class State implements Cloneable {
    public int width, height;

    public int myA, myB, myC, myD;
    public int oppA, oppB, oppC, oppD;

    public List<Entity> allEntities = new ArrayList<>();
    public List<Entity> myOrgans = new ArrayList<>();
    public List<Entity> oppOrgans = new ArrayList<>();
    public List<Entity> walls = new ArrayList<>();
    public List<Entity> proteins = new ArrayList<>();

    // 你也可维护单独的收集器、触手、sporer列表，但这里先不展开
    public State(int width, int height) {
        this.width = width;
        this.height = height;
    }

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

        for (Entity e : other.allEntities) {
            Entity cloned = e.clone();
            allEntities.add(cloned);

            switch (cloned.type) {
                case WALL:
                    walls.add(cloned); break;
                case A: case B: case C: case D:
                    proteins.add(cloned); break;
                default:
                    if (cloned.owner == Entity.Owner.SELF) {
                        myOrgans.add(cloned);
                    } else if (cloned.owner == Entity.Owner.OPPONENT) {
                        oppOrgans.add(cloned);
                    }
                    break;
            }
        }
    }

    @Override
    public State clone() {
        return new State(this);
    }

    public void addEntity(int x, int y, String typeStr, int ownerInt,
                          int organId, char dirChar, int parentId, int rootId) {

        // 解析 owner
        Entity.Owner owner;
        if (ownerInt == 1) owner = Entity.Owner.SELF;
        else if (ownerInt == 0) owner = Entity.Owner.OPPONENT;
        else owner = Entity.Owner.NONE;

        // 解析类型
        Entity.EntityType eType;
        try {
            eType = Entity.EntityType.valueOf(typeStr); // BASIC, SPORER, TENTACLE etc.
        } catch (Exception e) {
            eType = Entity.EntityType.WALL;
        }

        // 解析方向
        Direction d = Direction.fromSymbol(dirChar);

        // 构造 Entity
        Entity ent = new Entity(x, y, eType, owner, organId, d, parentId, rootId);
        allEntities.add(ent);

        switch (eType) {
            case WALL:
                walls.add(ent);
                break;
            case A: case B: case C: case D:
                proteins.add(ent);
                break;
            default:
                // organ
                if (owner == Entity.Owner.SELF) {
                    myOrgans.add(ent);
                } else if (owner == Entity.Owner.OPPONENT) {
                    oppOrgans.add(ent);
                }
                break;
        }
    }

    public boolean isOutOfBounds(int x, int y) {
        return (x < 0 || x >= width || y < 0 || y >= height);
    }
    public boolean isWall(int x, int y) {
        for (Entity w : walls) {
            if (w.x == x && w.y == y) return true;
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
}
