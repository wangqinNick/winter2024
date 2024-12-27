// Agent.java
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

        // 初始化最佳动作和最高得分
        Action bestAction = null;
        int bestScore = Integer.MIN_VALUE;

        for (Action action : possibleActions) {

            // 模拟执行动作后的新状态
            State simulatedState = simulate(state, action);

            // 评估模拟状态的得分
            int score = evaluateState(simulatedState);

            System.err.println("当前模拟动作为: " + action + "得分为: " + score);

            // 更新最佳动作
            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        // 如果找到了最佳动作，则返回
        if (bestAction != null) {
            System.err.println(String.format("选择动作: %s, 得分: %d", bestAction, bestScore));
            return bestAction;
        }

        // 否则，随机选择一个动作
        Action selectedAction = possibleActions.get(rng.nextInt(possibleActions.size()));
        System.err.println(String.format("选择随机动作: %s", selectedAction));
        return selectedAction;
    }

    /**
     * 评估给定状态的得分。
     * 1) 采集器朝向蛋白质的数量
     * 2) 采集器的数量
     * 3) 采集器的资源容量
     * 4) 采集器距离蛋白质的远近
     * 5) 器官的布局
     * 6) **新增**：器官数量
     *
     * @param state 要评估的状态
     * @return 评分分数
     */
    public int evaluateState(State state) {
        // 定义权重
        final double wHarvesterFacing = 50.0;    // 每个朝向蛋白质的采集器得分
        final double wHarvesterCount = 0;     // 每个采集器得分
        final double wHarvesterCapacity = 0;  // 每个具备资源的采集器得分
        final double wHarvesterDistance = 0;   // 每个采集器与蛋白质的距离得分
        final double wOrganOnProtein = -5.0;     // 每个蛋白质上有器官得分
        final double wTotalOrgans = 15.0;         // **新增**：每个器官的得分

        double totalScore = 0.0;

        // 1. 统计采集器朝向蛋白质的数量
        int harvestersFacing = 0;
        for (Entity harvester : state.ownHarvesters) {
            // 获取采集器的朝向偏移
            int[] offset = directionToOffset(harvester.direction);
            int targetX = harvester.x + offset[0];
            int targetY = harvester.y + offset[1];

            // 检查面对方向上是否有蛋白质
            if (!state.isOutOfBounds(targetX, targetY) && state.isProteinTile(targetX, targetY)) {
                harvestersFacing += 1;
//                System.err.println("采集器在 (" + targetX + ", " + targetY + ") 面向蛋白质");
            } else {
//                System.err.println("采集器在 (" + targetX + ", " + targetY + ") 没有面向蛋白质");
            }
        }
        totalScore += wHarvesterFacing * harvestersFacing;

        // 2. 统计采集器的数量
        int harvesterCount = state.ownHarvesters.size();
        totalScore += wHarvesterCount * harvesterCount;

        // 3. 统计具备资源容量的采集器数量
        int harvestersWithCapacity = 0;
        for (Entity harvester : state.ownHarvesters) {
            if (harvester.capacityC >= 1 && harvester.capacityD >= 1) {
                harvestersWithCapacity += 1;
            }
        }
        totalScore += wHarvesterCapacity * harvestersWithCapacity;

        // 4. 计算采集器与最近蛋白质的距离
        for (Entity harvester : state.ownHarvesters) {
            int minDistance = Integer.MAX_VALUE;
            for (Entity protein : state.proteins) {
                int distance = Math.abs(harvester.x - protein.x) + Math.abs(harvester.y - protein.y);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
            if (minDistance != Integer.MAX_VALUE) {
                totalScore -= wHarvesterDistance * minDistance; // 距离越近，分数越高
            }
        }

        // 5. 统计蛋白质上有器官的数量（已采集的蛋白质已被移除，不需额外检查）
        int organsOnProtein = 0;
        for (Entity protein : state.proteins) {
            for (Entity organ : state.myOrgans) {
                if (organ.x == protein.x && organ.y == protein.y) {
                    organsOnProtein += 1;
                    break;
                }
            }
        }
        totalScore += wOrganOnProtein * organsOnProtein;

        // 6. **新增**：根据器官数量增加总分
        int totalOrgans = state.myOrgans.size();
        totalScore += wTotalOrgans * totalOrgans;
        return (int) totalScore;
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


        for (Entity myOrgan : state.myOrgans) {
            for (int i = 0; i < directions.length; i++) {
                int dx = directions[i][0];
                int dy = directions[i][1];
                char dirChar = dirChars[i];

                int targetX = myOrgan.x + dx;
                int targetY = myOrgan.y + dy;

                // 检查目标位置是否可放置采集器
                if (!canGrowOrgan(state, targetX, targetY)) continue;

                // 1. 放置Harvester
                if (state.myC >= 1 && state.myD >= 1) { // 具备资源
                    actions.add(new Action(organIdForHarvester(state), targetX, targetY, Action.ActionType.HARVESTER, oppositeDirection(dirChar)));
                }

                // 2. 放置器官
                if (state.myA >= 1) {
                    actions.add(new Action(organIdForBasic(state), targetX, targetY, Action.ActionType.BASIC, 'X'));
                }
            }
        }


        // 3. 添加 WAIT 动作
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
     * 模拟执行一个动作后的新状态。
     *
     * @param state  当前游戏状态
     * @param action 要执行的动作
     * @return 执行动作后的新状态
     */
    public State simulate(State state, Action action) {
        // 深拷贝当前状态
        State newState = new State(state);

        switch (action.getActionType()) {
            case WAIT:
                // WAIT 动作不改变状态
//                System.err.println("模拟动作: WAIT");
                break;

            case BASIC:
                // 生长 BASIC
                growBasic(newState, action);
                break;

            case HARVESTER:
                // 生长 HARVESTER
                growHarvester(newState, action);
                break;

            default:
                // 未知动作类型，视为 WAIT
//                System.err.println("模拟动作: 未知动作类型，执行 WAIT");
                break;
        }

        return newState;
    }

    /**
     * 生长 BASIC 动作。
     *
     * @param state  新状态对象
     * @param action 要执行的 BASIC 动作
     */
    private void growBasic(State state, Action action) {
        // 消耗 1 个 A 类型蛋白质
        state.myA -= 1;

        // 创建新的 BASIC 器官实体
        Entity newBasic = new Entity(
                action.getX(),
                action.getY(),
                Entity.EntityType.BASIC,
                Entity.Owner.SELF,
                generateNewOrganId(state), // 生成新的器官 ID
                Direction.NONE, // BASIC 不需要方向
                action.getOrganId(), // 父器官 ID
                0 // ROOT ID 不适用
        );

        // 添加到状态中
        state.allEntities.add(newBasic);
        state.myOrgans.add(newBasic);
    }

    /**
     * 生长 HARVESTER 动作。
     *
     * @param state  新状态对象
     * @param action 要执行的 HARVESTER 动作
     */
    private void growHarvester(State state, Action action) {
        // 消耗 1 个 C 和 1 个 D 类型蛋白质
        state.myC -= 1;
        state.myD -= 1;

        // 创建新的 HARVESTER 器官实体
        Direction harvesterDir = Entity.parseDirection(action.getDirection());
        Entity newHarvester = new Entity(
                action.getX(),
                action.getY(),
                Entity.EntityType.HARVESTER,
                Entity.Owner.SELF,
                generateNewOrganId(state), // 生成新的器官 ID
                harvesterDir, // HARVESTER 的方向
                action.getOrganId(), // 父器官 ID
                0 // ROOT ID 不适用
        );

        // 添加到状态中
        state.allEntities.add(newHarvester);
        state.myOrgans.add(newHarvester);
        state.ownHarvesters.add(newHarvester);

        // HARVESTER 会采集面向方向的蛋白质
        int harvestX = action.getX() + directionToOffset(harvesterDir)[0];
        int harvestY = action.getY() + directionToOffset(harvesterDir)[1];

        if (!state.isOutOfBounds(harvestX, harvestY) && state.isProteinTile(harvestX, harvestY)) {
            // 获取蛋白质类型
            String proteinType = getProteinTypeAt(state, harvestX, harvestY);
            if (proteinType != null) {
                harvestProtein(state, proteinType);
                // 更新采集器的资源容量
                for (Entity harv : state.ownHarvesters) {
                    if (harv.x == action.getX() && harv.y == action.getY()) {
                        harv.capacityC += 1; // 根据采集到的蛋白质类型调整
                        harv.capacityD += 1;
                        break;
                    }
                }
//                System.err.println(String.format("HARVESTER 在 (%d, %d) 采集到蛋白质: %s", harvestX, harvestY, proteinType));
            }
        }
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

    /**
     * 获取 HARVESTER 的器官 ID，用于生成 HARVESTER 动作。
     * 如果没有可用的器官 ID，返回 ROOT 的 ID 或其他逻辑。
     *
     * @param state 当前游戏状态
     * @return HARVESTER 的器官 ID
     */
    private int organIdForHarvester(State state) {
        // 优先使用 ROOT 的器官 ID 作为父器官 ID
        for (Entity organ : state.myOrgans) {
            if (organ.type == Entity.EntityType.ROOT) {
                return organ.organId;
            }
        }
        // 如果没有 ROOT，使用任意一个器官的 ID
        if (!state.myOrgans.isEmpty()) {
            return state.myOrgans.get(0).organId;
        }
        // 默认返回 0，如果需要其他逻辑，请根据游戏规则调整
        return 0;
    }

    /**
     * 获取 BASIC 的器官 ID，用于生成 BASIC 动作。
     * 可以使用 ROOT 或其他逻辑作为父器官 ID。
     *
     * @param state 当前游戏状态
     * @return BASIC 的器官 ID
     */
    private int organIdForBasic(State state) {
        // 优先使用 ROOT 的器官 ID 作为父器官 ID
        for (Entity organ : state.myOrgans) {
            if (organ.type == Entity.EntityType.ROOT) {
                return organ.organId;
            }
        }
        // 如果没有 ROOT，使用任意一个器官的 ID
        if (!state.myOrgans.isEmpty()) {
            return state.myOrgans.get(0).organId;
        }
        // 默认返回 0，如果需要其他逻辑，请根据游戏规则调整
        return 0;
    }

    /**
     * 获取与当前方向相反的方向符号，用于 HARVESTER 朝向蛋白质
     *
     * @param dirChar 当前方向符号（'N', 'E', 'S', 'W'）
     * @return 相反方向的符号
     */
    private char oppositeDirection(char dirChar) {
        switch (dirChar) {
            case 'N':
                return 'S';
            case 'E':
                return 'W';
            case 'S':
                return 'N';
            case 'W':
                return 'E';
            default:
                return 'X';
        }
    }

    /**
     * 根据坐标偏移获取方向字符。
     *
     * @param dirX X 方向偏移（-1, 0, 1）
     * @param dirY Y 方向偏移（-1, 0, 1）
     * @return 方向字符（'N', 'E', 'S', 'W', 'X'）
     */
    private char getDirectionChar(int dirX, int dirY) {
        if (dirX == 0 && dirY == -1) return 'N';
        if (dirX == 1 && dirY == 0) return 'E';
        if (dirX == 0 && dirY == 1) return 'S';
        if (dirX == -1 && dirY == 0) return 'W';
        return 'X';
    }

    /**
     * 采集蛋白质。
     *
     * @param state       当前游戏状态
     * @param proteinType 蛋白质类型（"A", "B", "C", "D"）
     */
    private void harvestProtein(State state, String proteinType) {
        // 增加我方对应蛋白质的数量
        switch (proteinType) {
            case "A":
                state.myA += 1;
                break;
            case "B":
                state.myB += 1;
                break;
            case "C":
                state.myC += 1;
                break;
            case "D":
                state.myD += 1;
                break;
            default:
                break;
        }

        // 移除蛋白质源
        Iterator<Entity> iterator = state.proteins.iterator();
        while (iterator.hasNext()) {
            Entity e = iterator.next();
            if (e.type.name().equals(proteinType)) {
                iterator.remove();
                state.allEntities.remove(e);
                break;
            }
        }
    }

    /**
     * 生成新的器官 ID。
     *
     * @param state 当前游戏状态
     * @return 新的器官 ID
     */
    private int generateNewOrganId(State state) {
        int maxId = 0;
        for (Entity e : state.allEntities) {
            if (e.organId > maxId) {
                maxId = e.organId;
            }
        }
        return maxId + 1;
    }

    /**
     * 获取指定坐标处的蛋白质类型。
     *
     * @param state 当前游戏状态
     * @param x     X 坐标
     * @param y     Y 坐标
     * @return 蛋白质类型（"A", "B", "C", "D"）或 null
     */
    private String getProteinTypeAt(State state, int x, int y) {
        for (Entity e : state.proteins) {
            if (e.x == x && e.y == y) {
                return e.type.name();
            }
        }
        return null;
    }


    /**
     * 根据方向枚举获取对应的偏移量。
     *
     * @param direction 方向枚举（NORTH, EAST, SOUTH, WEST, NONE）
     * @return 对应的坐标偏移量
     */
    private int[] directionToOffset(Direction direction) {
        switch (direction) {
            case NORTH:
                return new int[]{0, -1};
            case EAST:
                return new int[]{1, 0};
            case SOUTH:
                return new int[]{0, 1};
            case WEST:
                return new int[]{-1, 0};
            default:
                return new int[]{0, 0};
        }
    }
}
