import java.util.*;

public class Agent {

    /**
     * 在当前回合里，只能生长一次:
     * 1. 若我方蛋白质数 (myA) > requiredActionsCount(生长所需)，才可尝试生长
     * 2. 找到**离蛋白质源最近**的我方器官 (遍历所有器官 + BFS)
     * 3. 若找到了最近蛋白质源，则生成一次 GROW 命令；否则尝试备用策略(周围四格)或 WAIT
     */
    public String getAction(State state, int requiredActionsCount) {
        // 1) 如果蛋白质不足，则无法生长，只能 WAIT
        if (state.myA < requiredActionsCount) {
            System.err.println("蛋白质不足, 等待...");
            return "WAIT";
        }

        // 如果没有器官，就无从生长
        if (state.myOrgans.isEmpty()) {
            System.err.println("没有任何我方器官, 等待...");
            return "WAIT";
        }

        // 2) 遍历所有我方器官，找出到达蛋白质源距离最近者
        BFSResult bestResult = null;       // 用于记录最优 BFS 结果
        State.Entity bestOrgan = null;     // 记录“发起生长”的器官

        for (State.Entity organ : state.myOrgans) {
            BFSResult r = findNextStepTowardsClosestProteinForOrgan(state, organ.x, organ.y);
            if (r.found) {
                // 若目前还没有最佳，或找到更短的距离，则更新
                if (bestResult == null || r.dist < bestResult.dist) {
                    bestResult = r;
                    bestOrgan = organ;
                }
            }
        }

        // 3) 如果找到可到达的蛋白质源，则直接从 bestOrgan 朝 nextStep 生长
        if (bestResult != null && bestOrgan != null) {
            System.err.println(
                    String.format("最佳器官ID=%d 距离最近蛋白质=%d", bestOrgan.organId, bestResult.dist)
            );
            return String.format(
                    "GROW %d %d %d BASIC",
                    bestOrgan.organId,
                    bestResult.nextStep[0],
                    bestResult.nextStep[1]
            );
        }

        // 如果所有器官都无法到达蛋白质源
        System.err.println("没有器官能到达蛋白质源，尝试周围四格随机生长(遍历所有器官)...");

        // 准备一个数据结构来存储 “(器官ID, x, y)”
        List<int[]> possibleGrows = new ArrayList<>();

        // 遍历自己所有器官
        for (State.Entity organ : state.myOrgans) {
            int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            for (int[] dir : directions) {
                int nx = organ.x + dir[0];
                int ny = organ.y + dir[1];
                if (state.isOutOfBounds(nx, ny)) continue;
                if (isCellEmptyOrProtein(state, nx, ny)) {
                    // 记录: [器官ID, 目标x, 目标y]
                    possibleGrows.add(new int[]{ organ.organId, nx, ny });
                }
            }
        }

        // 如果在所有器官的相邻格子中，找到了至少一个可生长位置
        if (!possibleGrows.isEmpty()) {
            // 从这些可生长位置里随机挑一个
            Random rng = new Random();
            int[] chosen = possibleGrows.get(rng.nextInt(possibleGrows.size()));
            // chosen[0] = organId, chosen[1] = x, chosen[2] = y
            return String.format("GROW %d %d %d BASIC", chosen[0], chosen[1], chosen[2]);
        } else {
            // 若所有器官四周都无可生长位置，则只能 WAIT
            System.err.println("所有器官周围都无空位, 等待...");
            return "WAIT";
        }
    }

    //========================================================================
    //                   核心：为某个器官做BFS，找离它最近的蛋白质
    //========================================================================

    /**
     * 对一个给定器官坐标 (startX, startY)，BFS 找到最近的蛋白质源。
     * 如果成功找到，则返回 BFSResult，其中包含 "下一步坐标" 和 "距离"。
     * 如果找不到，则返回 BFSResult.found = false。
     */
    private BFSResult findNextStepTowardsClosestProteinForOrgan(State state, int startX, int startY) {
        int w = state.width;
        int h = state.height;
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
            int cx = cur[0];
            int cy = cur[1];

            // 如果这是蛋白质源(且不是起点本身), 找到了
            if (state.isProteinTile(cx, cy) && !(cx == startX && cy == startY)) {
                // 回溯路径
                List<int[]> path = reconstructPath(startX, startY, cx, cy, parentX, parentY);

                if (path.isEmpty()) {
                    // 如果路径为空(可能极小概率起点就是蛋白质,或无效情况)
                    return new BFSResult(false, null, Integer.MAX_VALUE);
                }
                // path.get(0) 就是"下一步"坐标
                int dist = path.size(); // BFS距离(起点到蛋白质源的步数)
                return new BFSResult(true, path.get(0), dist);
            }

            // 继续向4方向扩张
            int[][] directions = {{0,-1},{1,0},{0,1},{-1,0}};
            for (int[] d : directions) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;  // 越界
                if (visited[ny][nx]) continue;

                // 不可穿墙/器官，但可踩蛋白质
                if (!isTraversable(state, nx, ny)) continue;

                visited[ny][nx] = true;
                parentX[ny][nx] = cx;
                parentY[ny][nx] = cy;
                queue.offer(new int[]{ nx, ny });
            }
        }

        // 没搜索到任何蛋白质
        return new BFSResult(false, null, Integer.MAX_VALUE);
    }

    /**
     * 回溯出完整路径 (sx, sy) -> (gx, gy)，返回一个列表(不含起点，含终点)。
     * 若想包含起点可自行修改。
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
                // 说明没有父节点, 回溯失败
                return new ArrayList<>();
            }
            cx = px;
            cy = py;
        }
        // 此时(cx,cy) == (sx,sy)
        Collections.reverse(path);
        return path; // 不包含起点, 但包含终点
    }

    /**
     * BFS中可穿行(空地或蛋白质), 不可穿墙/己方器官/对方器官
     */
    private boolean isTraversable(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y))       return false;
        if (state.isMyOrgan(x, y))    return false;
        if (state.isOppOrgan(x, y))   return false;
        // 蛋白质源视为可穿(便于找到并回溯路径)
        return true;
    }

    /**
     * 是否可生长(空地或蛋白质源)
     */
    private boolean isCellEmptyOrProtein(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y))       return false;
        if (state.isMyOrgan(x, y))    return false;
        if (state.isOppOrgan(x, y))   return false;
        // 如果是蛋白质(A,B,C,D) 或纯空白
        return true;
    }

    //===============================================================
    //                  BFSResult 辅助类
    //===============================================================
    private static class BFSResult {
        boolean found;      // 是否找到蛋白质
        int[] nextStep;     // 下一步坐标(离起点最近的一格)
        int dist;           // BFS 距离(步数)

        BFSResult(boolean found, int[] nextStep, int dist) {
            this.found = found;
            this.nextStep = nextStep;
            this.dist = dist;
        }
    }
}
