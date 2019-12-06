package uk.ac.ed.inf.powergrab;
import java.math.BigDecimal;

public class Position {
    //the two points that the define the position on the map
    public double latitude;
    public double longitude;
   
    //BigDecimal is used to ensure precision while carrying out the calculations.
    //constant r defines the set distance each move travels  
    private static final BigDecimal r = new BigDecimal(0.0003);
   
    private double convertToRadians(double angle) {
        return (angle*Math.PI)/180;
    }
   
    //variables for the trigonometric equations
    private BigDecimal cos675 = new BigDecimal(String.format("%.10f", Math.cos(convertToRadians(67.5))));
    private BigDecimal cos45 = new BigDecimal(String.format("%.10f", Math.cos(convertToRadians(45))));
    private BigDecimal cos225 = new BigDecimal(String.format("%.10f", Math.cos(convertToRadians(22.5))));
    private BigDecimal sin675 = new BigDecimal(String.format("%.10f", Math.sin(convertToRadians(67.5))));
    private BigDecimal sin45 = new BigDecimal(String.format("%.10f", Math.sin(convertToRadians(45))));
    private BigDecimal sin225 = new BigDecimal(String.format("%.10f", Math.sin(convertToRadians(22.5))));
   
    //variables used to determine width of plane
    private BigDecimal w2 = r.multiply(cos675);
    private BigDecimal w3 = r.multiply(cos45);
    private BigDecimal w4 = r.multiply(cos225);
    //variables used to determine height of plane
    private BigDecimal h2 = r.multiply(sin675);
    private BigDecimal h3 = r.multiply(sin45);
    private BigDecimal h4 = r.multiply(sin225);
   
    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
   
    public Position nextPosition(Direction direction) {
        //casting the latitude and longitude of the Position to BigDecimal
        BigDecimal lati = new BigDecimal(String.format("%.14f", latitude));
        BigDecimal longi = new BigDecimal(String.format("%.14f", longitude));
       
        //checks the direction the next position is in to determine the calculations needed
        switch(direction) {
            case N: return new Position(lati.add(r).doubleValue(), longitude);
            case NNE: return new Position(lati.add(h2).doubleValue(), longi.add(w2).doubleValue());
            case NE: return new Position(lati.add(h3).doubleValue(), longi.add(w3).doubleValue());
            case ENE: return new Position(lati.add(h4).doubleValue(), longi.add(w4).doubleValue());
            case E: return new Position(latitude, longi.add(r).doubleValue());
            case ESE: return new Position(lati.subtract(h4).doubleValue(), longi.add(w4).doubleValue());
            case SE: return new Position(lati.subtract(h3).doubleValue(), longi.add(w3).doubleValue());
            case SSE: return new Position(lati.subtract(h2).doubleValue(), longi.add(w2).doubleValue());
            case S: return new Position(lati.subtract(r).doubleValue(), longitude);
            case SSW: return new Position(lati.subtract(h2).doubleValue(), longi.subtract(w2).doubleValue());
            case SW: return new Position(lati.subtract(h3).doubleValue(), longi.subtract(w3).doubleValue());
            case WSW: return new Position(lati.subtract(h4).doubleValue(), longi.subtract(w4).doubleValue());
            case W: return new Position(latitude, longi.subtract(r).doubleValue());
            case WNW: return new Position(lati.add(h4).doubleValue(), longi.subtract(w4).doubleValue());
            case NW: return new Position(lati.add(h3).doubleValue(), longi.subtract(w3).doubleValue());
            case NNW: return new Position(lati.add(h2).doubleValue(), longi.subtract(w2).doubleValue());
            default: return new Position(latitude, longitude);
        }
    }
   
    public boolean inPlayArea() {
        //checks to see if the latitude of the position is within the set bounds
        if(latitude >= 55.946233 || latitude <= 55.942617 || 
                longitude >= -3.184319 || longitude <= -3.192473) {
            return false;
        }
        /*
         * if(latitude <= 55.942617) { return false; } //checks to see if the longitude
         * of the position is within the set bounds if(longitude >= -3.184319) { return
         * false; } if(longitude <= -3.192473) { return false; }
         */
        return true;
    }
   
}
