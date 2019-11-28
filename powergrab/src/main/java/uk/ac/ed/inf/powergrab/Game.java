package uk.ac.ed.inf.powergrab;
import com.google.gson.*;
import com.mapbox.geojson.*;
import com.mapbox.geojson.LineString;
import java.util.ArrayList;
import java.util.List;
import java.math.*;
import java.io.*;
import java.math.BigDecimal;

public class Game {
    private Position startPos;
    //private Position currentPos;
    private int moves;
    private Drone drone;
    private FeatureCollection initialFC;
    private FeatureCollection currentFC;
    private List<Point> lineCoords = new ArrayList<>();
    private PrintWriter movesFile;
    private PrintWriter mapFile;
    
    /*
    private List<Integer> makeMovesRuntimes = new ArrayList<>();
    private List<Integer> updateMapRuntimes = new ArrayList<>();
    private List<Integer> updateDroneRuntimes = new ArrayList<>();
    private List<Integer> updateFeatureRuntimes = new ArrayList<>();
    */
    
    Game(String droneType, Position start, int pseudoSeed, FeatureCollection fc, PrintWriter movesFile, PrintWriter mapFile) {
        this.drone = new Drone(start, droneType, pseudoSeed);
        this.startPos = start;
        this.initialFC = fc;
        this.currentFC = FeatureCollection.fromFeatures(fc.features());
        moves = 0;
        this.movesFile = movesFile;
        this.mapFile = mapFile;
    }
    
    public String getLineMap() {
        LineString flightPath = LineString.fromLngLats(lineCoords);
        JsonObject props = new JsonParser().parse("{}").getAsJsonObject();
        Feature lineMap = Feature.fromGeometry(flightPath, props);
        return lineMap.toJson();
    }
    
    //updates the game state once the drone has performed a move
    public void makeMove(Direction direction) {
        Position currentPos = drone.getCurrentPos();
    	Position nextPos = currentPos.nextPosition(direction);
    	//long startTime = System.currentTimeMillis();
    	updateMap(nextPos);
        //long endTime = System.currentTimeMillis();
        //updateMapRuntimes.add((int) (endTime- startTime));
        //System.out.println("Total execution time for updateMap "+moves+": " + (endTime-startTime) + "ms");
        //updates the drone if a station is within range
        if(rangeOfStation(nextPos)) {
            //long startTime2 = System.currentTimeMillis();
            updateDrone(drone, nearestStation(nextPos));
            //long endTime2 = System.currentTimeMillis();
            //updateDroneRuntimes.add((int) (endTime2-startTime2));
            //System.out.println("Total execution time for updateDrone "+moves+": " + (endTime2-startTime2) + "ms");
        }
        //prints the required information to an external text file
        movesFile.println(
            currentPos.latitude + ", " + 
        	currentPos.longitude + ", " + 
        	direction.toString() + ", " + 
        	nextPos.latitude  + ", " + 
        	nextPos.longitude + ", " +
        	drone.getCoins() + ", " +
        	drone.getCharge() 
        );
        drone.updateCurrentPos(nextPos);
        moves++;
    }
    
    //checks to see if the drone is in range of a station
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
        Feature nearestFeature = currentFC.features().get(0);    //sets the first feature in the collection as the nearest one
        double nearestPoint = distanceAway(current, (Point) nearestFeature.geometry());   //finds the distance between the first feature and drone   
        
        //finds the closest feature by finding the minimum distance in the collection
        for(int i = 1; i < 49; i++) {
        	Feature stationI = currentFC.features().get(i);
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
        BigDecimal charge = feature.getProperty("power").getAsBigDecimal();
        BigDecimal droneCoins = new BigDecimal(String.format("%.14f", drone.getCoins()));
        BigDecimal droneCharge = new BigDecimal(String.format("%.14f", drone.getCharge()));
        //long startTime = System.currentTimeMillis();
        updateFeature(feature, coins, charge, droneCoins, droneCharge);
        //long endTime = System.currentTimeMillis();
        //updateFeatureRuntimes.add((int) (endTime-startTime));
        //System.out.println("Total execution time for updateFeature "+moves+": " + (endTime-startTime) + "ms"); 
        drone.updateCoins(coins);
        drone.updateCharge(charge);
    }
    
    //updates the feature's properties after the exchange 
    public void updateFeature(Feature f, BigDecimal coins, BigDecimal charge, BigDecimal droneCoins, BigDecimal droneCharge) {
        int signum = coins.signum();
        
        switch(signum) {
            case -1: 
                coins = new BigDecimal(Math.min(0, coins.add(droneCoins).doubleValue()));
                break;
            case 0:
                break;
            case 1: 
                coins = new BigDecimal(0);
                break;
            default: throw new IllegalArgumentException("Coins has to be a signed doubled");
        }
        
        signum = charge.signum();
        switch(signum) {
            case -1: 
                charge = new BigDecimal(Math.min(0, charge.add(droneCharge).doubleValue()));
                break;
            case 0:
                break;
            case 1: 
                charge = new BigDecimal(0);
                break;
            default: throw new IllegalArgumentException("Charge has to be a signed doubled");
        }
        
        Point coords = (Point) f.geometry();
        String properties = "{ \"id\": " + f.getProperty("id") +
                ", \"coins\": " + coins.toString() +
                ", \"power\": " + charge.toString() + 
                ", \"marker-symbol\": "+ f.getProperty("marker-symbol") +
                ", \"marker-color\": "+ f.getProperty("marker-color") + "}";
        JsonObject props = new JsonParser().parse(properties).getAsJsonObject();
        Feature newF = Feature.fromGeometry(coords, props);
        
        currentFC.features().remove(f);
        currentFC.features().add(newF);
    }
    
    //updates the map to show the move just performed by the drone
    public void updateMap(Position newPos) {
        switch(moves) {
            case 1:
                lineCoords.add(Point.fromLngLat(startPos.longitude, startPos.latitude));
                lineCoords.add(Point.fromLngLat(newPos.longitude, newPos.latitude));
            default:
                lineCoords.add(Point.fromLngLat(newPos.longitude, newPos.latitude));
        }
    }
    
    //writes the Feature collection to an external geojson file for flight path visualisation
    public void writeMap() {
        LineString flightPath = LineString.fromLngLats(lineCoords);
        JsonObject props = new JsonParser().parse("{}").getAsJsonObject();
        Feature lineMap = Feature.fromGeometry(flightPath, props);
        initialFC.features().add(lineMap);
        mapFile.println(initialFC.toJson());
        mapFile.close();
    }
    
    //runs the game through till the drone completes 250 moves or runs out of charge
    public void playGame() {
        
        List<Pair> pointsNearGood = new ArrayList<>();
        List<Pair> pointsNearBad = new ArrayList<>();
        List<Pair> pointsNearNothing = new ArrayList<>();
        List<Feature> goodStations1 = new ArrayList<>();
        List<Feature> goodStations2 = new ArrayList<>();
        List<Feature> goodStations3 = new ArrayList<>();
        List<Feature> goodStations4 = new ArrayList<>();
        List<Feature> badStations1 = new ArrayList<>();
        List<Feature> badStations2 = new ArrayList<>();
        List<Feature> badStations3 = new ArrayList<>();
        List<Feature> badStations4 = new ArrayList<>();
        List<Feature> neutralSpace1 = new ArrayList<>();
        List<Feature> neutralSpace2 = new ArrayList<>();
        List<Feature> neutralSpace3 = new ArrayList<>();
        List<Feature> neutralSpace4 = new ArrayList<>();
        
        Position currentPos;
        Position[] nextPosition = new Position[16];
        
        switch(drone.getType()) {
        case "stateless":   //runs the stateless drone
            
            while(moves < 250 && drone.getCharge() > 1.25) {
                
                currentPos = drone.getCurrentPos();                
                int i = 0;
                
                while(i < 15) {
                    for(Direction d: Direction.values()) {
                        nextPosition[i] = currentPos.nextPosition(d);
                        i++;
                    }
                }
                
                for(int j=0; j < 15; j++) {
                    Position nextPos = nextPosition[j];
                    Pair nextPosPair = new Pair(nextPos, j);
                    if(!nextPos.inPlayArea()) continue;
                    if(rangeOfStation(nextPos)) {
                        int sign = (int) Math.signum(nearestStation(nextPos).getProperty("coins").getAsDouble());
                        switch(sign) {
                            case 1: 
                                pointsNearGood.add(nextPosPair);
                                break;
                            case -1:
                                pointsNearBad.add(nextPosPair);
                                break;
                        }
                    }else{
                        pointsNearNothing.add(nextPosPair);
                    }
                }
                //decide and make moves
                //long startTime = System.currentTimeMillis();
                makeMove(drone.decideMoveStateless(pointsNearGood, pointsNearBad, pointsNearNothing));
                //long endTime = System.currentTimeMillis();
                //makeMovesRuntimes.add((int) (endTime-startTime));
                //System.out.println("Total execution time for makeMove "+moves+": " + ((int) (endTime-startTime)) + "ms");
                
                pointsNearGood.clear();
                pointsNearBad.clear();
                pointsNearNothing.clear();
                
            }
            
        case "stateful":   //runs the stateful drone
            for(int i=0;i < 49; i++) {
                Feature f = initialFC.features().get(i);
                int sign = (int) Math.signum(f.getProperty("coins").getAsDouble());
                
                switch(sign) {
                    case 1:
                        switch(decideQuarter(f)) {
                            case 1: goodStations1.add(f);
                                break;
                            case 2: goodStations2.add(f);
                                break;
                            case 3: goodStations3.add(f);
                                break;
                            case 4: goodStations4.add(f);
                                break;
                            default: throw new IllegalArgumentException("Point must belong to a quarter");
                        }
                        break;
                    case -1:
                        switch(decideQuarter(f)) {
                            case 1: badStations1.add(f);
                                break;
                            case 2: badStations2.add(f);
                                break;
                            case 3: badStations3.add(f);
                                break;
                            case 4: badStations4.add(f);
                                break;
                            default: throw new IllegalArgumentException("Point must belong to a quarter");
                        }
                        break;
                    default: 
                        switch(decideQuarter(f)) {
                            case 1: neutralSpace1.add(f);
                                break;
                            case 2: neutralSpace2.add(f);
                                break;
                            case 3: neutralSpace3.add(f);
                                break;
                            case 4: neutralSpace4.add(f);
                                break;
                            default: throw new IllegalArgumentException("Point must belong to a quarter");
                        } 
                }                    
            }
            
            while(moves < 250 && drone.getCharge() > 1.25) {
                currentPos = drone.getCurrentPos();
                int bestQuarter=bestQuarter(goodStations1, goodStations2, goodStations3, goodStations4);
                
                if(!inBestQuarter(bestQuarter, currentPos)) {
                    
                }
                //switch(bestQuarter(goodStations1, goodStations2, goodStations3, goodStations4))                
                makeMove(drone.decideMoveStateless(pointsNearGood, pointsNearBad, pointsNearNothing));
            }
        }
        //int sum = makeMovesRuntimes.stream().mapToInt(Integer::intValue).sum();
        //System.out.println("Average total execution time for makeMove: " + (sum/250) + "ms");  
        
        //long startTime = System.currentTimeMillis();
        endGame();
        //long endTime = System.currentTimeMillis();
        //System.out.println("Total execution time for endGame: " + (endTime-startTime) + "ms");
        
    }
    
    public int decideQuarter(Feature f) {
        Position fPos = new Position(((Point) (f.geometry())).latitude(), ((Point) (f.geometry())).longitude());
        if(fPos.latitude >= -3.192473 && fPos.latitude <= -3.188396
                && fPos.longitude <= 55.946233 && fPos.longitude >= 55.944425) 
            return 1;
        if(fPos.latitude > -3.188396 && fPos.latitude <= -3.184319
                && fPos.longitude <= 55.946233 && fPos.longitude >= 55.944425) 
            return 2;
        if(fPos.latitude >= -3.192473 && fPos.latitude <= -3.188396
                && fPos.longitude < 55.944425 && fPos.longitude >= 55.942617) 
            return 3;
        if(fPos.latitude > -3.188396 && fPos.latitude <= -3.184319
                && fPos.longitude < 55.944425 && fPos.longitude >= 55.942617) 
            return 4;
        throw new IllegalArgumentException("Point must be within map boundaries");
    }
    
    public int bestQuarter(List<Feature> quarter1, List<Feature> quarter2, List<Feature> quarter3, List<Feature> quarter4){
        int bestQuarter=0;
        double q1rating=rateQuarter(quarter1);
        double q2rating=rateQuarter(quarter2);
        double q3rating=rateQuarter(quarter3);
        double q4rating=rateQuarter(quarter4);
        double best12;
        double best34;
        double bestRated;
        
        best12=Math.max(q1rating, q2rating);
        best34=Math.max(q3rating, q4rating);
        bestRated=Math.max(best12, best34);
        
        if(bestRated==q1rating) bestQuarter=1;
        if(bestRated==q2rating) bestQuarter=2;
        if(bestRated==q3rating) bestQuarter=3;
        if(bestRated==q4rating) bestQuarter=4;
        
        return bestQuarter;
    }
    
    public double rateQuarter(List<Feature> quarter) {
        double sum = 0;
        for(int i=0; i<quarter.size(); i++) {
            sum+=quarter.get(i).getProperty("coins").getAsDouble();
            sum+=quarter.get(i).getProperty("power").getAsDouble();
        }
        return sum;
    }
    
    public boolean inBestQuarter(int bestQuarter, Position fPos) {
        switch(bestQuarter) {
            case 1:
                if(fPos.latitude >= -3.192473 && fPos.latitude <= -3.188396
                    && fPos.longitude <= 55.946233 && fPos.longitude >= 55.944425) 
                return true;
            case 2:
                if(fPos.latitude > -3.188396 && fPos.latitude <= -3.184319
                        && fPos.longitude <= 55.946233 && fPos.longitude >= 55.944425) 
                    return true;
            case 3:   
                if(fPos.latitude >= -3.192473 && fPos.latitude <= -3.188396
                        && fPos.longitude < 55.944425 && fPos.longitude >= 55.942617) 
                    return true;
            case 4:
                if(fPos.latitude > -3.188396 && fPos.latitude <= -3.184319
                    && fPos.longitude < 55.944425 && fPos.longitude >= 55.942617) 
                return true;
            default: throw new IllegalArgumentException("Drone has to be with in the map");
        }
    }
    
    //closes the output stream to the text file, triggers the map to write and displays player score
    public void endGame() {
        movesFile.close();
        /*
        int sum1 = updateDroneRuntimes.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Average total execution time for updateDrone: " + (sum1/250) + "ms");
        
        int sum2 = updateFeatureRuntimes.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Average total execution time for updateFeature: " + (sum2/250) + "ms");
        
        int sum3 = updateMapRuntimes.stream().mapToInt(Integer::intValue).sum();
        System.out.println("Average total execution time for updateMap: " + (sum3/250) + "ms");
        */
        //long startTime = System.currentTimeMillis();
        writeMap();
        //long endTime = System.currentTimeMillis();
        //System.out.println("Total execution time for writMap: " + (endTime-startTime) + "ms");
        
        System.out.println("Game over!");
        System.out.println("You've used up all your moves or charge");
        System.out.println("Your final score is " + drone.getCoins());
        
    }
}
