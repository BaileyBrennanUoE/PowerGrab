package uk.ac.ed.inf.powergrab;
import com.google.gson.*;
import com.mapbox.geojson.*;
import java.util.ArrayList;
import java.util.List;
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
    public void makeMove(Direction direction, FeatureCollection fc) {
        Position currentPos = drone.getCurrentPos();
        Position nextPos = currentPos.nextPosition(direction);
        //long startTime = System.currentTimeMillis();
        updateMap(nextPos);
        //long endTime = System.currentTimeMillis();
        //updateMapRuntimes.add((int) (endTime- startTime));
        //System.out.println("Total execution time for updateMap "+moves+": " + (endTime-startTime) + "ms");
        //updates the drone if a station is within range
        if(rangeOfStation(nextPos, fc)) {
            //long startTime2 = System.currentTimeMillis();
            updateDrone(drone, nearestStation(nextPos, fc));
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
    public boolean rangeOfStation(Position p, FeatureCollection fc) {
        Point nearest = (Point) nearestStation(p, fc).geometry();
        return distanceAway(p, nearest) <= 0.00025;
    }
    
    //finds the Pythagorean distance between the drone and the station
    public double distanceAway(Position p1, Point p2) {
        double x = Math.pow(p1.latitude - p2.latitude(), 2);
        double y = Math.pow(p1.longitude - p2.longitude(), 2);
        return Math.sqrt(x + y);
    }
    
    //finds the nearest station relative to the drone's current position.
    public Feature nearestStation(Position current, FeatureCollection fc) {
        Feature stationI;
        Feature nearestFeature = fc.features().get(0);    //sets the first feature in the collection as the nearest one
        double nearestPoint = distanceAway(current, (Point) nearestFeature.geometry());   //finds the distance between the first feature and drone   
        
        //finds the closest feature by finding the minimum distance in the collection
        for(int i = 1; i < 49; i++) {
            stationI = currentFC.features().get(i);
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
                    if(rangeOfStation(nextPos, currentFC)) {
                        int sign = (int) Math.signum(nearestStation(nextPos, currentFC).getProperty("coins").getAsDouble());
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
                makeMove(drone.decideMove(pointsNearGood, pointsNearBad, pointsNearNothing), currentFC);
                //long endTime = System.currentTimeMillis();
                //makeMovesRuntimes.add((int) (endTime-startTime));
                //System.out.println("Total execution time for makeMove "+moves+": " + ((int) (endTime-startTime)) + "ms");
                
                pointsNearGood.clear();
                pointsNearBad.clear();
                pointsNearNothing.clear();
                
            }
            
        case "stateful":   //runs the stateful drone
            List<Feature> goodStations1=new ArrayList<>();
            List<Feature> goodStations2=new ArrayList<>();
            List<Feature> goodStations3=new ArrayList<>();
            List<Feature> goodStations4=new ArrayList<>();
            List<Feature> badStations1=new ArrayList<>();
            List<Feature> badStations2=new ArrayList<>();
            List<Feature> badStations3=new ArrayList<>();
            List<Feature> badStations4=new ArrayList<>();
            List<Feature> neutralSpace1=new ArrayList<>();
            List<Feature> neutralSpace2=new ArrayList<>();
            List<Feature> neutralSpace3=new ArrayList<>();
            List<Feature> neutralSpace4=new ArrayList<>();
            List<Feature> totalPosStations=new ArrayList<>();
            
            Feature f = initialFC.features().get(0);
            
            for(int i=0;i < 49; i++) {
                f = initialFC.features().get(i);
                int sign = (int) Math.signum(f.getProperty("coins").getAsDouble());
                
                switch(sign) {
                    case 1:
                        totalPosStations.add(f);
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
            
            System.out.println("In quarter 1 there is "+goodStations1.size()+" good stations, with rating: "+rateQuarter(goodStations1));
            System.out.println("In quarter 2 there is "+goodStations2.size()+" good stations, with rating: "+rateQuarter(goodStations2));
            System.out.println("In quarter 3 there is "+goodStations3.size()+" good stations, with rating: "+rateQuarter(goodStations3));
            System.out.println("In quarter 4 there is "+goodStations4.size()+" good stations, with rating: "+rateQuarter(goodStations4));
            System.out.println("There is "+totalPosStations.size()+" total good stations");
            System.out.println(" -------- ");
            
            int bestQuarter=bestQuarter(goodStations1, goodStations2, goodStations3, goodStations4);
            
            while(moves < 250 && drone.getCharge() > 1.25) {
                currentPos = drone.getCurrentPos();
                
                switch(bestQuarter) { 
                    case 1: 
                        System.out.println("Best quarter is: "+bestQuarter+", with rating: " 
                                +rateQuarter(goodStations4));
                    /*
                     * if(goodStations1.isEmpty()) { bestQuarter=bestQuarter(goodStations1,
                     * goodStations2, goodStations3,goodStations4);
                     * System.out.println("new best quarter: " + bestQuarter +". It was 1."); }
                     */ 
                        break; 
                    case 2: 
                        System.out.println("Best quarter is: "+bestQuarter+", with rating: " 
                                +rateQuarter(goodStations4));
                    /*
                     * if(goodStations2.isEmpty()) { bestQuarter=bestQuarter(goodStations1,
                     * goodStations2, goodStations3, goodStations4);
                     * System.out.println("new best quarter: " + bestQuarter + ". It was 2."); }
                     */
                        break; 
                    case 3:
                        System.out.println("Best quarter is: "+bestQuarter+", with rating: " 
                                +rateQuarter(goodStations4));
                    /*
                     * if(goodStations3.isEmpty()) { bestQuarter=bestQuarter(goodStations1,
                     * goodStations2, goodStations3, goodStations4);
                     * System.out.println("new best quarter: " + bestQuarter + ". It was 3."); }
                     */ 
                        break; 
                    case 4:
                        System.out.println("Best quarter is: "+bestQuarter+", with rating: " 
                                +rateQuarter(goodStations4));
                    /*
                     * if(goodStations4.isEmpty()) { bestQuarter=bestQuarter(goodStations1,
                     * goodStations2, goodStations3,goodStations4);
                     * System.out.println("new best quarter: " + bestQuarter +". It was 4."); }
                     */ 
                        break; 
                }
                
                
                if(!inBestQuarter(bestQuarter, currentPos)) {
                    moveToBestQuarter(bestQuarter, currentPos);
                    System.out.println("Moving towards best quarter");
                    System.out.println("> Move "+moves+" made");
                    //Feature f = nearestStation(currentPos);
                    currentPos = drone.getCurrentPos();
                    if(rangeOfStation(currentPos, currentFC)) {
                        f = nearestStation(currentPos, currentFC);
                    }
                    
                    switch(inQuarter(f, goodStations1, goodStations2, goodStations3, goodStations4)) {
                        case 1:
                            goodStations1.remove(f);
                            System.out.println("Station removed from quarter 1, new quarter rating is: "+rateQuarter(goodStations1));
                            break;
                        case 2:
                            goodStations2.remove(f);
                            System.out.println("Station removed from quarter 2, new quarter rating is: "+rateQuarter(goodStations2));
                            break;
                        case 3:
                            goodStations3.remove(f);
                            System.out.println("Station removed from quarter 3, new quarter rating is: "+rateQuarter(goodStations3));
                            break;
                        case 4:
                            goodStations4.remove(f);
                            System.out.println("Station removed from quarter 4, new quarter rating is: "+rateQuarter(goodStations4));
                            break;
                        default: throw new IllegalArgumentException("Failed to remove");
                    }
                    
                    bestQuarter=bestQuarter(goodStations1, goodStations2, goodStations3, goodStations4);
                    
                    System.out.println(" -------- ");
                    continue;
                }
                
                System.out.println("In best quarter");
                          
                int i = 0;
                
                while(i < 15) {
                    for(Direction d: Direction.values()) {
                        nextPosition[i] = currentPos.nextPosition(d);
                        i++;
                    }
                }
                
                System.out.println("Next positions decided");
                
                for(int j=0; j < 15; j++) {
                    Position nextPos = nextPosition[j];
                    Pair nextPosPair = new Pair(nextPos, j);
                    if(!inBestQuarter(bestQuarter, nextPos)) {
                        continue;
                    }
                    switch(bestQuarter) {
                        case 1:
                            if(rangeOfStation(nextPos, FeatureCollection.fromFeatures(goodStations1))) {
                                int sign = (int) Math.signum(nearestStation(nextPos, FeatureCollection.fromFeatures(goodStations1)).getProperty("coins").getAsDouble());
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
                            break;
                        case 2:
                            if(rangeOfStation(nextPos, FeatureCollection.fromFeatures(goodStations2))) {
                                int sign = (int) Math.signum(nearestStation(nextPos, FeatureCollection.fromFeatures(goodStations2)).getProperty("coins").getAsDouble());
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
                            break;
                        case 3:
                            if(rangeOfStation(nextPos, FeatureCollection.fromFeatures(goodStations3))) {
                                int sign = (int) Math.signum(nearestStation(nextPos, FeatureCollection.fromFeatures(goodStations3)).getProperty("coins").getAsDouble());
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
                            break;
                        case 4:
                            if(rangeOfStation(nextPos, FeatureCollection.fromFeatures(goodStations4))) {
                                int sign = (int) Math.signum(nearestStation(nextPos, FeatureCollection.fromFeatures(goodStations4)).getProperty("coins").getAsDouble());
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
                            break;
                        default: 
                            //System.out.println("Quarter "+bestQuarter+" failed switch");
                            throw new IllegalArgumentException("Next position must be in a quarter");
                    }
                }
                
                System.out.println("Stations decided");
                
                
                switch(bestQuarter) {
                case 1:
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations1));
                    //Position nearest = new Position(((Point) (f.geometry())).latitude(), ((Point) (f.geometry())).longitude());
                    makeMove(drone.decideMove(pointsNearGood, pointsNearBad, pointsNearNothing), FeatureCollection.fromFeatures(goodStations1));//, nearest));
                    //currentPos = drone.getCurrentPos();
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations1));
                case 2:
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations2));
                    //Position nearest = new Position(((Point) (f.geometry())).latitude(), ((Point) (f.geometry())).longitude());
                    makeMove(drone.decideMove(pointsNearGood, pointsNearBad, pointsNearNothing), FeatureCollection.fromFeatures(goodStations2));//, nearest));
                    //currentPos = drone.getCurrentPos();
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations2));
                case 3:
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations3));
                    //Position nearest = new Position(((Point) (f.geometry())).latitude(), ((Point) (f.geometry())).longitude());
                    makeMove(drone.decideMove(pointsNearGood, pointsNearBad, pointsNearNothing), FeatureCollection.fromFeatures(goodStations3));//, nearest));
                    //currentPos = drone.getCurrentPos();
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations3));
                case 4:
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations4));
                    //Position nearest = new Position(((Point) (f.geometry())).latitude(), ((Point) (f.geometry())).longitude());
                    makeMove(drone.decideMove(pointsNearGood, pointsNearBad, pointsNearNothing), FeatureCollection.fromFeatures(goodStations4));//, nearest));
                    //currentPos = drone.getCurrentPos();
                    //f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations4));
                }
                System.out.println("> Move "+moves+" made");
                
                //int quarter = inQuarter(f, goodStations1, goodStations2, goodStations3, goodStations4);
                System.out.println("Feature is in quarter: "+bestQuarter);
                switch(bestQuarter) {
                        case 1:
                            if(rangeOfStation(currentPos, FeatureCollection.fromFeatures(goodStations1))) {
                                f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations1));
                                goodStations1.remove(f);
                                System.out.println("Station removed from quarter 1, new quarter rating is: "+rateQuarter(goodStations1));
                            } 
                            break;
                        case 2:
                            if(rangeOfStation(currentPos, FeatureCollection.fromFeatures(goodStations2))) {
                                f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations2));
                                goodStations2.remove(f);
                                System.out.println("Station removed from quarter 2, new quarter rating is: "+rateQuarter(goodStations2));
                            }
                            break;
                        case 3:
                            if(rangeOfStation(currentPos, FeatureCollection.fromFeatures(goodStations3))) {
                                f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations3));
                                goodStations3.remove(f);
                                System.out.println("Station removed from quarter 3, new quarter rating is: "+rateQuarter(goodStations3));
                            }
                            break;
                        case 4:
                            if(rangeOfStation(currentPos, FeatureCollection.fromFeatures(goodStations4))) {
                                f = nearestStation(currentPos, FeatureCollection.fromFeatures(goodStations4));
                                goodStations4.remove(f);
                                System.out.println("Station removed from quarter 4, new quarter rating is: "+rateQuarter(goodStations4));
                            }
                            break;
                        default: throw new IllegalArgumentException("Failed to remove");
                }
                    
                System.out.println("Onto next move");
                
                bestQuarter=bestQuarter(goodStations1, goodStations2, goodStations3, goodStations4);
                
                pointsNearGood.clear();
                pointsNearBad.clear();
                pointsNearNothing.clear();
                
                System.out.println(" -------- ");
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
        if(fPos.longitude >= -3.192473 && fPos.longitude <= -3.188396
                && fPos.latitude <= 55.946233 && fPos.latitude >= 55.944425) 
            return 1;
        if(fPos.longitude > -3.188396 && fPos.longitude <= -3.184319
                && fPos.latitude <= 55.946233 && fPos.latitude >= 55.944425) 
            return 2;
        if(fPos.longitude >= -3.192473 && fPos.longitude <= -3.188396
                && fPos.latitude < 55.944425 && fPos.latitude >= 55.942617) 
            return 3;
        if(fPos.longitude > -3.188396 && fPos.longitude <= -3.184319
                && fPos.latitude < 55.944425 && fPos.latitude >= 55.942617) 
            return 4;
        throw new IllegalArgumentException("Station must be within map boundaries");
    }
    
    public int bestQuarter(List<Feature> quarter1, List<Feature> quarter2, List<Feature> quarter3, List<Feature> quarter4){
        int bestQuarter=0;
        double q1rating=rateQuarter(quarter1);
        //System.out.println("q1rating: " + q1rating);
        double q2rating=rateQuarter(quarter2);
        //System.out.println("q2rating: " + q2rating);
        double q3rating=rateQuarter(quarter3);
        //System.out.println("q3rating: " + q3rating);
        double q4rating=rateQuarter(quarter4);
        //System.out.println("q4rating: " + q4rating);
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
        if(quarter.isEmpty()) return 0;
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
                if(fPos.longitude >= -3.192473 && fPos.longitude <= -3.188396
                    && fPos.latitude <= 55.946233 && fPos.latitude >= 55.944425) 
                    return true;
            case 2:
                if(fPos.longitude > -3.188396 && fPos.longitude <= -3.184319
                        && fPos.latitude <= 55.946233 && fPos.latitude >= 55.944425) 
                    return true;
            case 3:   
                if(fPos.longitude >= -3.192473 && fPos.longitude <= -3.188396
                        && fPos.latitude < 55.944425 && fPos.latitude >= 55.942617) 
                    return true;
            case 4:
                if(fPos.longitude > -3.188396 && fPos.longitude <= -3.184319
                    && fPos.latitude < 55.944425 && fPos.latitude >= 55.942617) 
                    return true;
            default: return false;
        }
    }
    
    public void moveToBestQuarter(int bestQuarter, Position currentPos) {
        switch(bestQuarter) {
            case 1:
                if(currentPos.nextPosition(Direction.NW).inPlayArea()) { 
                    makeMove(Direction.NW, currentFC);
                }else if(currentPos.nextPosition(Direction.N).inPlayArea()){ 
                    makeMove(Direction.N, currentFC);
                }else {
                    makeMove(Direction.W, currentFC);
                }
            case 2:
                if(currentPos.nextPosition(Direction.NE).inPlayArea()) { 
                    makeMove(Direction.NE, currentFC);
                }else if(currentPos.nextPosition(Direction.N).inPlayArea()){ 
                    makeMove(Direction.N, currentFC);
                }else {
                    makeMove(Direction.E, currentFC);
                }
            case 3:
                if(currentPos.nextPosition(Direction.SW).inPlayArea()) { 
                    makeMove(Direction.SW, currentFC);
                }else if(currentPos.nextPosition(Direction.S).inPlayArea()){ 
                    makeMove(Direction.S, currentFC);
                }else {
                    makeMove(Direction.W, currentFC);
                }
            case 4:
                if(currentPos.nextPosition(Direction.SE).inPlayArea()) { 
                    makeMove(Direction.SE, currentFC);
                }else if(currentPos.nextPosition(Direction.S).inPlayArea()){ 
                    makeMove(Direction.S, currentFC);
                }else {
                    makeMove(Direction.E, currentFC);
                }
        }
    }
    
    public int inQuarter(Feature f, List<Feature> q1, List<Feature> q2, List<Feature> q3, List<Feature> q4) {
        if(q1.contains(f)) return 1;
        if(q2.contains(f)) return 2;
        if(q3.contains(f)) return 3;
        if(q4.contains(f)) return 4;
        throw new IllegalArgumentException("Feature must be in a quarter");
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
