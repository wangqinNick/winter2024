public class Entity implements Cloneable {

    public enum EntityType {
        WALL,
        ROOT,
        BASIC,
        HARVESTER,
        TENTACLE,
        SPORER,
        A,B,C,D
    }

    public enum Owner {
        SELF, OPPONENT, NONE
    }

    public int x,y;
    public EntityType type;
    public Owner owner;
    public int organId;
    public Direction direction;
    public int organParentId;
    public int organRootId;

    // 对于 HARVESTER
    public int capacityC, capacityD;

    public Entity(int x,int y, EntityType t, Owner ow,
                  int organId, Direction dir,
                  int pId,int rId){
        this.x= x; this.y= y;
        this.type= t;
        this.owner= ow;
        this.organId= organId;
        this.direction= dir;
        this.organParentId= pId;
        this.organRootId= rId;

        if(t== EntityType.HARVESTER){
            capacityC=1; capacityD=1;
        } else {
            capacityC=0; capacityD=0;
        }
    }

    public Entity(Entity other){
        this.x= other.x;
        this.y= other.y;
        this.type= other.type;
        this.owner= other.owner;
        this.organId= other.organId;
        this.direction= other.direction;
        this.organParentId= other.organParentId;
        this.organRootId= other.organRootId;
        this.capacityC= other.capacityC;
        this.capacityD= other.capacityD;
    }

    @Override
    public Entity clone(){
        return new Entity(this);
    }

    @Override
    public String toString(){
        return String.format("Entity{%s@(%d,%d),owner=%s,id=%d,dir=%s,p=%d,r=%d}",
                type,x,y,owner,organId,direction,organParentId,organRootId);
    }
}
