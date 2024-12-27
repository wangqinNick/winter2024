import java.util.*;

public class Agent {

    private Random rng = new Random(); // 随机数

    /**
     * 获取当前回合的行动。
     *
     * @param state                当前游戏状态
     * @param requiredActionsCount 本回合需要执行的动作数量（通常为1）
     * @return Action 对象，代表本回合的行动
     */
    public Action getAction(State state, int requiredActionsCount) {

        // 收集所有可能动作
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

            System.err.println("模拟动作: " + action + ", 得分: " + score);

            // 更新最佳动作
            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        if (bestAction != null) {
            System.err.println(String.format("选择动作: %s, 得分: %d", bestAction, bestScore));
            return bestAction;
        } else {
            // 如果没有找到合适的，就随机一个
            Action selectedAction = possibleActions.get(rng.nextInt(possibleActions.size()));
            System.err.println(String.format("随机动作: %s", selectedAction));
            return selectedAction;
        }
    }

    //=================================================================
    //  1) 改动点：限制 HARVESTER 动作只有在面前格子为蛋白质时才可执行
    //=================================================================
    /**
     * 收集所有可能动作。
     * - 若下一格可以长 organ，则考虑放 BASIC。
     * - 若下一格前方也确有蛋白质，则可以放 HARVESTER。
     * - 每回合还可选择 WAIT。
     */
    private List<Action> getAllPossibleActions(State state) {
        List<Action> actions = new ArrayList<>();

        // 定义四个方向及其对应的偏移
        int[][] directions = { {0, -1}, {1, 0}, {0, 1}, {-1, 0} }; // N, E, S, W
        char[] dirChars    = { 'N',     'E',    'S',    'W'  };

        for (Entity myOrgan : state.myOrgans) {
            for (int i = 0; i < directions.length; i++) {
                int dx = directions[i][0];
                int dy = directions[i][1];
                char dirChar = dirChars[i];

                // 新 organ 要长到的目标格
                int targetX = myOrgan.x + dx;
                int targetY = myOrgan.y + dy;

                // 判断能否生长 BASIC/HARVESTER
                if (!canGrowOrgan(state, targetX, targetY)) {
                    continue;
                }

                // ========== 放置 HARVESTER 的过滤逻辑 ==========
                // 只有当目标格前方一格为蛋白质时，才考虑 HARVESTER
                // 即 (targetX + dx, targetY + dy) 必须是蛋白质。
                if (state.myC >= 1 && state.myD >= 1) {
                    int frontX = targetX + dx;
                    int frontY = targetY + dy;
                    if (!state.isOutOfBounds(frontX, frontY) && state.isProteinTile(frontX, frontY)) {
                        // 确定“前方”是蛋白质才生成 HARVESTER 动作
                        actions.add(new Action(myOrgan.organId, targetX, targetY,
                                Action.ActionType.HARVESTER, dirChar));
                    }
                }

                // ========== 放置 BASIC ==========
                if (state.myA >= 1) {
                    actions.add(new Action(myOrgan.organId, targetX, targetY,
                            Action.ActionType.BASIC, 'X'));
                }
            }
        }

        // WAIT 动作
        actions.add(new Action()); // WAIT

        return actions;
    }

    /**
     * 判断在 (x, y) 是否可以生长一个器官 (BASIC/HARVESTER)。
     */
    private boolean canGrowOrgan(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y))       return false;
        if (state.isMyOrgan(x, y))    return false;
        if (state.isOppOrgan(x, y))   return false;
        // 可以生长在蛋白质源或空地
        return true;
    }

    //=================================================================
    //  2) 改动点：评估函数中对“HARVESTER 不面向蛋白质”进行负向惩罚
    //=================================================================
    /**
     * 评估给定状态的得分。
     */
    public int evaluateState(State state) {
        // 设定一些权重
        double wHarvesterFacingGood   = 50.0; // Harvester 面向蛋白质的奖励
        double wHarvesterFacingBad    = 15.0; // Harvester 面向空地的惩罚
        double wBasicOrganCount       = 10.0; // BASIC + ROOT + HARVESTER 的总数量加分
        double wResource              = 2.0;  // 全局资源数加分
        double wOrganDiff             = 3.0;  // 我方和对手器官数差

        double totalScore = 0.0;

        // (1) 统计 Harvester 朝向蛋白质
        int harvestersFacingProtein = 0;
        int harvestersFacingEmpty   = 0;
        for (Entity harv : state.ownHarvesters) {
            int[] offset = directionToOffset(harv.direction);
            int hx = harv.x + offset[0];
            int hy = harv.y + offset[1];
            if (!state.isOutOfBounds(hx, hy) && state.isProteinTile(hx, hy)) {
                // 面向蛋白质
                harvestersFacingProtein++;
            } else {
                // 不面向蛋白质 -> 惩罚
                harvestersFacingEmpty++;
            }
        }
        totalScore += wHarvesterFacingGood * harvestersFacingProtein;
        totalScore -= wHarvesterFacingBad * harvestersFacingEmpty;

        // (2) 我方器官数量加分
        totalScore += wBasicOrganCount * state.myOrgans.size();

        // (3) 资源加分
        int totalRes = state.myA + state.myB + state.myC + state.myD;
        totalScore += wResource * totalRes;

        // (4) 对手差值
        int diffOrgans = state.myOrgans.size() - state.oppOrgans.size();
        totalScore += wOrganDiff * diffOrgans;

        // 还可以加更多指标，比如蛋白质距离、蛋白质踩取等

        return (int) totalScore;
    }

    /**
     * 模拟执行一个动作后的新状态 (单步)。
     *
     * @param state  当前游戏状态
     * @param action 要执行的动作
     * @return 新状态
     */
    public State simulate(State state, Action action) {
        // 深拷贝当前状态
        State newState = new State(state);

        switch (action.getActionType()) {
            case WAIT:
                // WAIT 不改变任何东西
                break;
            case BASIC:
                growBasic(newState, action);
                break;
            case HARVESTER:
                growHarvester(newState, action);
                break;
            default:
                // 未知动作，忽略
                break;
        }

        return newState;
    }

    /**
     * 生长 BASIC organ。
     */
    private void growBasic(State state, Action action) {
        // 消耗 1 A
        state.myA -= 1;

        // 创建新的 BASIC 器官
        Entity newBasic = new Entity(
                action.getX(),
                action.getY(),
                Entity.EntityType.BASIC,
                Entity.Owner.SELF,
                generateNewOrganId(state),
                Direction.NONE,
                action.getOrganId(), // 父器官
                0
        );
        state.allEntities.add(newBasic);
        state.myOrgans.add(newBasic);
    }

    /**
     * 生长 HARVESTER organ。
     */
    private void growHarvester(State state, Action action) {
        // 消耗 1 C + 1 D
        state.myC -= 1;
        state.myD -= 1;

        Direction harvesterDir = Direction.fromSymbol(action.getDirection());

        Entity newHarvester = new Entity(
                action.getX(),
                action.getY(),
                Entity.EntityType.HARVESTER,
                Entity.Owner.SELF,
                generateNewOrganId(state),
                harvesterDir,
                action.getOrganId(), // 父器官
                0
        );
        state.allEntities.add(newHarvester);
        state.myOrgans.add(newHarvester);
        state.ownHarvesters.add(newHarvester);
    }

    /**
     * 生成新的 organId。
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
     * 根据枚举方向获取坐标偏移。
     */
    private int[] directionToOffset(Direction dir) {
        switch (dir) {
            case NORTH: return new int[]{0, -1};
            case EAST:  return new int[]{1, 0};
            case SOUTH: return new int[]{0, 1};
            case WEST:  return new int[]{-1, 0};
            default:    return new int[]{0, 0};
        }
    }
}
