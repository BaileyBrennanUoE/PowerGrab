package uk.ac.ed.inf.powergrab;
import com.google.gson.*;
import com.mapbox.geojson.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.math.BigDecimal;

public class Game {
    private Position startPos;
    private int moves;
    private Drone drone;
    private List<Feature> initialFC = new ArrayList<>();
    private List<Feature> currentFC=new ArrayList<>();
    private List<Point> lineCoords = new ArrayList<>();
    private PrintWriter movesFile;
    private PrintWriter mapFile;
    private Feature nearest;

    Game(String droneType, Position start, int pseudoSeed, FeatureCollection fc, 
            PrintWriter movesFile, PrintWriter mapFile) {
        
        this.drone = new Drone(start, droneType, pseudoSeed);
        this.startPos = start;
        this.initialFC = new ArrayList<Feature>(fc.features());
        this.currentFC = fc.features();
        moves = 0;
        this.movesFile = movesFile;
        this.mapFile = mapFile;
    }

    //updates the game state once the drone has performed a move
    private void makeMove(Direction direction) {
        
        //move has been made, so counter goes up
        moves++;
        //gets current position from drone
        Position currentPos = drone.getCurrentPos();
        //gets next position based on direction passed in
        Position nextPos = currentPos.nextPosition(direction);
        //updates the flight path
        updateMap(nextPos);
        /*
         * checks if the position the drone just 
         * moved to is in reach of a charging station
         */
        if(rangeOfStation(nextPos)) {
            nearest=nearestStation(nextPos);
            updateDrone(drone, nearest);
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
        //updates the drones current position to the position it just moved to
        drone.updateCurrentPos(nextPos);

    }

    //checks to see if the drone is in range of a station
    private boolean rangeOfStation(Position p) {
        //gets nearest station to the position pass in 
        Point nearest = (Point) nearestStation(p).geometry();
        return distanceAway(p, nearest) <= 0.00025;
    }

    //finds the Pythagorean distance between the drone and the station
    private double distanceAway(Position p1, Point p2) {
        double x = Math.pow(p1.latitude - p2.latitude(), 2);
        double y = Math.pow(p1.longitude - p2.longitude(), 2);
        return Math.sqrt(x + y);
    }

    //finds the nearest station relative to the drone's current position.
    private Feature nearestStation(Position current) {
        Feature stationI;
        //sets the first feature in the collection as the nearest one
        Feature nearestFeature = currentFC.get(0);    
        //finds the distance between the first feature and drone
        double nearestPoint = distanceAway(current, (Point) nearestFeature.geometry());      

        //finds the closest feature by finding the minimum distance in the collection
        for(int i = 1; i < 49; i++) {
            stationI = currentFC.get(i);
            double stationDistance = distanceAway(current, (Point) stationI.geometry()) ;
            if(nearestPoint > stationDistance){
                nearestFeature = stationI;
                nearestPoint = stationDistance;
            }
        }
        return nearestFeature;
    }

    //updates the drones charge and coin balance
    private void updateDrone(Drone drone, Feature feature) {
        //gets values for coins and charge from their sources
        BigDecimal coins = feature.getProperty("coins").getAsBigDecimal();
        BigDecimal charge = feature.getProperty("power").getAsBigDecimal();
        BigDecimal droneCoins = new BigDecimal(String.format("%.14f", drone.getCoins()));
        BigDecimal droneCharge = new BigDecimal(String.format("%.14f", drone.getCharge()));
        //updates the charging station the drone just visited
        updateFeature(feature, coins, charge, droneCoins, droneCharge);
        //updates drone's properties
        drone.updateCoins(coins);
        drone.updateCharge(charge);
    }

    //updates the feature's properties after the exchange 
    private void updateFeature(Feature f, BigDecimal coins, BigDecimal charge, 
            BigDecimal droneCoins, BigDecimal droneCharge) {
        
        //gets sign of the coins from the station
        int signum = coins.signum();
        
        /*
         * depending if the station is positive or negative
         * sets the new feature coins
         */
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
        
        /*
         * depending if the station is positive or negative
         * sets the new feature coins
         */
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
        
        //creating a feature to replace the station just visited
        Point coords = (Point) f.geometry();
        String properties = "{ \"id\": " + f.getProperty("id") +
                ", \"coins\": " + coins.toString() +
                ", \"power\": " + charge.toString() + 
                ", \"marker-symbol\": "+ f.getProperty("marker-symbol") +
                ", \"marker-color\": "+ f.getProperty("marker-color") + "}";
        JsonObject props = new JsonParser().parse(properties).getAsJsonObject();
        Feature newF = Feature.fromGeometry(coords, props);
        
        currentFC.remove(f);
        currentFC.add(newF);
    }

    //updates the map to show the move just performed by the drone
    private void updateMap(Position newPos) {
        /*
         * if no other moves have been made, write the start position to 
         * the list of positions visited by the drone
         */
        switch(moves) {
        case 1:
            lineCoords.add(Point.fromLngLat(startPos.longitude, startPos.latitude));
            lineCoords.add(Point.fromLngLat(newPos.longitude, newPos.latitude));
        default:
            lineCoords.add(Point.fromLngLat(newPos.longitude, newPos.latitude));
        }
    }

    //writes the Feature collection to an external geojson file for flight path visualisation
    private void writeMap() {
        //creates fight path feature
        LineString flightPath = LineString.fromLngLats(lineCoords);
        JsonObject props = new JsonParser().parse("{}").getAsJsonObject();
        Feature lineMap = Feature.fromGeometry(flightPath, props);
        //adds flight path feature to the Feature Collection to printed outs
        initialFC.add(lineMap);
        mapFile.println(FeatureCollection.fromFeatures(initialFC).toJson());
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
        //runs the stateless drone
        case "stateless":   
            
            //loop until drone can no longer play the game
            while(moves < 250 && drone.getCharge() > 1.25) {

                currentPos = drone.getCurrentPos();                
                
                //get all possible next moves
                int i = 0;
                while(i < 15) {
                    for(Direction d: Direction.values()) {
                        nextPosition[i] = currentPos.nextPosition(d);
                        i++;
                    }
                }

                /*
                 * loop through possible next positions so that they can be sorted in to good or
                 * bad moves
                 */
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
                
                //make move based on Stateless decision making
                makeMove(drone.decideMoveStateless(pointsNearGood, pointsNearBad, pointsNearNothing));
                
                //clear lists of nearby moves
                pointsNearGood.clear();
                pointsNearBad.clear();
                pointsNearNothing.clear();

            }
            break;
        //runs the stateful drone    
        case "stateful":   

            List<Feature> goodStations=new ArrayList<>();
            List<Feature> badStations=new ArrayList<>();
            List<Feature> rankedGood=new ArrayList<>();
            Direction d = Direction.N;
            Feature f = initialFC.get(0);
            
            //sort all features into positive or negative stations
            for(int i=0;i < initialFC.size(); i++) {
                f = initialFC.get(i);
                int sign = (int) Math.signum(f.getProperty("coins").getAsDouble());

                switch(sign) {
                case 1:
                    goodStations.add(f);
                    break;
                case -1:
                    badStations.add(f);
                    break;
                default: throw new IllegalArgumentException("must have sign");
                }
            }
            
            //rank all positive stations
            while(!goodStations.isEmpty()) {
                Feature bestStation = bestStation(goodStations);
                rankedGood.add(bestStation);
                goodStations.remove(bestStation);
            }

            //loop until drone can no longer play the game
            while(moves < 250 && drone.getCharge() > 1.25) {
                currentPos = drone.getCurrentPos();

                //get all possible next moves
                int i = 0;
                while(i < 15) {
                    for(Direction dir: Direction.values()) { nextPosition[i] =
                            currentPos.nextPosition(dir);
                    i++;
                    }
                }

                /*
                 * loop through possible next positions so that they can be sorted in to good or
                 * bad moves
                 */
                for(int j=0; j < 15; j++) {
                    Position nextPos = nextPosition[j];
                    Pair nextPosPair = new Pair(nextPos, j);
                    if(!nextPos.inPlayArea()) continue;
                    if(rangeOfStation(nextPos)) {
                        Feature nearest = nearestStation(nextPos);
                        int sign = (int)
                                Math.signum(nearest.getProperty("coins").getAsDouble());
                        switch(sign) {
                        case 1: pointsNearGood.add(nextPosPair);
                        break;
                        case -1: pointsNearBad.add(nextPosPair);
                        break;
                        }
                    }else {
                        pointsNearNothing.add(nextPosPair);
                    }
                }

                /*
                 * If the list of ranked positive stations is not empty then head towards the
                 * best station.
                 * If there is no more positive stations left, waste moves by calling Stateless
                 * drone decision making
                 */  
                if(!rankedGood.isEmpty()) {
                    /*
                     * If there happens to be a positive station nearby on the way, detour to it.
                     * If not, find the direction the best station is in and move there.
                     */
                    if(pointsNearGood.isEmpty()) {
                        Feature bestStation=rankedGood.get(0);

                        Position stationLocation= new Position(((Point) (bestStation.geometry())).latitude(),
                                ((Point) (bestStation.geometry())).longitude());

                        d = drone.findDirection(stationLocation);
                        while(!currentPos.nextPosition(d).inPlayArea()) {
                            d=drone.getNewDirection(d);
                        }
                        makeMove(d);
                        if(rangeOfStation(currentPos) && badStations.contains(nearest)) {
                            badStations.remove(nearest);
                        }
                    }else {
                        d = drone.decideMoveStateless(pointsNearGood, pointsNearBad, pointsNearGood);
                        makeMove(d);
                        if(rankedGood.contains(nearest)) {
                            rankedGood.remove(nearest);
                        }
                    } 
                }else {
                    makeMove(drone.decideMoveStateless(pointsNearGood, pointsNearBad, pointsNearNothing));
                }

                //clear the lists of nearby positions
                pointsNearGood.clear();
                pointsNearBad.clear();
                pointsNearNothing.clear();
            }
        }

        endGame();

    }

    //returns the best rated charging station within the list provided
    private Feature bestStation(List<Feature> stations) {
        Feature bestStation=stations.get(0);
        Feature currentStation=bestStation;
        //loops through the list of features looking for the max score station
        for(int i=1;i<stations.size()-1;i++) {
            currentStation=stations.get(i);
            if(rateFeature(currentStation)>rateFeature(bestStation)) {
                bestStation=currentStation;
            }
        }
        return bestStation;
    }

    //rates the feature based on the sum of its coins and power level
    private double rateFeature(Feature f) {
        double coins = f.getProperty("coins").getAsDouble();
        double charge = f.getProperty("power").getAsDouble();
        return coins+charge;
    }

    //closes the output stream to the text file and triggers the map to write
    private void endGame() {
        movesFile.close();
        writeMap();
        System.out.println("Score was: "+drone.getCoins());
    }
}