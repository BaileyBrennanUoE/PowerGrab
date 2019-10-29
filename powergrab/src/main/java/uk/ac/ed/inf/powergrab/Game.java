package uk.ac.ed.inf.powergrab;
import com.mapbox.geojson.*;
import java.math.*;
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
    private PrintWriter flightPath;
    
    Game(String droneType, Position start, int pseudoSeed, FeatureCollection fc, PrintWriter movesFile, PrintWriter mapFile) {
        this.drone = new Drone(droneType);
        this.currentPos = start;
        this.pseudoSeed = pseudoSeed;
        this.fc = fc;
        fc.features().add(lineMap);
        moves = 0;
        this.movesFile = movesFile;
        this.flightPath = mapFile;
    }
    
    public String getLineMap() {
        Point line = (Point) fc.features().get(49).geometry();
        return line.coordinates().toString();
    }
    
    //updates the game state once the drone has performed a move
    public void makeMove(Direction direction) {
    	Position nextPos = currentPos.nextPosition(direction);
        moves++;    
        updateMap(currentPos, nextPos, fc);
        if(rangeOfStation(currentPos)) {
            updateDrone(drone, nearestStation(currentPos));
        }
        try{
        	movesFile.println(
        			currentPos.latitude + ", " + 
        			currentPos.longitude + ", " + 
        			direction  + ", " + 
        			nextPos.latitude  + ", " + 
        			nextPos.longitude + ", " +
        			drone.getCoins() + ", " +
        			drone.getCharge()
        	);
        } finally {
        	currentPos = nextPos;
        }
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
        double nearestPoint = distanceAway(current, (Point) nearestFeature.geometry());   //finds the distance between the first feature and drone   
        
        //finds the closest feature by finding the minimum distance in the collection
        for(int i = 1; i < 49; i++) {
        	Feature stationI = fc.features().get(i);
            double stationDistance = distanceAway(current, (Point) stationI.geometry()) ;
            if(nearestPoint > stationDistance){
                nearestFeature = stationI;
                nearestPoint = stationDistance;
            }
        }
        return nearestFeature;
    }
    
    //updates the drones charge and coin balance
    public void updateDrone(Drone drone, Feature feature) {
        BigDecimal coins = feature.getProperty("coins").getAsBigDecimal();
        BigDecimal charge = feature.getProperty("charge").getAsBigDecimal();
        BigDecimal droneCoins = new BigDecimal(String.format("%.14f", drone.getCoins()));
        BigDecimal droneCharge = new BigDecimal(String.format("%.14f", drone.getCharge()));
        updateFeature(feature, coins, charge, droneCoins, droneCharge);
        drone.updateCoins(coins);
        drone.updateCharge(charge);
        
    }
    
    //updates the feature's properties after the exchange 
    public void updateFeature(Feature f, BigDecimal coins, BigDecimal charge, BigDecimal droneCoins, BigDecimal droneCharge) {
        if(coins.signum() == -1) {
            coins = new BigDecimal(Math.min(0, coins.add(droneCoins).doubleValue()));
        }
    }
    
    //updates the map to show the move just performed by the drone
    public void updateMap(Position from, Position to, FeatureCollection fc) {
        
    }
    
    //runs the game through till the drone completes 250 moves or runs out of charge
    public void playGame() {
        if(drone.getType() == "stateless") {    //runs the stateless drone
            while(moves != 250 || drone.getCharge() > 1.25) {
                //decide and make moves    
            }
        }
        if(drone.getType() == "stateful") {    //runs the stateful drone
            while(moves != 250 || drone.getCharge() > 1.25) {
                //decide and make moves    
            }
        }
        endGame();
    }
    
    public void endGame() {
        
    }
}
