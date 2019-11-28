package uk.ac.ed.inf.powergrab;

/* public class Direction {
    //Defines constants for each compass point to be used by the Position class
    public static final Direction N = new Direction();
    public static final Direction NNE = new Direction();    
    public static final Direction NE = new Direction();
    public static final Direction ENE = new Direction();
    public static final Direction E = new Direction();
    public static final Direction ESE = new Direction();
    public static final Direction SE = new Direction();
    public static final Direction SSE = new Direction();
    public static final Direction S = new Direction();
    public static final Direction SSW = new Direction();
    public static final Direction SW = new Direction();
    public static final Direction WSW = new Direction();
    public static final Direction W = new Direction();
    public static final Direction WNW = new Direction();
    public static final Direction NW = new Direction();
    public static final Direction NNW = new Direction();
    
}*/

public enum Direction {
    N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW;
    
    @Override 
    public String toString() {
        switch(this) {
            case N: return "N";
            case NNE: return "NNE";
            case NE: return "NE";
            case ENE: return "ENE";
            case E: return "E";
            case ESE: return "ESE";
            case SE: return "SE";
            case SSE: return "SSE";
            case S: return "S";
            case SSW: return "SSW";
            case SW: return "SW";
            case WSW: return "WSW";
            case W: return "W";
            case WNW: return "WNW";
            case NW: return "NW";
            case NNW: return "NNW";
            default: throw new IllegalArgumentException();
        }
    }
}