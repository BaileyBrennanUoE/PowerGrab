package uk.ac.ed.inf.powergrab;
import java.math.BigDecimal;

public class Position {
    public double latitude;
    public double longitude;
    public double r = 0.0003;
    //constants used to determine width of plane
    public double w2 = r*Math.cos(67.5);
    public double w3 = r*Math.cos(45);
    public double w4 = r*Math.cos(22.5);
    //constants used to determine height of plane
    public double h2 = r*Math.sin(67.5);
    public double h3 = r*Math.sin(45);
    public double h4 = r*Math.sin(22.5);
    
    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public Position nextPosition(Direction direction) {
        if(direction == Direction.N) {
            return new Position(latitude + r, longitude);
        }
        if(direction == Direction.NNE) {
            return new Position(latitude + h2, longitude + w2);
        }
        if(direction == Direction.NE) {
            return new Position(latitude + h3, longitude + w3);
        }
        if(direction == Direction.ENE) {
            return new Position(latitude + h4, longitude + w4);
        }
        if(direction == Direction.E) {
            return new Position(latitude, longitude + r);
        }
        if(direction == Direction.ESE) {
            return new Position(latitude + h2, longitude - w2);
        }
        if(direction == Direction.SE) {
            return new Position(latitude + h3, longitude - w3);
        }
        if(direction == Direction.SSE) {
            return new Position(latitude + h4, longitude - w4);
        }
        if(direction == Direction.S) {
            return new Position(latitude - r, longitude);
        }
        if(direction == Direction.SSW) {
            return new Position(latitude - h2, longitude - w2);
        }
        if(direction == Direction.SW) {
            return new Position(latitude - h3, longitude - w3);
        }
        if(direction == Direction.WSW) {
            return new Position(latitude - h4, longitude - w4);
        }
        if(direction == Direction.W) {
            return new Position(latitude, longitude - r);
        }
        if(direction == Direction.WNW) {
            return new Position(latitude - h2, longitude + w2);
        }
        if(direction == Direction.NW) {
            return new Position(latitude - h3, longitude + w3);
        }
        if(direction == Direction.NNW) {
            return new Position(latitude - h4, longitude + w4);
        }
        return new Position(latitude, longitude);
    }
    
    public boolean inPlayArea() {
        if(latitude >= 55.946233) {
            return false;
        }
        if(latitude <= 55.942617) {
            return false;
        }
        if(longitude >= -3.184319) {
            return false;
        }
        if(longitude <= -3.192473) {
            return false;
        }
        return true;
    }
}
