package uk.ac.ed.inf.powergrab;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

public class Drone{
    private Position currentPos;
    private String type;
    private Random rnd;
    private double charge;
    private double coins;
    
    //constructor
    public Drone(Position currentPos, String type, int pseudoSeed){
        this.currentPos = currentPos;
        this.type = type;
        this.rnd = new Random(pseudoSeed);
        this.charge = 250.0;
        this.coins = 0.0;
    }
    
    //getters
    public Position getCurrentPos() {
        return currentPos;
    }
    public String getType() {
        return type;
    }
    
    public double getCharge() {
        return charge;
    }
    
    public double getCoins() {
        return coins;
    }
    
    //setters
    public void updateCurrentPos(Position newPos) {
        this.currentPos = newPos;
    }

    public void updateCoins(BigDecimal stationCoins) {
        BigDecimal preciseCoins = new BigDecimal(String.format("%.14f", coins));
        coins = Math.max(0,(preciseCoins.add(stationCoins)).doubleValue());     //Drone cannot store debt
    }

    public void updateCharge(BigDecimal stationCharge) {
        BigDecimal moveCost = new BigDecimal("1.25");
        BigDecimal preciseCharge = new BigDecimal(String.format("%.14f", charge));
        charge = Math.max(0, (preciseCharge.add(stationCharge).subtract(moveCost)).doubleValue());     //Drone cannot store debt
    }
   
    //Runs the deciding process on where to make the next move for the stateless drone
    public Direction decideMoveStateless(List<Pair> goodStations, List<Pair> badStations, List<Pair> neutralPoints) {
        
        int index = 0;
        Direction d = Direction.N;
        
        if(!goodStations.isEmpty()) {
            index = rnd.nextInt(goodStations.size());
            d = asDirection(goodStations.get(index).getDirection());
        }else if(!neutralPoints.isEmpty()) {
            index = rnd.nextInt(neutralPoints.size());
            d = asDirection(neutralPoints.get(index).getDirection());
        }else {
            index = rnd.nextInt(badStations.size());
            d = asDirection(badStations.get(index).getDirection());
        }
           
        return d;
    }
    
    /*
     * public Direction decideMoveStateful(List<Position> goodStations,
     * List<Position> badStations, List<Position> neutralSpace) {
     * 
     * Direction d = Direction.N;
     * 
     * return d; }
     */
    
    //gets the direction from the corresponding int
    public Direction asDirection(int d) {
        switch(d) {
        case 0: return Direction.N;
        case 1: return Direction.NNE;
        case 2: return Direction.NE;
        case 3: return Direction.ENE;
        case 4: return Direction.E;
        case 5: return Direction.ESE;
        case 6: return Direction.SE;
        case 7: return Direction.SSE;
        case 8: return Direction.S;
        case 9: return Direction.SSW;
        case 10: return Direction.SW;
        case 11: return Direction.WSW;
        case 12: return Direction.W;
        case 13: return Direction.WNW;
        case 14: return Direction.NW;
        case 15: return Direction.NNW;
        default: throw new IllegalArgumentException("Direction can only be one from the 16-point compass rose");
        }
    }
}
