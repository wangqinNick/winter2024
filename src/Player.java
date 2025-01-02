import java.util.*;
import java.util.List;

public class Player {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        int width = in.nextInt();
        int height= in.nextInt();

        Agent agent = new Agent();

        while(true){
            if(!in.hasNextInt()) return;
            int entityCount= in.nextInt();

            State state= new State(width,height);

            for(int i=0;i<entityCount;i++){
                int x= in.nextInt();
                int y= in.nextInt();
                String typeStr= in.next();
                int owner= in.nextInt();
                int organId= in.nextInt();
                char dirChar= in.next().charAt(0);
                int parentId= in.nextInt();
                int rootId= in.nextInt();

                state.addEntity(x,y,typeStr,owner,organId,dirChar,parentId,rootId);
            }

            state.myA= in.nextInt();
            state.myB= in.nextInt();
            state.myC= in.nextInt();
            state.myD= in.nextInt();

            state.oppA= in.nextInt();
            state.oppB= in.nextInt();
            state.oppC= in.nextInt();
            state.oppD= in.nextInt();

            int requiredActionsCount= in.nextInt();

            // 你可以输出地图检查
//             state.debugPrintGrid();

            // 获取动作
            List<Action> actions= agent.getActions(state, requiredActionsCount);

            // 补齐或截断
            while(actions.size()< requiredActionsCount){
                actions.add(new Action()); // WAIT
            }
            if(actions.size()> requiredActionsCount){
                actions= actions.subList(0, requiredActionsCount);
            }

            // 输出
            for(Action a: actions){
                System.out.println(a);
            }
        }
    }
}
