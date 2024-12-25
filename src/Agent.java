import java.util.*;

public class Agent {

    /**
     * 在当前回合里，只能生长一次:
     * 1. 若我方蛋白质数 (myA) > requiredActionsCount(生长所需)，才可尝试生长
     * 2. 找到可作为父器官的一个器官 (此处选最后一个, 也可自行更改)
     * 3. 使用 BFS 找到最近的蛋白质源:
     *    - 如果找到, 获取从 parent 到蛋白质源的路径, 提取第2个节点作为"下一步"去 GROW
     * 4. 如果找不到, 则可以尝试原先的逻辑(周围四格)或者 WAIT
     * 5. 如果没有可行位置或蛋白质不足, 则输出 WAIT
     */
    public String getAction(State state, int requiredActionsCount) {
        // 1) 如果蛋白质不足，则无法生长，只能 WAIT
        if (state.myA <= requiredActionsCount) {
            System.err.println("蛋白质不足, 等待...");
            return "WAIT";
        }

        // 2) 选一个我方器官作为父器官
        if (state.myOrgans.isEmpty()) {
            System.err.println("没有任何我方器官, 等待...");
            return "WAIT"; // 没有器官无法生长
        }
        // 例如这里选择列表里的**最后一个**器官 (你也可以选第一个或ROOT)
        State.Entity parent = state.myOrgans.get(state.myOrgans.size() - 1);

        // 3) BFS 找最近蛋白质源, 返回 "下一步" 坐标
        int[] nextMove = findNextStepTowardsClosestProtein(state, parent.x, parent.y);
        if (nextMove != null) {
            // 找到蛋白质源的方向，直接 GROW
            int nx = nextMove[0];
            int ny = nextMove[1];
            return String.format("GROW %d %d %d BASIC", parent.organId, nx, ny);
        } else {
            System.err.println("附近没有可达的蛋白质源，尝试在周围四格找空位...");

            // 如果找不到蛋白质源，就使用之前的简单四格生长逻辑或 WAIT
            // 这里演示“在周围四格找一个空位或蛋白质源”的做法
            List<int[]> candidatePositions = new ArrayList<>();
            int[][] directions = {{ 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 }};
            for (int[] dir : directions) {
                int nx = parent.x + dir[0];
                int ny = parent.y + dir[1];
                if (state.isOutOfBounds(nx, ny)) continue;
                if (isCellEmptyOrProtein(state, nx, ny)) {
                    candidatePositions.add(new int[]{ nx, ny });
                }
            }
            if (!candidatePositions.isEmpty()) {
                Random rng = new Random();
                int[] chosen = candidatePositions.get(rng.nextInt(candidatePositions.size()));
                return String.format("GROW %d %d %d BASIC", parent.organId, chosen[0], chosen[1]);
            } else {
                System.err.println("周围也无空位, 等待...");
                return "WAIT";
            }
        }
    }

    /**
     * BFS 查找从 (startX, startY) 到最近蛋白质源的路径，并返回下一步坐标。
     * 如果找不到任何蛋白质源，返回 null。
     */
    private int[] findNextStepTowardsClosestProtein(State state, int startX, int startY) {
        // 如果 (startX, startY) 本身在蛋白质上(极端情况)，那下一步就相等?
        // 这里暂不考虑这种情况，一般器官不会在蛋白质源格上。

        // BFS 初始化
        int w = state.width;
        int h = state.height;
        boolean[][] visited = new boolean[h][w];
        // 存储每个坐标的前驱，用于路径回溯
        int[][] parentX = new int[h][w];
        int[][] parentY = new int[h][w];
        for (int i = 0; i < h; i++) {
            Arrays.fill(parentX[i], -1);
            Arrays.fill(parentY[i], -1);
        }

        Queue<int[]> queue = new ArrayDeque<>();
        queue.offer(new int[]{ startX, startY });
        visited[startY][startX] = true;

        // BFS
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0];
            int cy = cur[1];

            // 如果这里是蛋白质源，就可以结束搜索 --
            // 但实际上，一开始 (cx,cy) 是器官所在处，通常不会是蛋白质源。
            // 我们需要找下一个遇到蛋白质的坐标
            if (state.isProteinTile(cx, cy) && !(cx == startX && cy == startY)) {
                // 找到了蛋白质源，从 (cx, cy) 回溯路径
                return reconstructNextStep(startX, startY, cx, cy, parentX, parentY);
            }

            // 扩展四个方向
            int[][] directions = {{0,-1},{1,0},{0,1},{-1,0}};
            for (int[] d : directions) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue; // 越界
                if (visited[ny][nx]) continue;

                // BFS 要允许走到蛋白质源上(以便搜索到它)
                // 但不能穿墙、不能穿自己的器官、也不能穿对手器官
                if (!isTraversable(state, nx, ny)) continue;

                visited[ny][nx] = true;
                parentX[ny][nx] = cx;
                parentY[ny][nx] = cy;
                queue.offer(new int[]{ nx, ny });
            }
        }

        // 没找到蛋白质源
        return null;
    }

    /**
     * 回溯路径，并返回“从起点出发的第一步”坐标
     * @param sx 起点X
     * @param sy 起点Y
     * @param gx 目标(发现蛋白质)X
     * @param gy 目标(发现蛋白质)Y
     */
    private int[] reconstructNextStep(int sx, int sy,
                                      int gx, int gy,
                                      int[][] parentX, int[][] parentY) {
        // 回溯路径 (gx, gy) -> (sx, sy)
        List<int[]> path = new ArrayList<>();
        int cx = gx, cy = gy;
        while (!(cx == sx && cy == sy)) {
            path.add(new int[]{ cx, cy });
            int px = parentX[cy][cx];
            int py = parentY[cy][cx];
            cx = px;
            cy = py;
        }
        // 此时 (cx,cy) == (sx, sy)
        // path 中存的是从目标往回走的坐标，不含起点，但含目标
        Collections.reverse(path); // 反转为从起点 -> 目标

        // 如果 path 为空(其实不会空, 因为找到就至少目标自己),
        // 或者 path.size() == 1 (目标就在起点隔壁?),
        // 则第一步就是 path.get(0)
        if (path.isEmpty()) {
            return null;
        }
        // path.get(0) 就是离起点最近的一步(其实就是目标?), 但通常 BFS 找到的点可能就是下一个格子
        // 例如, 当蛋白质就在相邻格时, path.size()=1, path.get(0)=那格 => nextStep=那格
        return path.get(0);
    }

    /**
     * BFS中, 判断 (x,y) 是否可通行:
     * 不可越界, 不可为墙/器官; 可以是蛋白质源(A,B,C,D)或纯空地
     */
    private boolean isTraversable(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y)) return false;
        if (state.isMyOrgan(x, y)) return false;
        if (state.isOppOrgan(x, y)) return false;
        // 蛋白质源也允许通行, 这样才能搜索到它
        return true;
    }

    /**
     * 是否空地或蛋白质源 (仅用于本回合生长判断, 不一定跟 BFS 中的 isTraversable 一致)
     */
    private boolean isCellEmptyOrProtein(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        // 不能是墙或器官
        if (state.isWall(x, y)) return false;
        if (state.isMyOrgan(x, y)) return false;
        if (state.isOppOrgan(x, y)) return false;
        // 如果是蛋白质(A,B,C,D) 或什么都没有(空格)
        return true;
    }
}
