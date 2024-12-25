import java.util.*;

/**
 * State 类用于存储和管理游戏状态及实体信息。
 */
public class State {

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
    public List<Entity> harvesters = new ArrayList<>(); // HARVESTER

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
     * 添加实体到 State 中，并根据类型进行分类存储。
     *
     * @param x              X 坐标
     * @param y              Y 坐标
     * @param type           实体类型（WALL, ROOT, BASIC, HARVESTER, A, B, C, D）
     * @param owner          所有者（1: 我方, 0: 对手, -1: 无归属）
     * @param organId        器官 ID（非器官则为0）
     * @param organDir       器官方向（N, W, S, E 或 X）
     * @param organParentId  父器官 ID（ROOT 类型器官为0）
     * @param organRootId    ROOT 器官 ID（非器官则为0）
     */
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
            case "HARVESTER":
                if (owner == 1) {
                    myOrgans.add(e);
                    if ("HARVESTER".equals(type)) {
                        harvesters.add(e);
                    }
                } else if (owner == 0) {
                    oppOrgans.add(e);
                    if ("HARVESTER".equals(type)) {
                        harvesters.add(e);
                    }
                }
                break;
            case "A":
            case "B":
            case "C":
            case "D":
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
        for (Entity e : myOrgans) {
            if (e.x == x && e.y == y) return true;
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
     * 实体类，表示地图上的各种实体。
     */
    public static class Entity {
        public int x, y;
        public String type;      // WALL, ROOT, BASIC, HARVESTER, A, B, C, D
        public int owner;        // 1: 我方, 0: 对手, -1: 无归属
        public int organId;      // 器官ID (非器官则为0)
        public String organDir;  // N, W, S, E 或 X
        public int organParentId;
        public int organRootId;

        /**
         * 构造函数，初始化实体属性。
         *
         * @param x              X 坐标
         * @param y              Y 坐标
         * @param type           实体类型
         * @param owner          所有者
         * @param organId        器官 ID
         * @param organDir       器官方向
         * @param organParentId  父器官 ID
         * @param organRootId    ROOT 器官 ID
         */
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
}
