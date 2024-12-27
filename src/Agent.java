import java.util.*;

/**
 * 完整示例：
 * 1) 使用 Minimax + alpha-beta（深度=2）;
 * 2) 在 build SPORER 时不再强制 "hasProteinInLine" => 改为 "canReachAnyProteinInLine" 或更宽松；
 * 3) 保留 SPORE, TENTACLE, HARVESTER, BASIC 逻辑;
 * 4) 评估函数对多ROOT、SPORER、SPORE都给了较高加分;
 * 5) doTentacleAttack 模拟
 */
public class Agent {

    private static final int SEARCH_DEPTH = 2;   // 保证能看见"下一回合SPORE"的潜在收益
    private static final int ACTIONS_LIMIT = 8; // 每层动作截断

    /**
     * 核心接口：返回在本回合要对每个ROOT做的动作
     */
    public List<Action> getActions(State state, int requiredActionsCount){
        // 找到所有我方 ROOT organ
        List<Entity> myRoots = new ArrayList<>();
        for(Entity e: state.myOrgans){
            if(e.type== Entity.EntityType.ROOT && e.organParentId==0){
                myRoots.add(e);
            }
        }

        // 多 organism => 每个给一条指令
        List<Action> results = new ArrayList<>();
        for(Entity root: myRoots){
            Action best = doMinimaxSearch(state, root, SEARCH_DEPTH);
            results.add(best);
        }
        return results;
    }

    //================== Minimax 顶层 ==================//
    private Action doMinimaxSearch(State state, Entity myRoot, int depth){
        // 生成可能动作
        List<Action> myActions = generateAllPossibleActionsForRoot(state, myRoot, true);

        // 调试：打印动作列表
        System.err.println("=== Possible actions for rootId=" + myRoot.organId + " ===");
        for(Action ac: myActions){
            System.err.println("   -> " + ac);
        }

        if(myActions.isEmpty()){
            return new Action(); // WAIT
        }

        // quick evaluate => sort => limit
        List<ScoredAction> scored = new ArrayList<>();
        for(Action a: myActions){
            State sim = simulate(state, a, true, myRoot);
            int val   = evaluateState(sim);
            scored.add(new ScoredAction(a, val));

            System.err.println("SimAction="+ a +" => Score="+ val);
        }
        scored.sort((a,b)-> Integer.compare(b.score, a.score));
        if(scored.size()>ACTIONS_LIMIT){
            scored = scored.subList(0, ACTIONS_LIMIT);
        }

        // alpha-beta
        Action bestAct = null;
        int bestVal = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE, beta = Integer.MAX_VALUE;

        for(ScoredAction sc: scored){
            State sim = simulate(state, sc.action, true, myRoot);
            int val   = minimaxOpponent(sim, depth-1, alpha, beta);
            if(val > bestVal){
                bestVal = val;
                bestAct = sc.action;
            }
            alpha = Math.max(alpha, bestVal);
            if(alpha >= beta) break;
        }

        System.err.println("=== doMinimaxSearch => BestAct="+ bestAct +" with val="+ bestVal);
        if(bestAct == null){
            bestAct = scored.get(0).action;
        }
        return bestAct;
    }

    private int minimaxOpponent(State st, int depth, int alpha, int beta){
        if(depth <= 0){
            return evaluateState(st);
        }

        List<Action> oppActions = generateAllPossibleOpponentActions(st);
        if(oppActions.isEmpty()){
            return evaluateState(st);
        }

        // 升序(对手想最小化我们分)
        List<ScoredAction> scored = new ArrayList<>();
        for(Action a: oppActions){
            State sim = simulate(st, a, false, null);
            int val   = evaluateState(sim);
            scored.add(new ScoredAction(a, val));
        }
        scored.sort(Comparator.comparingInt(sa-> sa.score));
        if(scored.size()>ACTIONS_LIMIT){
            scored = scored.subList(0, ACTIONS_LIMIT);
        }

        int bestVal = Integer.MAX_VALUE;
        for(ScoredAction sc: scored){
            State sim = simulate(st, sc.action, false, null);
            int val   = minimaxMyTurn(sim, depth-1, alpha, beta);
            if(val < bestVal){
                bestVal = val;
            }
            beta = Math.min(beta, bestVal);
            if(alpha >= beta) break;
        }
        return bestVal;
    }

    private int minimaxMyTurn(State st, int depth, int alpha, int beta){
        if(depth <= 0){
            return evaluateState(st);
        }

        // 简化：只看一个root
        Entity singleRoot = null;
        for(Entity e: st.myOrgans){
            if(e.type== Entity.EntityType.ROOT && e.organParentId==0){
                singleRoot=e;
                break;
            }
        }
        if(singleRoot==null){
            return evaluateState(st);
        }

        List<Action> myActs = generateAllPossibleActionsForRoot(st, singleRoot, true);
        if(myActs.isEmpty()){
            return evaluateState(st);
        }

        // quick evaluate => sort => limit
        List<ScoredAction> scored = new ArrayList<>();
        for(Action a: myActs){
            State sim = simulate(st, a, true, singleRoot);
            int val   = evaluateState(sim);
            scored.add(new ScoredAction(a, val));
        }
        scored.sort((a,b)-> Integer.compare(b.score, a.score));
        if(scored.size()>ACTIONS_LIMIT){
            scored = scored.subList(0, ACTIONS_LIMIT);
        }

        int bestVal = Integer.MIN_VALUE;
        for(ScoredAction sc: scored){
            State sim = simulate(st, sc.action, true, singleRoot);
            int val   = minimaxOpponent(sim, depth-1, alpha, beta);
            if(val > bestVal){
                bestVal= val;
            }
            alpha = Math.max(alpha,bestVal);
            if(alpha>=beta) break;
        }
        return bestVal;
    }

    //================== 动作生成 ==================//
    private List<Action> generateAllPossibleActionsForRoot(State s, Entity root, boolean isSelf){
        List<Action> acts= new ArrayList<>();
        // WAIT
        acts.add(new Action());

        // 找此 root organism
        List<Entity> organs= new ArrayList<>();
        for(Entity e: s.myOrgans){
            if(e.type== Entity.EntityType.ROOT && e.organId== root.organId){
                organs.add(e);
            }
            if(e.organRootId== root.organId){
                organs.add(e);
            }
        }
        for(Entity o: organs){
            acts.addAll(generatePossibleActionsForOrgan(s, o, isSelf));
        }
        return acts;
    }

    private List<Action> generateAllPossibleOpponentActions(State s){
        List<Action> result= new ArrayList<>();
        // WAIT
        result.add(new Action());
        // 对手建 BASIC/TENTACLE (简化)
        for(Entity opp: s.oppOrgans){
            result.addAll(generatePossibleActionsForOrgan(s, opp, false));
        }
        return result;
    }

    /**
     * 关键修正：
     * - 建 SPORER 时，不再过度依赖 hasProteinInLine(...)
     * - 改成 canReachAnyProteinInLine(...) 或更宽松 => 保证第一回合就可构建 SPORER
     */
    private List<Action> generatePossibleActionsForOrgan(State st, Entity organ, boolean isSelf){
        List<Action> result= new ArrayList<>();

        int myA= isSelf? st.myA: st.oppA;
        int myB= isSelf? st.myB: st.oppB;
        int myC= isSelf? st.myC: st.oppC;
        int myD= isSelf? st.myD: st.oppD;

        // SPORE: if organ is SPORER
        if(organ.type== Entity.EntityType.SPORER && myA>=1 && myB>=1 && myC>=1 && myD>=1){
            for(Entity p: st.proteins){
                if(isLineNoBlockAndSameRowOrCol(st, organ.x, organ.y, p.x,p.y)){
                    System.err.println("Generate SPORE => from sporer="+organ.organId
                            +", target=("+p.x+","+p.y+")");
                    result.add(new Action(organ.organId, p.x, p.y));
                }
            }
        }

        // GROW SPORER (B+D)
        if(myB>=1 && myD>=1){
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            char[] ds={'N','E','S','W'};
            for(int i=0;i<4;i++){
                int nx= organ.x+ dd[i][0];
                int ny= organ.y+ dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;

                // 改进：只要该方向能到达蛋白质 => 允许
                if(canReachAnyProteinInLine(st,nx,ny, ds[i])){
                    System.err.println("Generate SPORER => organId="+organ.organId
                            +", at=("+nx+","+ny+"), dir="+ ds[i]);
                    result.add(new Action(organ.organId, nx, ny,
                            Action.ActionType.SPORER, ds[i]));
                }
            }
        }

        // GROW TENTACLE(B+C)
        if(myB>=1 && myC>=1){
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            char[] ds={'N','E','S','W'};
            for(int i=0; i<4; i++){
                int nx= organ.x+ dd[i][0];
                int ny= organ.y+ dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;
                result.add(new Action(organ.organId,nx,ny, Action.ActionType.TENTACLE, ds[i]));
            }
        }

        // GROW HARVESTER(C+D)
        if(myC>=1 && myD>=1){
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            char[] ds={'N','E','S','W'};
            for(int i=0;i<4;i++){
                int nx= organ.x+ dd[i][0];
                int ny= organ.y+ dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;
                result.add(new Action(organ.organId,nx,ny, Action.ActionType.HARVESTER, ds[i]));
            }
        }

        // GROW BASIC(A)
        if(myA>=1){
            int[][] dd={{0,-1},{1,0},{0,1},{-1,0}};
            for(int i=0;i<4;i++){
                int nx= organ.x+ dd[i][0];
                int ny= organ.y+ dd[i][1];
                if(!isCellFree(st,nx,ny)) continue;
                result.add(new Action(organ.organId,nx,ny, Action.ActionType.BASIC,'X'));
            }
        }

        // WAIT
        result.add(new Action());

        return result;
    }

    // 如果想完全放开 => 直接 return true;
    private boolean canReachAnyProteinInLine(State st,int sx,int sy, char dirChar){
        // 改进：若你想更宽松 => return true;
        // 否则做 “从 (sx,sy) 沿dir直到碰到蛋白质 or 墙 or organ”
        Direction d= Direction.fromSymbol(dirChar);
        int[] off= directionToOffset(d);

        int cx= sx, cy= sy;
        while(true){
            cx+= off[0]; cy+= off[1];
            if(st.isOutOfBounds(cx,cy)) return false;
            if(st.isWall(cx,cy)) return false;
            if(st.isMyOrgan(cx,cy) || st.isOppOrgan(cx,cy)) return false;
            if(st.isProteinTile(cx,cy)){
                return true; // 找到蛋白质 => ok
            }
        }
    }

    // SPORE 用的：同列/同行 & 中途无organ/wall => 目标是蛋白质
    private boolean isLineNoBlockAndSameRowOrCol(State st,int sx,int sy,int tx,int ty){
        if(!(sx==tx||sy==ty)) return false;
        if(!st.isProteinTile(tx,ty)) return false;
        int dx= (tx>sx)?1:((tx<sx)?-1:0);
        int dy= (ty>sy)?1:((ty<sy)?-1:0);

        int cx= sx, cy= sy;
        while(true){
            cx+= dx; cy+= dy;
            if(cx==tx && cy==ty) return true;
            if(st.isOutOfBounds(cx,cy)) return false;
            if(st.isWall(cx,cy)) return false;
            if(st.isMyOrgan(cx,cy)|| st.isOppOrgan(cx,cy)) return false;
        }
    }

    private boolean isCellFree(State st,int x,int y){
        if(st.isOutOfBounds(x,y)) return false;
        Entity e= st.grid[y][x];
        if(e==null) return true;
        // 若是蛋白质 => 也可踩
        if(e.type== Entity.EntityType.A|| e.type== Entity.EntityType.B
                || e.type== Entity.EntityType.C|| e.type== Entity.EntityType.D){
            return true;
        }
        return false;
    }

    //================= simulate =================//
    public State simulate(State origin, Action act, boolean isSelf, Entity root){
        State st= new State(origin);

        switch(act.getActionType()){
            case WAIT:
                break;
            case BASIC:
                doGrowBasic(st,act,isSelf,root);
                break;
            case HARVESTER:
                doGrowHarvester(st,act,isSelf,root);
                break;
            case TENTACLE:
                doGrowTentacle(st,act,isSelf,root);
                break;
            case SPORER:
                doGrowSporer(st,act,isSelf,root);
                break;
            case SPORE:
                doSpore(st,act,isSelf);
                break;
        }

        doTentacleAttack(st);
        return st;
    }

    private void doGrowBasic(State s,Action a, boolean isSelf, Entity root){
        if(isSelf) s.myA--; else s.oppA--;
        Entity b= new Entity(a.getX(),a.getY(), Entity.EntityType.BASIC,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), Direction.NONE, a.getOrganId(),
                (root==null)?0: root.organId
        );
        s.addEntityDirect(b);

        // 若踩蛋白质 => +3
        Entity tile= s.grid[a.getY()][a.getX()];
        if(tile!= b && tile!=null && (
                tile.type== Entity.EntityType.A
                        || tile.type== Entity.EntityType.B
                        || tile.type== Entity.EntityType.C
                        || tile.type== Entity.EntityType.D)){
            if(tile.type== Entity.EntityType.A){ if(isSelf) s.myA+=3; else s.oppA+=3;}
            if(tile.type== Entity.EntityType.B){ if(isSelf) s.myB+=3; else s.oppB+=3;}
            if(tile.type== Entity.EntityType.C){ if(isSelf) s.myC+=3; else s.oppC+=3;}
            if(tile.type== Entity.EntityType.D){ if(isSelf) s.myD+=3; else s.oppD+=3;}
            s.removeEntity(tile);
        }
    }

    private void doGrowHarvester(State s,Action a, boolean isSelf, Entity root){
        if(isSelf){ s.myC--; s.myD--; }
        else { s.oppC--; s.oppD--; }

        Direction d= Direction.fromSymbol(a.getDirection());
        Entity h= new Entity(a.getX(),a.getY(), Entity.EntityType.HARVESTER,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), d, a.getOrganId(),
                (root==null)?0:root.organId
        );
        s.addEntityDirect(h);
    }

    private void doGrowTentacle(State s,Action a, boolean isSelf, Entity root){
        if(isSelf){ s.myB--; s.myC--; }
        else { s.oppB--; s.oppC--; }

        Direction d= Direction.fromSymbol(a.getDirection());
        Entity t= new Entity(a.getX(),a.getY(), Entity.EntityType.TENTACLE,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), d, a.getOrganId(),
                (root==null)?0:root.organId
        );
        s.addEntityDirect(t);
    }

    private void doGrowSporer(State s,Action a, boolean isSelf, Entity root){
        // B+D
        if(isSelf){ s.myB--; s.myD--; }
        else { s.oppB--; s.oppD--; }

        Direction d= Direction.fromSymbol(a.getDirection());
        // 如果你想保留一点过滤 => 也可 canReachAnyProteinInLine(...)，
        // 这里就不多写
        Entity sp= new Entity(a.getX(), a.getY(), Entity.EntityType.SPORER,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), d, a.getOrganId(),
                (root==null)?0:root.organId
        );
        s.addEntityDirect(sp);
    }

    private void doSpore(State s, Action a, boolean isSelf){
        // cost(A,B,C,D)
        if(isSelf){ s.myA--; s.myB--; s.myC--; s.myD--; }
        else { s.oppA--; s.oppB--; s.oppC--; s.oppD--; }

        Entity r= new Entity(a.getX(),a.getY(), Entity.EntityType.ROOT,
                isSelf? Entity.Owner.SELF: Entity.Owner.OPPONENT,
                genId(s), Direction.NONE, 0,0
        );
        s.addEntityDirect(r);
    }

    // TENTACLE 攻击模拟
    private void doTentacleAttack(State s){
        // 我方 tent => kill opp
        List<Entity> killOpp= new ArrayList<>();
        for(Entity t: s.myOrgans){
            if(t.type== Entity.EntityType.TENTACLE){
                int[] off= directionToOffset(t.direction);
                int fx= t.x+ off[0], fy= t.y+ off[1];
                if(!s.isOutOfBounds(fx,fy)){
                    Entity e= s.grid[fy][fx];
                    if(e!=null && e.owner== Entity.Owner.OPPONENT){
                        killOrganAndDescendants(s,e,false, killOpp);
                    }
                }
            }
        }

        // 对手 tent => kill me
        List<Entity> killMe= new ArrayList<>();
        for(Entity t: s.oppOrgans){
            if(t.type== Entity.EntityType.TENTACLE){
                int[] off= directionToOffset(t.direction);
                int fx= t.x+ off[0], fy= t.y+ off[1];
                if(!s.isOutOfBounds(fx,fy)){
                    Entity e= s.grid[fy][fx];
                    if(e!=null && e.owner== Entity.Owner.SELF){
                        killOrganAndDescendants(s,e,true, killMe);
                    }
                }
            }
        }

        for(Entity dead: killOpp) s.removeEntity(dead);
        for(Entity dead: killMe)  s.removeEntity(dead);
    }

    private void killOrganAndDescendants(State s, Entity target, boolean isMySide, List<Entity> removal){
        Queue<Entity> Q= new LinkedList<>();
        Q.add(target);
        while(!Q.isEmpty()){
            Entity d= Q.poll();
            if(!removal.contains(d)){
                removal.add(d);
                List<Entity> pool= isMySide? s.myOrgans: s.oppOrgans;
                for(Entity c: pool){
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

        // organ差
        int diff= st.myOrgans.size()- st.oppOrgans.size();
        score += 10* diff;

        // 资源差
        int myRes= st.myA+ st.myB+ st.myC+ st.myD;
        int oppRes= st.oppA+ st.oppB+ st.oppC+ st.oppD;
        score += 2*(myRes- oppRes);

        // root>1 => +80*(rootCount-1)
        int rootCount=0;
        for(Entity e: st.myOrgans){
            if(e.type== Entity.EntityType.ROOT && e.organParentId==0){
                rootCount++;
            }
        }
        if(rootCount>1){
            score += 80*(rootCount-1);
        }

        // sporer => +40, 若能立即 SPORE => +60
        for(Entity e: st.myOrgans){
            if(e.type== Entity.EntityType.SPORER){
                score += 40; // 基本加分
                // 若 A,B,C,D>=1 => 检查 line
                if(st.myA>=1 && st.myB>=1 && st.myC>=1 && st.myD>=1){
                    if(canSporeThisTurn(st,e)){
                        score += 60;
                    }
                }
            }
        }

        // TENTACLE面向对手 => +30
        score += measureTentacleFacing(st);

        // HARVESTER面向蛋白质 => +10
        score += measureHarvesterFacing(st);

        // SPORER距离蛋白质 => BFS => + (20-dist)
        score += measureSporerDistance(st);

        return score;
    }

    private boolean canSporeThisTurn(State st, Entity sp){
        // 遍历蛋白质 => 同行列 & 无阻挡 => yes
        for(Entity p: st.proteins){
            if(isLineNoBlockAndSameRowOrCol(st, sp.x,sp.y, p.x,p.y)){
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
                        val+=30;
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
                        val += 10;
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
                int d= findClosestProteinDist(st, sp.x, sp.y);
                if(d < 99999){
                    val += Math.max(0, 20-d);
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
            int cx= cur[0], cy= cur[1], cd= cur[2];
            for(int[] d: dirs){
                int nx= cx+d[0], ny= cy+d[1];
                if(nx<0||nx>=st.width||ny<0||ny>=st.height) continue;
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

    // 方向 => 偏移
    private int[] directionToOffset(Direction d){
        switch(d){
            case NORTH:return new int[]{0,-1};
            case EAST: return new int[]{1,0};
            case SOUTH:return new int[]{0,1};
            case WEST: return new int[]{-1,0};
            default: return new int[]{0,0};
        }
    }

    private int genId(State st){
        int mx=0;
        for(Entity e: st.allEntities){
            if(e.organId>mx) mx=e.organId;
        }
        return mx+1;
    }

    private static class ScoredAction{
        Action action;
        int score;
        ScoredAction(Action a,int s){
            this.action=a; this.score=s;
        }
    }
}
