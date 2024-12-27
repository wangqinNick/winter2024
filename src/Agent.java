import java.util.*;

public class Agent {

    private Random rng = new Random();

    /**
     * 多 organism 同时行动：为每个 root organ 输出一条指令 (或 WAIT)
     */
    public List<Action> getActions(State state, int requiredActionsCount) {
        // 1) 找出我方所有 root organ
        List<Entity> myRoots = new ArrayList<>();
        for (Entity organ : state.myOrgans) {
            // organParentId=0 表示它本身是个ROOT
            if (organ.type == Entity.EntityType.ROOT && organ.organParentId == 0) {
                myRoots.add(organ);
            }
        }
        // 2) 对每个 root organ，决定一个最佳指令
        List<Action> results = new ArrayList<>();

        for (Entity root : myRoots) {
            Action bestCmd = decideSingleOrganismAction(state, root);
            results.add(bestCmd);
        }

        return results;
    }

    /**
     * 针对某个 ROOT 领导的 organism，决定一个动作
     * (单步搜索demo：在其子器官中，找到可能最优GROW/SPORE/WAIT)
     */
    private Action decideSingleOrganismAction(State state, Entity root) {
        // 收集“该 organism”内的所有器官
        List<Entity> organsOfThisRoot = new ArrayList<>();
        for (Entity o : state.myOrgans) {
            if (o.organRootId == root.organId) {
                organsOfThisRoot.add(o);
            }
        }

        // 收集可用动作
        List<Action> candidateActions = new ArrayList<>();
        candidateActions.add(new Action()); // WAIT

        // 遍历这些 organ，生成 grow/spore
        for (Entity organ : organsOfThisRoot) {
            candidateActions.addAll(generatePossibleActionsForOrgan(state, organ));
        }

        // 简单打分，选最优
        Action best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Action act : candidateActions) {
            State simulated = simulate(state, root, act);
            int score = evaluateState(simulated, root);
            if (score > bestScore) {
                bestScore = score;
                best = act;
            }
        }
        return (best != null) ? best : new Action(); // fallback WAIT
    }

    /**
     * 针对给定 organ，生成可能动作 (GROW BASIC/HARVESTER/TENTACLE/SPORER) + SPORE
     */
    private List<Action> generatePossibleActionsForOrgan(State state, Entity organ) {
        List<Action> actions = new ArrayList<>();

        int[][] dirs = { {0,-1},{1,0},{0,1},{-1,0} };
        char[] dirSym = { 'N','E','S','W' };

        // 如果是 SPORER，则可以尝试发射 SPORE(新ROOT)
        // 需要 1A+1B+1C+1D
        if (organ.type == Entity.EntityType.SPORER) {
            if (state.myA >= 1 && state.myB >= 1 && state.myC >= 1 && state.myD >= 1) {
                // demo: 任意一格(甚至可多格)——实际上规则是“射线”直线可达空地
                // 这里只示例 pick some place forward
                // 你可以 BFS 或远射到特定坐标
                for (int i = 0; i < dirs.length; i++) {
                    // 这里简单示例：只要面前不越界就 SPORE
                    // 真实中你可遍历一整条线
                    int nx = organ.x + dirs[i][0];
                    int ny = organ.y + dirs[i][1];
                    if (!state.isOutOfBounds(nx, ny)
                            && !state.isWall(nx, ny)
                            && !state.isMyOrgan(nx, ny)
                            && !state.isOppOrgan(nx, ny))
                    {
                        actions.add(new Action(organ.organId, nx, ny)); // SPORE id x y
                    }
                }
            }
        }

        // 对所有 organ，尝试 GROW SPORER
        if (state.myB >= 1 && state.myD >= 1) {
            // 可以造 SPORER
            for (int i = 0; i < dirs.length; i++) {
                int nx = organ.x + dirs[i][0];
                int ny = organ.y + dirs[i][1];
                if (!canGrowOrgan(state, nx, ny)) continue;
                actions.add(new Action(organ.organId, nx, ny,
                        Action.ActionType.SPORER, dirSym[i]));
            }
        }

        // GROW TENTACLE (需要 1B+1C)
        if (state.myB >= 1 && state.myC >= 1) {
            for (int i = 0; i < dirs.length; i++) {
                int nx = organ.x + dirs[i][0];
                int ny = organ.y + dirs[i][1];
                if (!canGrowOrgan(state, nx, ny)) continue;
                actions.add(new Action(organ.organId, nx, ny,
                        Action.ActionType.TENTACLE, dirSym[i]));
            }
        }

        // GROW HARVESTER (需要 1C+1D)
        if (state.myC >= 1 && state.myD >= 1) {
            for (int i = 0; i < dirs.length; i++) {
                int nx = organ.x + dirs[i][0];
                int ny = organ.y + dirs[i][1];
                if (!canGrowOrgan(state, nx, ny)) continue;
                actions.add(new Action(organ.organId, nx, ny,
                        Action.ActionType.HARVESTER, dirSym[i]));
            }
        }

        // GROW BASIC (需要1A)
        if (state.myA >= 1) {
            for (int i = 0; i < dirs.length; i++) {
                int nx = organ.x + dirs[i][0];
                int ny = organ.y + dirs[i][1];
                if (!canGrowOrgan(state, nx, ny)) continue;
                actions.add(new Action(organ.organId, nx, ny,
                        Action.ActionType.BASIC, 'X'));
            }
        }

        return actions;
    }

    private boolean canGrowOrgan(State s, int x, int y) {
        if (s.isOutOfBounds(x, y)) return false;
        if (s.isWall(x, y))       return false;
        if (s.isMyOrgan(x, y))    return false;
        if (s.isOppOrgan(x, y))   return false;
        // 也可判断蛋白质格 -> 允许踩
        return true;
    }

    /**
     * 简化版：模拟单条动作对 "root organism" 的影响
     */
    private State simulate(State original, Entity root, Action action) {
        State newState = new State(original); // 深拷贝

        switch (action.getActionType()) {
            case WAIT:
                break;
            case BASIC:
                growBasic(newState, action, root);
                break;
            case HARVESTER:
                growHarvester(newState, action, root);
                break;
            case TENTACLE:
                growTentacle(newState, action, root);
                break;
            case SPORER:
                growSporer(newState, action, root);
                break;
            case SPORE:
                sporeNewRoot(newState, action);
                break;
            default:
                break;
        }
        return newState;
    }

    private void growBasic(State s, Action a, Entity root) {
        s.myA -= 1;
        Entity e = new Entity(a.getX(), a.getY(),
                Entity.EntityType.BASIC,
                Entity.Owner.SELF,
                generateNewOrganId(s),
                Direction.NONE,
                a.getOrganId(),
                root.organId // 同一个root
        );
        s.allEntities.add(e);
        s.myOrgans.add(e);
    }

    private void growHarvester(State s, Action a, Entity root) {
        s.myC -= 1; s.myD -= 1;
        Direction dir = Direction.fromSymbol(a.getDirection());
        Entity e = new Entity(a.getX(), a.getY(),
                Entity.EntityType.HARVESTER,
                Entity.Owner.SELF,
                generateNewOrganId(s),
                dir,
                a.getOrganId(),
                root.organId
        );
        s.allEntities.add(e);
        s.myOrgans.add(e);
    }

    private void growTentacle(State s, Action a, Entity root) {
        s.myB -= 1; s.myC -= 1;
        Direction dir = Direction.fromSymbol(a.getDirection());
        Entity e = new Entity(a.getX(), a.getY(),
                Entity.EntityType.TENTACLE,
                Entity.Owner.SELF,
                generateNewOrganId(s),
                dir,
                a.getOrganId(),
                root.organId
        );
        s.allEntities.add(e);
        s.myOrgans.add(e);
    }

    private void growSporer(State s, Action a, Entity root) {
        s.myB -= 1; s.myD -= 1;
        Direction dir = Direction.fromSymbol(a.getDirection());
        Entity e = new Entity(a.getX(), a.getY(),
                Entity.EntityType.SPORER,
                Entity.Owner.SELF,
                generateNewOrganId(s),
                dir,
                a.getOrganId(),
                root.organId
        );
        s.allEntities.add(e);
        s.myOrgans.add(e);
    }

    /**
     * SPORE 指令 -> 新的 ROOT organ 出现
     * cost: A,B,C,D 各 1
     */
    private void sporeNewRoot(State s, Action a) {
        s.myA -= 1; s.myB -= 1; s.myC -= 1; s.myD -= 1;

        // ROOT organ never has parentId
        Entity root = new Entity(
                a.getX(), a.getY(),
                Entity.EntityType.ROOT,
                Entity.Owner.SELF,
                generateNewOrganId(s),
                Direction.NONE,
                0, // parent=0
                0  // rootId=0 (自己就是root)
        );
        s.allEntities.add(root);
        s.myOrgans.add(root);
    }

    /**
     * 评估函数 (非常简单示例)
     */
    private int evaluateState(State s, Entity root) {
        int score = 0;

        // 1) 我方 organ 总数
        score += 10 * s.myOrgans.size();

        // 2) 对手 organ 差
        int diff = s.myOrgans.size() - s.oppOrgans.size();
        score += 5 * diff;

        // 3) 我方剩余资源
        int sumRes = s.myA + s.myB + s.myC + s.myD;
        score += 2 * sumRes;

        // 4) 如果放了 Harvester 面向蛋白质?
        // 5) 如果 Sporer 正面对手?
        // 6) 如果离蛋白质更近? (可加 BFS 计算)
        // ...
        // 自行扩展

        return score;
    }

    /**
     * 生成新 organId
     */
    private int generateNewOrganId(State s) {
        int maxId = 0;
        for (Entity e : s.allEntities) {
            if (e.organId > maxId) maxId = e.organId;
        }
        return maxId + 1;
    }
}
