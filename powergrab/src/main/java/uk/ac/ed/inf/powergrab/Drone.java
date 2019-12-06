package uk.ac.ed.inf.powergrab;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

public class Drone {
    private Position currentPos;
    private String type;
    private Random rnd;
    private double charge;
    private double coins;

    // constructor
    public Drone(Position currentPos, String type, int pseudoSeed) {
        this.currentPos = currentPos;
        this.type = type;
        this.rnd = new Random(pseudoSeed);
        this.charge = 250.0;
        this.coins = 0.0;
    }

    // getters
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

    // setters
    public void updateCurrentPos(Position newPos) {
        this.currentPos = newPos;
    }

    public void updateCoins(BigDecimal stationCoins) {
        BigDecimal preciseCoins = new BigDecimal(String.format("%.14f", coins));
        coins = Math.max(0, (preciseCoins.add(stationCoins)).doubleValue()); // Drone cannot store debt
    }

    public void updateCharge(BigDecimal stationCharge) {
        BigDecimal moveCost = new BigDecimal("1.25");
        BigDecimal preciseCharge = new BigDecimal(String.format("%.14f", charge));
        // drone cannot store debt so take max to ensure it does not drop below zero
        charge = Math.max(0, (preciseCharge.add(stationCharge).subtract(moveCost)).doubleValue());
    }

    // Runs the deciding process on where to make the next move for the Stateless
    // drone
    public Direction decideMoveStateless(List<Pair> goodStations, List<Pair> badStations, List<Pair> neutralPoints) {

        int index = 0;
        Direction d = Direction.N;

        /*
         * If there are positive station(s) in reach choose between one of them at
         * random, if not then choose between any empty spaces available. If there is no
         * other possible moves then choose between negative stations at random.
         */
        if (!goodStations.isEmpty()) {
            index = rnd.nextInt(goodStations.size());
            d = asDirection(goodStations.get(index).getDirection());
        } else if (!neutralPoints.isEmpty()) {
            index = rnd.nextInt(neutralPoints.size());
            d = asDirection(neutralPoints.get(index).getDirection());
        } else {
            index = rnd.nextInt(badStations.size());
            d = asDirection(badStations.get(index).getDirection());
        }

        return d;
    }

    // finds the direction from the drone's current position to that of the best stations position
    // by comparing latitudes and longitudes
    public Direction findDirection(Position nearestStation) {
        Direction d = Direction.N;

        if (currentPos.latitude < nearestStation.latitude && currentPos.longitude < nearestStation.longitude) {
            d = Direction.NE;
        }
        if (currentPos.latitude < nearestStation.latitude && currentPos.longitude > nearestStation.longitude) {
            d = Direction.NW;
        }
        if (currentPos.latitude < nearestStation.latitude && currentPos.longitude == nearestStation.longitude) {
            d = Direction.N;
        }
        if (currentPos.latitude > nearestStation.latitude && currentPos.longitude < nearestStation.longitude) {
            d = Direction.SE;
        }
        if (currentPos.latitude > nearestStation.latitude && currentPos.longitude > nearestStation.longitude) {
            d = Direction.SW;
        }
        if (currentPos.latitude > nearestStation.latitude && currentPos.longitude == nearestStation.longitude) {
            d = Direction.S;
        }
        if (currentPos.latitude == nearestStation.latitude && currentPos.longitude < nearestStation.longitude) {
            d = Direction.E;
        }
        if (currentPos.latitude == nearestStation.latitude && currentPos.longitude > nearestStation.longitude) {
            d = Direction.E;
        }

        return d;
    }
    
    //gets a new direction for when the drone is about to go out of bounds
    public Direction getNewDirection(Direction d) {
        /*
         * Depending on which boundary the drone is trying to cross
         * change the direction to one that does not leave the map
         * but still goes in the relatively correct direction to 
         * the best station.
         */
        //case for drone trying to break Northern boundary
        if(currentPos.nextPosition(d).latitude >= 55.946233)  {
            switch(d) {
            case N:
                d=Direction.E;
                break;
            case NNE:
                d=Direction.E;
                break;
            case NE:
                d=Direction.E;
                break;
            case ENE:
                d=Direction.E;
                break;
            case WNW:
                d=Direction.W;
                break;
            case NW:
                d=Direction.W;
                break;
            case NNW:
                d=Direction.W;
                break;
            default: throw new IllegalArgumentException();
            }
        }
        //case for drone trying to break Southern boundary
        if(currentPos.nextPosition(d).latitude <= 55.942617)  {
            switch(d) {
            case S:
                d=Direction.E;
                break;
            case SSE:
                d=Direction.E;
                break;
            case SE:
                d=Direction.E;
                break;
            case ESE:
                d=Direction.E;
                break;
            case WSW:
                d=Direction.W;
                break;
            case SW:
                d=Direction.W;
                break;
            case SSW:
                d=Direction.W;
                break;
            default: throw new IllegalArgumentException();
            }
        }
        //case for drone trying to break Eastern boundary
        if(currentPos.nextPosition(d).longitude >= -3.184319)  {
            switch(d) {
            case E:
                d=Direction.N;
                break;
            case NNE:
                d=Direction.N;
                break;
            case NE:
                d=Direction.N;
                break;
            case ENE:
                d=Direction.N;
                break;
            case SSE:
                d=Direction.S;
                break;
            case SE:
                d=Direction.S;
                break;
            case ESE:
                d=Direction.S;
                break;
            default: throw new IllegalArgumentException();
            }
        }
        
        //case for drone trying to break Western boundary
        if(currentPos.nextPosition(d).longitude <= -3.192473)  {
            switch(d) {
            case W:
                d=Direction.N;
                break;
            case NNW:
                d=Direction.N;
                break;
            case NW:
                d=Direction.N;
                break;
            case WNW:
                d=Direction.N;
            case SSW:
                d=Direction.S;
                break;
            case WSW:
                d=Direction.S;
                break;
            case SW:
                d=Direction.S;
                break;
            default: throw new IllegalArgumentException();
            }
        }

        return d;
    }

    // gets the direction from the corresponding int
    public Direction asDirection(int d) {
        switch (d) {
        case 0:
            return Direction.N;
        case 1:
            return Direction.NNE;
        case 2:
            return Direction.NE;
        case 3:
            return Direction.ENE;
        case 4:
            return Direction.E;
        case 5:
            return Direction.ESE;
        case 6:
            return Direction.SE;
        case 7:
            return Direction.SSE;
        case 8:
            return Direction.S;
        case 9:
            return Direction.SSW;
        case 10:
            return Direction.SW;
        case 11:
            return Direction.WSW;
        case 12:
            return Direction.W;
        case 13:
            return Direction.WNW;
        case 14:
            return Direction.NW;
        case 15:
            return Direction.NNW;
        default:
            throw new IllegalArgumentException("Direction can only be one from the 16-point compass rose");
        }
    }
}