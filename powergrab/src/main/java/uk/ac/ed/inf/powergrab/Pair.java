package uk.ac.ed.inf.powergrab;

public class Pair{
    private final Position nextPos;
    private final int direction;
    
    public Pair(Position nextPos, int direction) {
        this.nextPos = nextPos;
        this.direction = direction;
    }
    
    public Position getPos() {
        return nextPos;
    }
    
    public int getDirection() {
        return direction;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof Pair)) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        return (this.getPos() == ((Pair) obj).getPos()) 
                && (this.getDirection() == ((Pair) obj).getDirection());
    }
}