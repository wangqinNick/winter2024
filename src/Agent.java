import java.util.*;

/**
 * Agent 类负责决定每回合的行动策略。
 */
public class Agent {

    private Random rng = new Random(); // 统一的随机实例，避免每次调用时创建新对象

    /**
     * 获取当前回合的行动。
     *
     * @param state                当前游戏状态
     * @param requiredActionsCount 本回合需要执行的动作数量（通常为1）
     * @return Action 对象，代表本回合的行动
     */
    public Action getAction(State state, int requiredActionsCount) {

        // 获取所有可能的动作
        List<Action> possibleActions = getAllPossibleActions(state);

        if (possibleActions.isEmpty()) {
            // 如果没有可执行的动作，则等待
            return new Action(); // WAIT
        }

        // 随机选择一个动作
        Action selectedAction = possibleActions.get(rng.nextInt(possibleActions.size()));
        System.err.printf("选择动作: %s%n", selectedAction);
        return selectedAction;
    }

    /**
     * 获取当前状态下所有可能的动作。
     *
     * @param state 当前游戏状态
     * @return 包含所有可能动作的列表
     */
    private List<Action> getAllPossibleActions(State state) {
        List<Action> actions = new ArrayList<>();

        // 定义四个方向及其对应的偏移量
        int[][] directions = { {0, -1}, {1, 0}, {0, 1}, {-1, 0} }; // N, E, S, W
        char[] dirChars = { 'N', 'E', 'S', 'W' };

        for (State.Entity organ : state.myOrgans) {
            for (int i = 0; i < directions.length; i++) {
                int dx = directions[i][0];
                int dy = directions[i][1];
                char dirChar = dirChars[i];

                int targetX = organ.x + dx;
                int targetY = organ.y + dy;

                // 检查目标位置是否可放置器官
                if (!canGrowOrgan(state, targetX, targetY)) {
                    continue;
                }

                // 1. 尝试生长 BASIC
                if (state.myA >= 1) { // 假设生长 BASIC 需要消耗 1 个 A 类型蛋白质
                    actions.add(new Action(organ.organId, targetX, targetY, Action.ActionType.BASIC, dirChar));
                }

                // 2. 尝试生长 HARVESTER
                if (state.myC >= 1 && state.myD >= 1) { // 生长 HARVESTER 需要 1 个 C 和 1 个 D
                    // HARVESTER 必须面向蛋白质源
                    int harvestX = targetX + dx;
                    int harvestY = targetY + dy;
                    if (!state.isOutOfBounds(harvestX, harvestY) && state.isProteinTile(harvestX, harvestY)) {
                        actions.add(new Action(organ.organId, targetX, targetY, Action.ActionType.HARVESTER, dirChar));
                    }
                }
            }
        }

        // 添加 WAIT 动作
        actions.add(new Action()); // WAIT

        return actions;
    }

    /**
     * 判断是否可以在 (x, y) 生长一个器官。
     *
     * @param state 当前游戏状态
     * @param x     目标 X 坐标
     * @param y     目标 Y 坐标
     * @return 如果可以生长则返回 true，否则返回 false
     */
    private boolean canGrowOrgan(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y)) return false;
        if (state.isMyOrgan(x, y)) return false;
        if (state.isOppOrgan(x, y)) return false;
        // 可以生长在蛋白质源或空地
        return true;
    }

    /**
     * 尝试建造 HARVESTER。如果成功，返回 HARVESTER 的 Action；否则返回 null。
     *
     * @param state 当前游戏状态
     * @return HARVESTER 的 Action 对象或 null
     */
    private Action tryBuildHarvester(State state) {
        // 需要至少1 C 和1 D 蛋白质
        if (state.myC < 1 || state.myD < 1) {
            return null; // 不满足建造条件
        }

        // 遍历所有我方器官，寻找可建造 HARVESTER 的位置
        int[][] neighbors = { {0,-1}, {1,0}, {0,1}, {-1,0} };
        char[] dirMap = { 'N', 'E', 'S', 'W' }; // 对应方向

        for (State.Entity organ : state.myOrgans) {
            for (int i = 0; i < 4; i++) {
                int nx = organ.x + neighbors[i][0];
                int ny = organ.y + neighbors[i][1];
                // 判断 (nx, ny) 是否可放 HARVESTER
                if (!canPlaceHarvester(state, nx, ny)) continue;

                // HARVESTER 面向的坐标
                int fx = nx + neighbors[i][0];
                int fy = ny + neighbors[i][1];
                if (state.isOutOfBounds(fx, fy)) continue;

                // 检查 (fx, fy) 是否为蛋白质源
                if (state.isProteinTile(fx, fy)) {
                    char dir = dirMap[i]; // HARVESTER 的方向
                    System.err.println(String.format("建造 HARVESTER, 面向 %c, 位置 (%d, %d)", dir, nx, ny));
                    return new Action(organ.organId, nx, ny, Action.ActionType.HARVESTER, dir);
                }
            }
        }

        // 如果没有找到合适位置，则不建造 HARVESTER
        return null;
    }

    /**
     * 判断是否可以在 (x, y) 放置 HARVESTER。
     *
     * @param state 当前游戏状态
     * @param x     X 坐标
     * @param y     Y 坐标
     * @return 如果可以放置则返回 true，否则返回 false
     */
    private boolean canPlaceHarvester(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y)) return false;
        if (state.isMyOrgan(x, y)) return false;
        if (state.isOppOrgan(x, y)) return false;
        if (state.isProteinTile(x, y)) return false; // 不允许踩蛋白质源
        return true;
    }

    /**
     * 使用 BFS 找到从 (startX, startY) 出发最近的蛋白质源的下一步坐标。
     *
     * @param state  当前游戏状态
     * @param startX 起始 X 坐标
     * @param startY 起始 Y 坐标
     * @return BFSResult 对象，包含是否找到、下一步坐标及距离
     */
    private BFSResult findNextStepTowardsClosestProteinForOrgan(State state, int startX, int startY) {
        int w = state.width, h = state.height;
        boolean[][] visited = new boolean[h][w];
        int[][] parentX = new int[h][w];
        int[][] parentY = new int[h][w];

        for (int i = 0; i < h; i++) {
            Arrays.fill(parentX[i], -1);
            Arrays.fill(parentY[i], -1);
        }

        Queue<int[]> queue = new ArrayDeque<>();
        queue.offer(new int[]{ startX, startY });
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1];
            // 找到蛋白质源且不是起点
            if (state.isProteinTile(cx, cy) && !(cx == startX && cy == startY)) {
                List<int[]> path = reconstructPath(startX, startY, cx, cy, parentX, parentY);
                if (!path.isEmpty()) {
                    return new BFSResult(true, path.get(0), path.size());
                }
            }
            // 扩展四个方向
            int[][] directions = { {0,-1}, {1,0}, {0,1}, {-1,0} };
            for (int[] d : directions) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue; // 越界
                if (visited[ny][nx]) continue;
                if (!isTraversableForBFS(state, nx, ny)) continue;
                visited[ny][nx] = true;
                parentX[ny][nx] = cx;
                parentY[ny][nx] = cy;
                queue.offer(new int[]{ nx, ny });
            }
        }

        // 没找到蛋白质源
        return new BFSResult(false, null, Integer.MAX_VALUE);
    }

    /**
     * 回溯路径，从 (sx, sy) 到 (gx, gy)。
     *
     * @param sx      起始 X 坐标
     * @param sy      起始 Y 坐标
     * @param gx      目标 X 坐标
     * @param gy      目标 Y 坐标
     * @param parentX 父节点 X 数组
     * @param parentY 父节点 Y 数组
     * @return 路径列表（不包含起点，包含终点）
     */
    private List<int[]> reconstructPath(int sx, int sy, int gx, int gy,
                                        int[][] parentX, int[][] parentY) {
        List<int[]> path = new ArrayList<>();
        int cx = gx, cy = gy;
        while (!(cx == sx && cy == sy)) {
            path.add(new int[]{ cx, cy });
            int px = parentX[cy][cx];
            int py = parentY[cy][cx];
            if (px == -1 && py == -1) {
                // 回溯失败
                return new ArrayList<>();
            }
            cx = px;
            cy = py;
        }
        Collections.reverse(path);
        return path; // 不包含起点, 含终点
    }

    /**
     * 判断某坐标在 BFS 中是否可通行（可走蛋白质源）。
     *
     * @param state 当前游戏状态
     * @param x     X 坐标
     * @param y     Y 坐标
     * @return 如果可通行则返回 true，否则返回 false
     */
    private boolean isTraversableForBFS(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y)) return false;
        if (state.isMyOrgan(x, y)) return false;
        if (state.isOppOrgan(x, y)) return false;
        // 可以走蛋白质源
        return true;
    }

    /**
     * 判断某坐标是否为空地或蛋白质源。
     *
     * @param state 当前游戏状态
     * @param x     X 坐标
     * @param y     Y 坐标
     * @return 如果是空地或蛋白质源则返回 true，否则返回 false
     */
    private boolean isCellEmptyOrProtein(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y)) return false;
        if (state.isMyOrgan(x, y)) return false;
        if (state.isOppOrgan(x, y)) return false;
        // 蛋白质源或空地
        return true;
    }

    /**
     * 随机返回 'N','E','S','W' 之一。
     *
     * @return 随机方向字符
     */
    private char getRandomDirection() {
        char[] dirs = { 'N', 'E', 'S', 'W' };
        return dirs[rng.nextInt(dirs.length)];
    }

    /**
     * BFSResult 类用于存储 BFS 的结果。
     */
    private static class BFSResult {
        boolean found;      // 是否找到蛋白质源
        int[] nextStep;     // 下一步坐标 (x, y)
        int dist;           // 距离

        /**
         * 构造函数。
         *
         * @param found    是否找到蛋白质源
         * @param nextStep 下一步坐标
         * @param dist     距离
         */
        public BFSResult(boolean found, int[] nextStep, int dist) {
            this.found = found;
            this.nextStep = nextStep;
            this.dist = dist;
        }
    }
}
