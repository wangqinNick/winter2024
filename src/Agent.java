import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Agent 类：决定本回合要下什么指令。
 * 当前是单步搜索 + 评估函数。
 */
public class Agent {

    private Random rng = new Random();

    /**
     * 主入口：给定当前状态与需要输出的动作数(通常1)，返回一条最优指令
     */
    public Action getAction(State state, int requiredActionsCount) {
        // 收集所有可行动作
        List<Action> possibleActions = getAllPossibleActions(state);

        if (possibleActions.isEmpty()) {
            return new Action(); // WAIT
        }

        Action bestAction = null;
        int bestScore = Integer.MIN_VALUE;

        for (Action action : possibleActions) {
            // simulate
            State simulated = simulate(state, action);
            // evaluate
            int score = evaluateState(simulated);

            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        if (bestAction == null) {
            // fallback: 随机
            return possibleActions.get(rng.nextInt(possibleActions.size()));
        }
        System.err.println("Best Action: " + bestAction + " => score=" + bestScore);
        return bestAction;
    }

    /**
     * 收集所有可行动作 (BASIC, HARVESTER, TENTACLE, WAIT)
     */
    private List<Action> getAllPossibleActions(State state) {
        List<Action> actions = new ArrayList<>();

        int[][] directions = { {0,-1},{1,0},{0,1},{-1,0} };
        char[] dirChars    = { 'N', 'E', 'S', 'W' };

        for (Entity myOrgan : state.myOrgans) {
            for (int i=0; i<directions.length; i++) {
                int dx = directions[i][0];
                int dy = directions[i][1];
                char dirChar = dirChars[i];

                int nx = myOrgan.x + dx;
                int ny = myOrgan.y + dy;

                // 判断能否生长
                if (!canGrowOrgan(state, nx, ny)) {
                    continue;
                }

                // 1) 放 TENTACLE
                if (state.myB >= 1 && state.myC >= 1) {
                    actions.add(new Action(myOrgan.organId, nx, ny, Action.ActionType.TENTACLE, dirChar));
                }

                // 2) 放 BASIC
                if (state.myA >= 1) {
                    actions.add(new Action(myOrgan.organId, nx, ny, Action.ActionType.BASIC, 'X'));
                }

                // 3) 放 HARVESTER (若你想在无蛋白质地图上也造收集器，可保留)
                if (state.myC >= 1 && state.myD >= 1) {
                    actions.add(new Action(myOrgan.organId, nx, ny, Action.ActionType.HARVESTER, dirChar));
                }
            }
        }

        // 4) WAIT
        actions.add(new Action()); // WAIT

        return actions;
    }

    /**
     * 判断 (x,y) 是否能生长一个 organ
     */
    private boolean canGrowOrgan(State state, int x, int y) {
        if (state.isOutOfBounds(x, y)) return false;
        if (state.isWall(x, y))       return false;
        if (state.isMyOrgan(x, y))    return false;
        if (state.isOppOrgan(x, y))   return false;
        return true;
    }

    /**
     * 对给定动作进行模拟
     */
    public State simulate(State state, Action action) {
        // 深拷贝
        State newState = new State(state);

        switch (action.getActionType()) {
            case WAIT:
                // 不做任何事
                break;
            case BASIC:
                growBasic(newState, action);
                break;
            case HARVESTER:
                growHarvester(newState, action);
                break;
            case TENTACLE:
                growTentacle(newState, action);
                break;
            default:
                break;
        }

        // 如果需要也可在这里模拟TENTACLE攻击，但通常比赛引擎会下一回合才呈现效果
        return newState;
    }

    private void growBasic(State s, Action action) {
        s.myA -= 1;
        Entity basic = new Entity(
                action.getX(), action.getY(),
                Entity.EntityType.BASIC,
                Entity.Owner.SELF,
                generateOrganId(s),
                Direction.NONE,
                action.getOrganId(),
                0
        );
        s.allEntities.add(basic);
        s.myOrgans.add(basic);
    }

    private void growHarvester(State s, Action action) {
        s.myC -= 1;
        s.myD -= 1;
        Direction dir = Direction.fromSymbol(action.getDirection());
        Entity harv = new Entity(
                action.getX(), action.getY(),
                Entity.EntityType.HARVESTER,
                Entity.Owner.SELF,
                generateOrganId(s),
                dir,
                action.getOrganId(),
                0
        );
        s.allEntities.add(harv);
        s.myOrgans.add(harv);
        s.ownHarvesters.add(harv);
    }

    private void growTentacle(State s, Action action) {
        // 消耗B, C
        s.myB -= 1;
        s.myC -= 1;

        Direction dir = Direction.fromSymbol(action.getDirection());
        Entity tent = new Entity(
                action.getX(), action.getY(),
                Entity.EntityType.TENTACLE,
                Entity.Owner.SELF,
                generateOrganId(s),
                dir,
                action.getOrganId(),
                0
        );
        s.allEntities.add(tent);
        s.myOrgans.add(tent);
        s.ownTentacles.add(tent);
    }

    /**
     * 简易评估函数
     */
    public int evaluateState(State s) {
        double wMyOrganCount    = 10.0;
        double wDiffOrganCount  = 5.0;
        double wMyResources     = 2.0;
        double wTentacleFacing  = 25.0; // TENTACLE正面朝对手organ的价值

        double score = 0.0;

        // 1) 我方器官数量
        score += wMyOrganCount * s.myOrgans.size();

        // 2) 我方 - 对手 器官数差
        int diff = s.myOrgans.size() - s.oppOrgans.size();
        score += wDiffOrganCount * diff;

        // 3) 资源越多越好
        int totalMyRes = s.myA + s.myB + s.myC + s.myD;
        score += wMyResources * totalMyRes;

        // 4) 若 TENTACLE 正面朝对手organ，则加分
        for (Entity tent : s.ownTentacles) {
            int[] offset = directionToOffset(tent.direction);
            int fx = tent.x + offset[0];
            int fy = tent.y + offset[1];
            // 若刚好对手organ在 (fx, fy)
            for (Entity opp : s.oppOrgans) {
                if (opp.x == fx && opp.y == fy) {
                    score += wTentacleFacing;
                    break;
                }
            }
        }

        return (int) score;
    }

    private int generateOrganId(State s) {
        int maxId = 0;
        for (Entity e : s.allEntities) {
            if (e.organId > maxId) {
                maxId = e.organId;
            }
        }
        return maxId + 1;
    }

    private int[] directionToOffset(Direction d) {
        switch (d) {
            case NORTH: return new int[]{0,-1};
            case EAST:  return new int[]{1,0};
            case SOUTH: return new int[]{0,1};
            case WEST:  return new int[]{-1,0};
            default:    return new int[]{0,0};
        }
    }
}
