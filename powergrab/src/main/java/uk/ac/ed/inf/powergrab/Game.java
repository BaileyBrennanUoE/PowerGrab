package uk.ac.ed.inf.powergrab;
import com.mapbox.geojson.*;

public class Game {
    private Position start;
    private Position currentPos;
    private int moves;
    private Drone drone;
    private FeatureCollection fc;
    private int pseudoSeed;
    
    Game(String droneType, Position start, int pseudoSeed, FeatureCollection fc) {
        this.drone = new Drone(droneType);
        this.start = start;
        this.currentPos = start;
        this.pseudoSeed = pseudoSeed;
        this.fc = fc;
        moves = 0;
    }
    
    public void makeMove(Direction direction) {
        moves++;
        updateMap(currentPos, currentPos.nextPosition(direction), fc);
        currentPos = currentPos.nextPosition(direction);
        if(rangeOfStation(currentPos)) {
            updateDrone(drone, nearestStation(currentPos));
        }
    }
    
    public boolean rangeOfStation(Position p) {
        return true;
    }
    
    public void updateDrone(Drone drone, Feature feature) {
        drone.updateCoins(feature.getProperty("coins").getAsBigDecimal());
        drone.updateCharge(feature.getProperty("charge").getAsBigDecimal());
    }
    
    public Feature nearestStation(Position current) {
        
        
        return fc.features().get(0);
    }
    
    public void updateMap(Position from, Position to, FeatureCollection fc) {
        
    }
    
    public void playGame() {
        if(drone.getType() == "stateless") {
            while(moves != 250 || drone.getCharge() > 1.25) {
                
            }
        }
        if(drone.getType() == "stateful") {
            while(moves != 250 || drone.getCharge() > 1.25) {
                
            }
        }
        endGame();
    }
    
    public void endGame() {
        
    }
}
