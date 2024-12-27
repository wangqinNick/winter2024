import java.util.ArrayList;
import java.util.List;

public class State implements Cloneable {

    public int width,height;
    public Entity[][] grid;

    public int myA,myB,myC,myD;
    public int oppA,oppB,oppC,oppD;

    // 可以加个 turnNumber 如果需要
    // public int turnNumber = 0; // 如果你要记录回合

    public List<Entity> allEntities= new ArrayList<>();
    public List<Entity> myOrgans= new ArrayList<>();
    public List<Entity> oppOrgans= new ArrayList<>();
    public List<Entity> walls= new ArrayList<>();
    public List<Entity> proteins= new ArrayList<>();

    public State(int w,int h){
        width= w; height= h;
        grid= new Entity[h][w];
    }

    public State(State other){
        this.width= other.width;
        this.height= other.height;
        this.grid= new Entity[height][width];

        this.myA= other.myA;
        this.myB= other.myB;
        this.myC= other.myC;
        this.myD= other.myD;

        this.oppA= other.oppA;
        this.oppB= other.oppB;
        this.oppC= other.oppC;
        this.oppD= other.oppD;

        for(int yy=0; yy<height; yy++){
            for(int xx=0; xx<width; xx++){
                grid[yy][xx]= null;
            }
        }

        for(Entity e: other.allEntities){
            Entity c= e.clone();
            allEntities.add(c);

            if(c.type== Entity.EntityType.WALL){
                walls.add(c);
            } else if(c.type== Entity.EntityType.A|| c.type== Entity.EntityType.B
                    || c.type== Entity.EntityType.C|| c.type== Entity.EntityType.D){
                proteins.add(c);
            } else {
                if(c.owner== Entity.Owner.SELF) myOrgans.add(c);
                else if(c.owner== Entity.Owner.OPPONENT) oppOrgans.add(c);
            }
            if(!isOutOfBounds(c.x,c.y)){
                grid[c.y][c.x]= c;
            }
        }
    }

    @Override
    public State clone(){
        return new State(this);
    }

    public void addEntity(int x,int y,String typeStr,int ownerInt,int organId,
                          char dirChar,int parentId,int rootId){
        // owner
        Entity.Owner ow;
        if(ownerInt==1) ow= Entity.Owner.SELF;
        else if(ownerInt==0) ow= Entity.Owner.OPPONENT;
        else ow= Entity.Owner.NONE;

        // type
        Entity.EntityType t;
        try{
            t= Entity.EntityType.valueOf(typeStr);
        }catch(Exception e){
            t= Entity.EntityType.WALL;
        }

        Direction d= Direction.fromSymbol(dirChar);

        Entity ent= new Entity(x,y,t,ow,organId,d,parentId,rootId);
        allEntities.add(ent);

        if(t== Entity.EntityType.WALL){
            walls.add(ent);
        } else if(t== Entity.EntityType.A || t== Entity.EntityType.B
                || t== Entity.EntityType.C || t== Entity.EntityType.D){
            proteins.add(ent);
        } else {
            if(ow== Entity.Owner.SELF) myOrgans.add(ent);
            else if(ow== Entity.Owner.OPPONENT) oppOrgans.add(ent);
        }

        if(!isOutOfBounds(x,y)){
            grid[y][x]= ent;
        }
    }

    public void addEntityDirect(Entity e){
        allEntities.add(e);
        if(e.type== Entity.EntityType.WALL){
            walls.add(e);
        } else if(e.type== Entity.EntityType.A|| e.type== Entity.EntityType.B
                || e.type== Entity.EntityType.C|| e.type== Entity.EntityType.D){
            proteins.add(e);
        } else {
            if(e.owner== Entity.Owner.SELF) myOrgans.add(e);
            else if(e.owner== Entity.Owner.OPPONENT) oppOrgans.add(e);
        }
        if(!isOutOfBounds(e.x,e.y)){
            grid[e.y][e.x]= e;
        }
    }

    public void removeEntity(Entity e){
        allEntities.remove(e);
        if(e.type== Entity.EntityType.WALL) walls.remove(e);
        else if(e.type== Entity.EntityType.A|| e.type== Entity.EntityType.B
                || e.type== Entity.EntityType.C|| e.type== Entity.EntityType.D){
            proteins.remove(e);
        } else {
            if(e.owner== Entity.Owner.SELF) myOrgans.remove(e);
            else if(e.owner== Entity.Owner.OPPONENT) oppOrgans.remove(e);
        }
        if(!isOutOfBounds(e.x,e.y) && grid[e.y][e.x]== e){
            grid[e.y][e.x]= null;
        }
    }

    public boolean isOutOfBounds(int x,int y){
        return (x<0||x>=width||y<0||y>=height);
    }

    public boolean isWall(int x,int y){
        if(isOutOfBounds(x,y)) return false;
        Entity e= grid[y][x];
        return (e!=null && e.type== Entity.EntityType.WALL);
    }

    public boolean isMyOrgan(int x,int y){
        if(isOutOfBounds(x,y)) return false;
        Entity e= grid[y][x];
        if(e==null) return false;
        if(e.owner== Entity.Owner.SELF
                && e.type!= Entity.EntityType.WALL
                && e.type!= Entity.EntityType.A
                && e.type!= Entity.EntityType.B
                && e.type!= Entity.EntityType.C
                && e.type!= Entity.EntityType.D)
            return true;
        return false;
    }

    public boolean isOppOrgan(int x,int y){
        if(isOutOfBounds(x,y)) return false;
        Entity e= grid[y][x];
        if(e==null) return false;
        if(e.owner== Entity.Owner.OPPONENT
                && e.type!= Entity.EntityType.WALL
                && e.type!= Entity.EntityType.A
                && e.type!= Entity.EntityType.B
                && e.type!= Entity.EntityType.C
                && e.type!= Entity.EntityType.D)
            return true;
        return false;
    }

    public boolean isProteinTile(int x,int y){
        if(isOutOfBounds(x,y)) return false;
        Entity e= grid[y][x];
        if(e==null) return false;
        return (e.type== Entity.EntityType.A
                || e.type== Entity.EntityType.B
                || e.type== Entity.EntityType.C
                || e.type== Entity.EntityType.D);
    }

    public void debugPrintGrid(){
        System.err.println("==== DEBUG MAP ====");
        for(int yy=0; yy<height; yy++){
            StringBuilder sb= new StringBuilder();
            for(int xx=0;xx<width; xx++){
                Entity e= grid[yy][xx];
                if(e==null) sb.append('.');
                else sb.append(getSymbol(e));
            }
            System.err.println(sb.toString());
        }
        System.err.println("==== END DEBUG ====");
    }

    private char getSymbol(Entity e){
        switch(e.type){
            case WALL:return '#';
            case ROOT:return (e.owner== Entity.Owner.SELF)? 'R':'r';
            case BASIC:return (e.owner== Entity.Owner.SELF)? 'B':'b';
            case HARVESTER:return (e.owner== Entity.Owner.SELF)? 'H':'h';
            case TENTACLE:return (e.owner== Entity.Owner.SELF)? 'T':'t';
            case SPORER:return (e.owner== Entity.Owner.SELF)? 'S':'s';
            case A:return 'A';
            case B:return 'B';
            case C:return 'C';
            case D:return 'D';
            default:return '?';
        }
    }
}
