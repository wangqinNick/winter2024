import java.util.*;

public class State {

    public int width;
    public int height;

    // 我方蛋白质库存
    public int myA, myB, myC, myD;
    // 对手蛋白质
    public int oppA, oppB, oppC, oppD;

    public List<Entity> allEntities = new ArrayList<>();
    public List<Entity> myOrgans = new ArrayList<>();
    public List<Entity> oppOrgans = new ArrayList<>();
    public List<Entity> walls = new ArrayList<>();
    public List<Entity> proteins = new ArrayList<>();

    public State(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void addEntity(int x, int y, String type, int owner, int organId,
                          String organDir, int organParentId, int organRootId) {
        Entity e = new Entity(x, y, type, owner, organId, organDir, organParentId, organRootId);
        allEntities.add(e);

        switch (type) {
            case "WALL":
                walls.add(e);
                break;
            case "ROOT":
            case "BASIC":
                if (owner == 1) {
                    myOrgans.add(e);
                } else if (owner == 0) {
                    oppOrgans.add(e);
                }
                break;
            case "A":
            case "B":
            case "C":
            case "D":
                proteins.add(e);
                break;
            default:
                break;
        }
    }

    public boolean isOutOfBounds(int x, int y) {
        return x < 0 || x >= width || y < 0 || y >= height;
    }

    public boolean isWall(int x, int y) {
        for (Entity e : walls) {
            if (e.x == x && e.y == y) return true;
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

    // 实体类
    public static class Entity {
        public int x, y;
        public String type;      // WALL, ROOT, BASIC, A, B, C, D ...
        public int owner;        // 1: 我方, 0: 对手, -1: 无归属
        public int organId;
        public String organDir;
        public int organParentId;
        public int organRootId;

        public Entity(int x, int y, String type, int owner, int organId,
                      String organDir, int organParentId, int organRootId) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.owner = owner;
            this.organId = organId;
            this.organDir = organDir;
            this.organParentId = organParentId;
            this.organRootId = organRootId;
        }
    }

    public boolean isProteinTile(int x, int y) {
        // 检查所有记录在 State.proteins 列表中的蛋白质源
        for (Entity e : proteins) {
            if (e.x == x && e.y == y) {
                return true;
            }
        }
        return false;
    }
}
