import java.util.*;
import java.io.*;
import java.math.*;

class Player {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // 读取地图宽高
        int width = in.nextInt();
        int height = in.nextInt();

        // 创建 Agent
        Agent agent = new Agent();

        // 游戏循环
        while (true) {
            // 初始化 / 更新当前回合的State
            State state = new State(width, height);

            // 读取实体数
            int entityCount = in.nextInt();
            for (int i = 0; i < entityCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                String type = in.next();
                int owner = in.nextInt();
                int organId = in.nextInt();
                String organDir = in.next();
                int organParentId = in.nextInt();
                int organRootId = in.nextInt();

                // 将实体信息存储到 state
                state.addEntity(x, y, type, owner, organId, organDir, organParentId, organRootId);
            }

            // 读取我方与对手的蛋白质数量
            state.myA = in.nextInt();
            state.myB = in.nextInt();
            state.myC = in.nextInt();
            state.myD = in.nextInt();
            state.oppA = in.nextInt();
            state.oppB = in.nextInt();
            state.oppC = in.nextInt();
            state.oppD = in.nextInt();

            // 这里的 requiredActionsCount 在你给的“新规则”定义下：生长1次需要多少蛋白质
            // 不再是“本回合需要输出多少条指令”
            int requiredActionsCount = in.nextInt();

            // 每回合只能生长一次，故只需要 1 条命令
            // 在你当前描述下，我们就直接获取 1 条动作
            String command = agent.getAction(state, requiredActionsCount);

            // 输出动作
            System.out.println(command);
        }
    }
}
