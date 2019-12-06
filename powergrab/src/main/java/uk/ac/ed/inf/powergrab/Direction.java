package uk.ac.ed.inf.powergrab;

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