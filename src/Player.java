import java.util.*;

public class Player {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // 读取地图宽度与高度
        int width = in.nextInt();
        int height = in.nextInt();

        // 构造一个 Agent
        Agent agent = new Agent();

        // 游戏循环
        while (true) {
            // 读取实体数量
            if (!in.hasNextInt()) {
                // 没有更多输入，退出
                return;
            }
            int entityCount = in.nextInt();

            // 构造本回合的 State
            State state = new State(width, height);

            // 读取实体信息
            for (int i = 0; i < entityCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                String type = in.next();
                int owner = in.nextInt();
                int organId = in.nextInt();
                char organDir = in.next().charAt(0); // N, W, S, E, X
                int organParentId = in.nextInt();
                int organRootId = in.nextInt();

                state.addEntity(x, y, type, owner, organId, organDir, organParentId, organRootId);
            }

            // 读取我方蛋白质
            state.myA = in.nextInt();
            state.myB = in.nextInt();
            state.myC = in.nextInt();
            state.myD = in.nextInt();

            // 读取对手蛋白质
            state.oppA = in.nextInt();
            state.oppB = in.nextInt();
            state.oppC = in.nextInt();
            state.oppD = in.nextInt();

            // requiredActionsCount 等于当前我方总ROOT数（通常）
            int requiredActionsCount = in.nextInt();

            // 调用 Agent
            List<Action> actions = agent.getActions(state, requiredActionsCount);

            // 确保输出 exactly requiredActionsCount 条指令
            // 若 actions.size() < requiredActionsCount，用 WAIT 填充
            while (actions.size() < requiredActionsCount) {
                actions.add(new Action()); // WAIT
            }
            // 若多余，则截断
            actions = actions.subList(0, requiredActionsCount);

            // 逐行打印
            for (Action act : actions) {
                System.out.println(act);
            }
        }
    }
}
