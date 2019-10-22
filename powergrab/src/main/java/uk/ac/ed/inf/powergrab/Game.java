package uk.ac.ed.inf.powergrab;
import com.mapbox.geojson.*;
import java.io.*;

public class Game {
    private Position currentPos;
    private int moves;
    private Drone drone;
    private FeatureCollection fc;
    private Feature lineMap = Feature.fromJson(
            "{"+
            "\"type\""+":"+"\"Feature\""+","+
                "\"properties\""+":"+"{}"+","+
                "\"geometry\""+":"+"{"
                    +"\"type\""+":"+"\"LineString\""+","
                    +"\"coordinates\""+":"+"["+
                        /*
                         * "[ −3.188396, 55.944425 ]" + "," + 
                         *
                         *  "[ −3.1882811949702905, 55.944147836140246 ]"+
                         */
                    "]"+
                "}"+ 
            "}"
            );
    private int pseudoSeed;
    private PrintWriter movesFile;
    private PrintWriter mapFile;
    
    Game(String droneType, Position start, int pseudoSeed, FeatureCollection fc, PrintWriter movesFile, PrintWriter mapFile) {
        this.drone = new Drone(droneType);
        this.currentPos = start;
        this.pseudoSeed = pseudoSeed;
        this.fc = fc;
        fc.features().add(lineMap);
        moves = 0;
    }
    
    public String getLineMap() {
        Point line = (Point) fc.features().get(49).geometry();
        return line.coordinates().toString();
    }
    
    //updates the game state once the drone has performed a move
    public void makeMove(Direction direction) {
        moves++;    
        updateMap(currentPos, currentPos.nextPosition(direction), fc);
        if(rangeOfStation(currentPos)) {
            updateDrone(drone, nearestStation(currentPos));
        }
        
        currentPos = currentPos.nextPosition(direction);
    }
    
    public boolean rangeOfStation(Position p) {
        Point nearest = (Point) nearestStation(p).geometry();
        return distanceAway(p, nearest) <= 0.00025;
    }
    
    //finds the Pythagorean distance between the drone and the station
    public double distanceAway(Position p1, Point p2) {
        double x = Math.pow(p1.latitude - p2.latitude(), 2);
        double y = Math.pow(p1.longitude - p2.longitude(), 2);
        return Math.sqrt(x + y);
    }
    
    //finds the nearest station relative to the drone's current position.
    public Feature nearestStation(Position current) {
        Feature nearestFeature = fc.features().get(0);    //sets the first feature in the collection as the nearest one
        double nearestPoint = distanceAway(current, (Point) fc.features().get(0).geometry());   //finds the distance between the first feature and drone   
        
        //finds the closest feature by finding the minimum distance in the collection
        for(int i = 1; i < 49; i++) {
            double stationDistance = distanceAway(current, (Point) fc.features().get(i).geometry()) ;
            if(nearestPoint > stationDistance){
                nearestFeature = fc.features().get(i);
                nearestPoint = stationDistance;
            }
        }
        return nearestFeature;
    }
    
    //updates the drones charge and coin balance
    public void updateDrone(Drone drone, Feature feature) {
        drone.updateCoins(feature.getProperty("coins").getAsBigDecimal());
        drone.updateCharge(feature.getProperty("charge").getAsBigDecimal());
        updateFeature(feature);
    }
    
    //updates the feature's properties after the exchange 
    public void updateFeature(Feature f) {
        
    }
    
    //updates the map to show the move just performed by the drone
    public void updateMap(Position from, Position to, FeatureCollection fc) {
        
    }
    
    //runs the game through till the drone completes 250 moves or runs out of charge
    public void playGame() {
        if(drone.getType() == "stateless") {    //runs the stateless drone
            while(moves != 250 || drone.getCharge() > 1.25) {
                
            }
        }
        if(drone.getType() == "stateful") {    //runs the stateful drone
            while(moves != 250 || drone.getCharge() > 1.25) {
                
            }
        }
        endGame();
    }
    
    public void endGame() {
        
    }
}
