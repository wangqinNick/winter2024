import java.util.*;
import java.io.*;

/**
 * Player 类是程序的入口，负责读取输入、更新状态，并输出行动命令。
 */
public class Player {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // 读取地图宽度和高度
        int width = in.nextInt();
        int height = in.nextInt();

        Agent agent = new Agent();

        // 游戏循环
        while (true) {
            // 构造本回合的 State
            State state = new State(width, height);

            // 读取实体数量
            int entityCount = in.nextInt();
            for (int i = 0; i < entityCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                String type = in.next();
                int owner = in.nextInt();
                int organId = in.nextInt();
                String organDir = in.next(); // N, W, S, E 或 X
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

            // 读取 requiredActionsCount (本关通常为1)
            int requiredActionsCount = in.nextInt();

            // 获取行动
            Action action = agent.getAction(state, requiredActionsCount);

            // 输出行动命令
            System.out.println(action);
        }
    }
}
