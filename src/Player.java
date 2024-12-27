import java.util.Scanner;

/**
 * Player 类：比赛程序的入口。
 * - 读取输入
 * - 构建 State
 * - 调用 Agent.getAction(...)
 * - 输出指令
 */
public class Player {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // 读取地图宽度与高度
        int width = in.nextInt();
        int height = in.nextInt();

        // 构造一个 Agent
        Agent agent = new Agent();

        while (true) {
            // 每回合读取 entityCount
            int entityCount = in.nextInt();
            State state = new State(width, height);

            // 读取所有实体信息
            for (int i = 0; i < entityCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                String type = in.next();
                int owner = in.nextInt();
                int organId = in.nextInt();
                char organDir = in.next().charAt(0); // N, W, S, E 或 X
                int organParentId = in.nextInt();
                int organRootId = in.nextInt();

                state.addEntity(x, y, type, owner, organId, organDir, organParentId, organRootId);
            }

            // 读取我方蛋白质数量
            state.myA = in.nextInt();
            state.myB = in.nextInt();
            state.myC = in.nextInt();
            state.myD = in.nextInt();

            // 读取对手蛋白质数量
            state.oppA = in.nextInt();
            state.oppB = in.nextInt();
            state.oppC = in.nextInt();
            state.oppD = in.nextInt();

            // 读取 requiredActionsCount (通常为1)
            int requiredActionsCount = in.nextInt();

            // 调试打印(可选)
            System.err.println("=== NEW TURN ===");
            System.err.println("My Proteins: A=" + state.myA + " B=" + state.myB + " C=" + state.myC + " D=" + state.myD);
            System.err.println("Opp Proteins: A=" + state.oppA + " B=" + state.oppB + " C=" + state.oppC + " D=" + state.oppD);

            // 获取行动
            Action action = agent.getAction(state, requiredActionsCount);

            // 输出行动命令
            System.out.println(action);
        }
    }
}
