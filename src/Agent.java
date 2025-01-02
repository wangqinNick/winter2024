import java.util.*;

/**
 * Agent 完整示例：
 * 1) Minimax + alpha-beta, depth=2
 * 2) 移除了建SPORER时必须当场看到蛋白质的严格过滤，改为 canReachAnyProteinInLine(...)
 * 3) TENTACLE攻击、SPORE、HARVESTER等逻辑
 * 4) DEBUG 常量控制调试输出
 */
public class Agent {

    // ---------- 调试开关 ----------
    private static final boolean DEBUG = true;  // 若要在比赛时去掉所有调试日志，设为 false

    // ---------- 搜索参数 ----------
    private static final int SEARCH_DEPTH = 2;
    private static final int ACTIONS_LIMIT = 8;

    //===================================================================
    // 主入口: 返回本回合每个 root organism 的动作
    //===================================================================
    public List<Action> getActions(State state, int requiredActionsCount) {
        // 找出我方所有 root organ
        List<Entity> myRoots = new ArrayList<>();
        for (Entity e : state.myOrgans) {
            if (e.type == Entity.EntityType.ROOT && e.organParentId == 0) {
                myRoots.add(e);
            }
        }

        // 分别对每个root做一次搜索
        List<Action> results = new ArrayList<>();
        for (Entity root : myRoots) {
            Action bestAction = doMinimaxSearch(state, root, SEARCH_DEPTH);
            results.add(bestAction);
        }
        return results;
    }

    //===================================================================
    // Minimax 顶层搜索
    //===================================================================
    private Action doMinimaxSearch(State state, Entity myRoot, int depth) {
        // 生成可选动作
        List<Action> myActions = generateAllPossibleActionsForRoot(state, myRoot, true);

        if (DEBUG) {
            System.err.println("=== Possible actions for rootId=" + myRoot.organId + " ===");
            for(Action ac : myActions){
                System.err.println("   -> " + ac);
            }
        }

        if (myActions.isEmpty()) {
            return new Action(); // WAIT
        }

        // 快速打分 + 排序 + 截断
        List<ScoredAction> scoredList = new ArrayList<>();
        for (Action a : myActions) {
            State sim = simulate(state, a, true, myRoot);
            int val = evaluateState(sim);
            scoredList.add(new ScoredAction(a, val));

            if (DEBUG) {
                System.err.println("SimAction=" + a + " => Score=" + val);
            }
        }
        scoredList.sort((a,b)-> Integer.compare(b.score, a.score)); // 降序
        if (scoredList.size() > ACTIONS_LIMIT) {
            scoredList = scoredList.subList(0, ACTIONS_LIMIT);
        }

        // alpha-beta
        Action bestAct = null;
        int bestVal = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta  = Integer.MAX_VALUE;

        for (ScoredAction sc : scoredList) {
            State sim = simulate(state, sc.action, true, myRoot);
            int val = minimaxOpponent(sim, depth-1, alpha, beta);

            if (val > bestVal) {
                bestVal = val;
                bestAct = sc.action;
            }
            alpha = Math.max(alpha, bestVal);
            if (alpha >= beta) break;
        }

        if (DEBUG) {
            System.err.println("=== doMinimaxSearch => BestAct=" + bestAct + " with val=" + bestVal);
        }

        if (bestAct == null) {
            bestAct = scoredList.get(0).action;
        }
        return bestAct;
    }

    //===================================================================
    // 对手回合 (Minimax，对手想让我们的分尽量小)
    //===================================================================
    private int minimaxOpponent(State st, int depth, int alpha, int beta) {
        if (depth <= 0) {
            return evaluateState(st);
        }
        List<Action> oppActs = generateAllPossibleOpponentActions(st);
        if (oppActs.isEmpty()) {
            return evaluateState(st);
        }

        // 升序排序
        List<ScoredAction> scoredList = new ArrayList<>();
        for (Action a : oppActs) {
            State sim = simulate(st, a, false, null);
            int val = evaluateState(sim);
            scoredList.add(new ScoredAction(a, val));
        }
        scoredList.sort(Comparator.comparingInt(sa -> sa.score)); // ascending
        if (scoredList.size() > ACTIONS_LIMIT) {
            scoredList = scoredList.subList(0, ACTIONS_LIMIT);
        }

        int bestVal = Integer.MAX_VALUE;
        for (ScoredAction sc : scoredList) {
            State sim = simulate(st, sc.action, false, null);
            int val = minimaxMyTurn(sim, depth-1, alpha, beta);
            if (val < bestVal) {
                bestVal = val;
            }
            beta = Math.min(beta, bestVal);
            if (alpha >= beta) break;
        }
        return bestVal;
    }

    //===================================================================
    // 我方回合 (Minimax)，后续层
    //===================================================================
    private int minimaxMyTurn(State st, int depth, int alpha, int beta) {
        if (depth <= 0) {
            return evaluateState(st);
        }

        // 找出一个 root
        Entity singleRoot = null;
        for (Entity e : st.myOrgans) {
            if (e.type == Entity.EntityType.ROOT && e.organParentId == 0) {
                singleRoot = e;
                break;
            }
        }
        if (singleRoot == null) {
            return evaluateState(st);
        }

        List<Action> myActs = generateAllPossibleActionsForRoot(st, singleRoot, true);
        if (myActs.isEmpty()) {
            return evaluateState(st);
        }

        // quick evaluate => sort => limit
        List<ScoredAction> scoredList = new ArrayList<>();
        for (Action a : myActs) {
            State sim = simulate(st, a, true, singleRoot);
            int val = evaluateState(sim);
            scoredList.add(new ScoredAction(a, val));
        }
        scoredList.sort((a,b)-> Integer.compare(b.score,a.score)); // desc
        if (scoredList.size() > ACTIONS_LIMIT) {
            scoredList = scoredList.subList(0, ACTIONS_LIMIT);
        }

        int bestVal = Integer.MIN_VALUE;
        for (ScoredAction sc : scoredList) {
            State sim = simulate(st, sc.action, true, singleRoot);
            int val = minimaxOpponent(sim, depth-1, alpha, beta);
            if (val > bestVal) {
                bestVal = val;
            }
            alpha = Math.max(alpha, bestVal);
            if (alpha >= beta) break;
        }
        return bestVal;
    }

    //===================================================================
    // 根据当前 root organism 生成动作
    //===================================================================
    private List<Action> generateAllPossibleActionsForRoot(State s, Entity root, boolean isSelf){
        List<Action> acts= new ArrayList<>();

        // WAIT
        acts.add(new Action());

        // 找出 root organism 下所有器官
        List<Entity> organs = new ArrayList<>();
        for (Entity e : s.myOrgans) {
            if (e.type == Entity.EntityType.ROOT && e.organId != root.organId) {
                organs.add(e);
            }
            if (e.organRootId == root.organId) {
                organs.add(e);
            }
        }

        for (Entity o : organs) {
            acts.addAll(generatePossibleActionsForOrgan(s, o, isSelf));
        }
        return acts;
    }

    private List<Action> generateAllPossibleOpponentActions(State s){
        List<Action> result = new ArrayList<>();
        // WAIT
        result.add(new Action());
        // 对手简化 => BASIC / TENTACLE
        for (Entity opp : s.oppOrgans) {
            result.addAll(generatePossibleActionsForOrgan(s, opp, false));
        }
        return result;
    }

    //===================================================================
    // generatePossibleActionsForOrgan: 包含 SPORER / SPORE / TENTACLE / HARVESTER / BASIC
    //===================================================================
    private List<Action> generatePossibleActionsForOrgan(State st, Entity organ, boolean isSelf){
        List<Action> result = new ArrayList<>();

        int myA = isSelf? st.myA : st.oppA;
        int myB = isSelf? st.myB : st.oppB;
        int myC = isSelf? st.myC : st.oppC;
        int myD = isSelf? st.myD : st.oppD;

        // SPORE => organ==SPORER && A,B,C,D>=1
        if (organ.type == Entity.EntityType.SPORER && myA>=1 && myB>=1 && myC>=1 && myD>=1) {
            for (Entity p : st.proteins) {
                // 同行/同列 + 无阻挡 => SPORE
                if (isLineNoBlockAndSameRowOrCol(st, organ.x, organ.y, p.x, p.y)) {
                    if (DEBUG) {
                        System.err.println("Generate SPORE => sporerId="+organ.organId
                                +", to=("+p.x+","+p.y+")");
                    }
                    result.add(new Action(organ.organId, p.x, p.y)); // SPORE
                }
            }
        }

        // GROW SPORER !!!
        if (myB >=1 && myD >=1) {                           // 若有资源
            int[][] dirs = {{0,-1},{1,0},{0,1},{-1,0}};
            char[] ds     = {'N','E','S','W'};
            for (int i=0; i<4; i++) {                       // 遍历母体的四个方向
                int nx = organ.x + dirs[i][0];
                int ny = organ.y + dirs[i][1];
                if (!isCellFree(st, nx, ny)) continue;

                // 检查 (nx,ny) 朝 ds[i] 方向是否能到达蛋白质
                for (int j = 0; j < 4; j++) {
                    if (canReachAnyProteinInLine(st, nx, ny, ds[j])) {  // !
                        if (DEBUG) {
                            System.err.println("Generate SPORER => organId=" + organ.organId
                                    + ", at=(" + nx + "," + ny + "), dir=" + ds[j]);
                        }
                        result.add(new Action(organ.organId, nx, ny,
                                Action.ActionType.SPORER, ds[j]));
                    }
                }
            }
        }

        // GROW TENTACLE => (B+C)
        if (myB>=1 && myC>=1) {
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            char[] ds={'N','E','S','W'};
            for(int i=0;i<4;i++){
                int nx= organ.x + dd[i][0];
                int ny= organ.y + dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;
//                for (int j = 0; j < 4; j++) {
//                    result.add(new Action(organ.organId, nx, ny, Action.ActionType.TENTACLE, ds[j]));
//                }
                result.add(new Action(organ.organId, nx, ny, Action.ActionType.TENTACLE, ds[i]));
            }
        }

        // GROW HARVESTER => (C+D)
        if (myC>=1 && myD>=1) {
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            char[] ds={'N','E','S','W'};
            for(int i=0;i<4;i++){
                int nx= organ.x + dd[i][0];
                int ny= organ.y + dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;
//                for (int j = 0; j < 4; j++) {
//                    result.add(new Action(organ.organId, nx, ny, Action.ActionType.HARVESTER, ds[j]));
//                }
                result.add(new Action(organ.organId, nx, ny, Action.ActionType.HARVESTER, ds[i]));
            }
        }

        // GROW BASIC => (A)
        if (myA>=1) {
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            for(int i=0;i<4;i++){
                int nx= organ.x + dd[i][0];
                int ny= organ.y + dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;
                result.add(new Action(organ.organId, nx, ny, Action.ActionType.BASIC,'X'));
            }
        }

        // WAIT
//        result.add(new Action());

        return result;
    }

    //===================================================================
    // ! 判断 (sx,sy) 位置是否可往 direction 走到蛋白质(中途无墙/organ)
    //===================================================================
    private boolean canReachAnyProteinInLine(State st, int sx,int sy, char dirChar){
        Direction d = Direction.fromSymbol(dirChar);
        int[] off = directionToOffset(d);

        int cx = sx, cy = sy;
        while(true){
            cx += off[0];
            cy += off[1];
            if(st.isOutOfBounds(cx,cy)) return false;
            if(st.isWall(cx,cy)) return false;
            if(st.isMyOrgan(cx,cy) || st.isOppOrgan(cx,cy)) return false;
            if(st.isProteinTile(cx,cy)){
                return true; // 只要能找到一个蛋白质
            }
        }
    }

    // SPORE时：同行/同列 & 无阻挡
    private boolean isLineNoBlockAndSameRowOrCol(State st, int sx,int sy,int tx,int ty){
        if(!(sx==tx || sy==ty)) return false;
        if(!st.isProteinTile(tx,ty)) return false;

        int dx= (tx>sx)?1: ((tx<sx)?-1:0);
        int dy= (ty>sy)?1: ((ty<sy)?-1:0);

        int cx= sx, cy= sy;
        while(true){
            cx+=dx; cy+=dy;
            if(cx==tx && cy==ty){
                return true;
            }
            if(st.isOutOfBounds(cx,cy)) return false;
            if(st.isWall(cx,cy)) return false;
            if(st.isMyOrgan(cx,cy)|| st.isOppOrgan(cx,cy)) return false;
        }
    }

    private boolean isCellFree(State st, int x,int y){
        if(st.isOutOfBounds(x,y)) return false;
        Entity e= st.grid[y][x];
        if(e==null) return true;
        // 可踩蛋白质
        if(e.type== Entity.EntityType.A || e.type== Entity.EntityType.B
                || e.type== Entity.EntityType.C || e.type== Entity.EntityType.D){
            return true;
        }
        return false;
    }

    //=================== simulate ===================//

    public State simulate(State origin, Action act, boolean isSelf, Entity myRoot){
        State st= new State(origin);

        switch(act.getActionType()){
            case WAIT:
                break;

            case BASIC:
                doGrowBasic(st, act, isSelf, myRoot);
                break;

            case HARVESTER:
                doGrowHarvester(st, act, isSelf, myRoot);
                break;

            case TENTACLE:
                doGrowTentacle(st, act, isSelf, myRoot);
                break;

            case SPORER:
                doGrowSporer(st, act, isSelf, myRoot);
                break;

            case SPORE:
                doSpore(st, act, isSelf);
                break;
        }

        doTentacleAttack(st);
        return st;
    }

    private void doGrowBasic(State s, Action a, boolean isSelf, Entity root){
        if(isSelf) s.myA--; else s.oppA--;
        Entity b= new Entity(a.getX(),a.getY(), Entity.EntityType.BASIC,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), Direction.NONE, a.getOrganId(),
                root==null?0: root.organId
        );
        s.addEntityDirect(b);

        // 踩蛋白质 => +3
        Entity tile= s.grid[a.getY()][a.getX()];
        if(tile!=b && tile!=null &&
                (tile.type== Entity.EntityType.A
                        || tile.type== Entity.EntityType.B
                        || tile.type== Entity.EntityType.C
                        || tile.type== Entity.EntityType.D)){
            if(tile.type== Entity.EntityType.A){ if(isSelf) s.myA+=3; else s.oppA+=3; }
            if(tile.type== Entity.EntityType.B){ if(isSelf) s.myB+=3; else s.oppB+=3; }
            if(tile.type== Entity.EntityType.C){ if(isSelf) s.myC+=3; else s.oppC+=3; }
            if(tile.type== Entity.EntityType.D){ if(isSelf) s.myD+=3; else s.oppD+=3; }
            s.removeEntity(tile);
        }
    }

    private void doGrowHarvester(State s, Action a, boolean isSelf, Entity root){
        // cost C+D
        if(isSelf){ s.myC--; s.myD--; }
        else { s.oppC--; s.oppD--; }

        Direction d= Direction.fromSymbol(a.getDirection());
        Entity h= new Entity(a.getX(), a.getY(), Entity.EntityType.HARVESTER,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), d, a.getOrganId(),
                root==null?0: root.organId
        );
        s.addEntityDirect(h);
    }

    private void doGrowTentacle(State s, Action a, boolean isSelf, Entity root){
        // cost B+C
        if(isSelf){ s.myB--; s.myC--; }
        else { s.oppB--; s.oppC--; }

        Direction d= Direction.fromSymbol(a.getDirection());
        Entity t= new Entity(a.getX(), a.getY(), Entity.EntityType.TENTACLE,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), d, a.getOrganId(),
                root==null?0: root.organId
        );
        s.addEntityDirect(t);
    }

    private void doGrowSporer(State s, Action a, boolean isSelf, Entity root){
        // cost B+D
        if(isSelf){ s.myB--; s.myD--; }
        else { s.oppB--; s.oppD--; }

        Direction d= Direction.fromSymbol(a.getDirection());
        Entity sp= new Entity(a.getX(), a.getY(), Entity.EntityType.SPORER,
                isSelf?Entity.Owner.SELF:Entity.Owner.OPPONENT,
                genId(s), d, a.getOrganId(),
                root==null?0: root.organId
        );
        s.addEntityDirect(sp);
    }

    private void doSpore(State s, Action a, boolean isSelf){
        // cost A,B,C,D
        if(isSelf){ s.myA--; s.myB--; s.myC--; s.myD--; }
        else { s.oppA--; s.oppB--; s.oppC--; s.oppD--; }

        // 生成新的ROOT
        Entity r= new Entity(a.getX(), a.getY(), Entity.EntityType.ROOT,
                isSelf?Entity.Owner.SELF:Entity.Owner.OPPONENT,
                genId(s), Direction.NONE, 0,0
        );
        s.addEntityDirect(r);
    }

    private void doTentacleAttack(State s){
        // 我方 tent => kill 对手
        List<Entity> killOpp= new ArrayList<>();
        for(Entity t: s.myOrgans){
            if(t.type== Entity.EntityType.TENTACLE){
                int[] off= directionToOffset(t.direction);
                int fx= t.x+ off[0], fy= t.y+ off[1];
                if(!s.isOutOfBounds(fx,fy)){
                    Entity e= s.grid[fy][fx];
                    if(e!=null && e.owner== Entity.Owner.OPPONENT){
                        killOrganAndDescendants(s,e,false,killOpp);
                    }
                }
            }
        }

        // 对手 tent => kill 我方
        List<Entity> killMe= new ArrayList<>();
        for(Entity t: s.oppOrgans){
            if(t.type== Entity.EntityType.TENTACLE){
                int[] off= directionToOffset(t.direction);
                int fx= t.x+ off[0], fy= t.y+ off[1];
                if(!s.isOutOfBounds(fx,fy)){
                    Entity e= s.grid[fy][fx];
                    if(e!=null && e.owner== Entity.Owner.SELF){
                        killOrganAndDescendants(s,e,true,killMe);
                    }
                }
            }
        }

        for(Entity e: killOpp) s.removeEntity(e);
        for(Entity e: killMe)  s.removeEntity(e);
    }

    private void killOrganAndDescendants(State s, Entity target, boolean isMySide, List<Entity> removal){
        Queue<Entity> Q= new LinkedList<>();
        Q.add(target);
        while(!Q.isEmpty()){
            Entity d= Q.poll();
            if(!removal.contains(d)){
                removal.add(d);
                List<Entity> pool= isMySide? s.myOrgans: s.oppOrgans;
                for(Entity c : pool){
                    if(c.organParentId== d.organId){
                        Q.add(c);
                    }
                }
            }
        }
    }

    //================= 评估函数 =================//
    private int evaluateState(State st){
        int score=0;

        // 1) organ数量差
        int diff= st.myOrgans.size() - st.oppOrgans.size();
        score += 10 * diff;

        // 2) 资源差
        int myRes= st.myA + st.myB + st.myC + st.myD;
        int oppRes= st.oppA + st.oppB + st.oppC + st.oppD;
        score += 2 * (myRes - oppRes);

        // 3) 多ROOT => +80*(rootCount-1)
        int rootCount=0;
        for(Entity e: st.myOrgans){
            if(e.type== Entity.EntityType.ROOT && e.organParentId==0){
                rootCount++;
            }
        }
        if(rootCount>1){
            score += 80*(rootCount-1);
        }

        // 4) SPORER => +40，每个当场可 SPORE => +60
        for(Entity e: st.myOrgans){
            if(e.type == Entity.EntityType.SPORER){
                score+= 40;
                // 若A,B,C,D>=1 && 当场可 SPORE => +60
                if(st.myA>=1 && st.myB>=1 && st.myC>=1 && st.myD>=1){
                    if(canSporeThisTurn(st,e)){
                        score+= 60;
                    }
                }
            }
        }

        // 5) TENTACLE面向对手 => +30
        score += measureTentacleFacing(st);

        // 6) HARVESTER 面向蛋白质 => +10
        score += measureHarvesterFacing(st);

        // 7) SPORER 离蛋白质 BFS => +(20-dist)
        score += measureSporerDistance(st);

        return score;
    }

    // 若能SPORE => 遍历蛋白质
    private boolean canSporeThisTurn(State st, Entity sp){
        for(Entity p: st.proteins){
            if(isLineNoBlockAndSameRowOrCol(st, sp.x, sp.y, p.x, p.y)){
                return true;
            }
        }
        return false;
    }

    private int measureTentacleFacing(State st){
        int val=0;
        for(Entity t: st.myOrgans){
            if(t.type== Entity.EntityType.TENTACLE){
                int[] off= directionToOffset(t.direction);
                int fx= t.x+ off[0], fy= t.y+ off[1];
                if(!st.isOutOfBounds(fx,fy)){
                    Entity e= st.grid[fy][fx];
                    if(e!=null && e.owner== Entity.Owner.OPPONENT){
                        val+= 30;
                    }
                }
            }
        }
        return val;
    }

    private int measureHarvesterFacing(State st){
        int val=0;
        for(Entity h: st.myOrgans){
            if(h.type== Entity.EntityType.HARVESTER){
                int[] off= directionToOffset(h.direction);
                int fx= h.x+ off[0], fy= h.y+ off[1];
                if(!st.isOutOfBounds(fx,fy)){
                    Entity e= st.grid[fy][fx];
                    if(e!=null && (e.type== Entity.EntityType.A
                            || e.type== Entity.EntityType.B
                            || e.type== Entity.EntityType.C
                            || e.type== Entity.EntityType.D)){
                        val+= 10;
                    }
                }
            }
        }
        return val;
    }

    private int measureSporerDistance(State st){
        int val=0;
        for(Entity sp: st.myOrgans){
            if(sp.type== Entity.EntityType.SPORER){
                int dist= findClosestProteinDist(st, sp.x, sp.y);
                if(dist<99999){
                    val += Math.max(0, 20-dist);
                }
            }
        }
        return val;
    }

    private int findClosestProteinDist(State st,int sx,int sy){
        if(st.isOutOfBounds(sx,sy)) return 99999;
        if(st.isProteinTile(sx,sy)) return 0;

        boolean[][] visited= new boolean[st.height][st.width];
        Queue<int[]> Q= new LinkedList<>();
        Q.offer(new int[]{sx,sy,0});
        visited[sy][sx]= true;

        int[][] dirs={{0,-1},{1,0},{0,1},{-1,0}};
        while(!Q.isEmpty()){
            int[] cur= Q.poll();
            int cx=cur[0], cy=cur[1], cd=cur[2];
            for(int[] d: dirs){
                int nx= cx+d[0], ny= cy+d[1];
                if(nx<0||nx>=st.width|| ny<0||ny>=st.height) continue;
                if(visited[ny][nx]) continue;
                if(st.isWall(nx,ny)) continue;
                visited[ny][nx]= true;
                if(st.isProteinTile(nx,ny)){
                    return cd+1;
                }
                Q.offer(new int[]{nx,ny,cd+1});
            }
        }
        return 99999;
    }

    private int[] directionToOffset(Direction d){
        switch(d){
            case NORTH:return new int[]{0,-1};
            case EAST:return new int[]{1,0};
            case SOUTH:return new int[]{0,1};
            case WEST:return new int[]{-1,0};
            default:return new int[]{0,0};
        }
    }

    private int genId(State st){
        int mx=0;
        for(Entity e: st.allEntities){
            if(e.organId>mx) mx=e.organId;
        }
        return mx+1;
    }

    //================= 内部类 =================//
    private static class ScoredAction {
        Action action;
        int score;
        ScoredAction(Action a,int s){
            this.action=a;
            this.score=s;
        }
    }
}
