package uk.ac.ed.inf.powergrab;
import java.math.BigDecimal;

public class Position {
    //the two points that the define the position on the map
    public double latitude;
    public double longitude;
    
    //BigDecimal is used to ensure precision while carrying out the calculations.
    //constant r defines the set distance each move travels   
    public static final BigDecimal r = new BigDecimal(0.0003);
    
    //variables for the trigonometric equations
    public BigDecimal cos675 = new BigDecimal(String.format("%.10f", Math.cos(1.178097)));
    public BigDecimal cos45 = new BigDecimal(String.format("%.10f", Math.cos(0.785398)));
    public BigDecimal cos225 = new BigDecimal(String.format("%.10f", Math.cos(0.3926991)));
    public BigDecimal sin675 = new BigDecimal(String.format("%.10f", Math.sin(1.178097)));
    public BigDecimal sin45 = new BigDecimal(String.format("%.10f", Math.sin(0.785398)));
    public BigDecimal sin225 = new BigDecimal(String.format("%.10f", Math.sin(0.3926991)));
    
    //variables used to determine width of plane
    public BigDecimal w2 = r.multiply(cos675);
    public BigDecimal w3 = r.multiply(cos45);
    public BigDecimal w4 = r.multiply(cos225);
    //variables used to determine height of plane
    public BigDecimal h2 = r.multiply(sin675);
    public BigDecimal h3 = r.multiply(sin45);
    public BigDecimal h4 = r.multiply(sin225);
    
    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public Position nextPosition(Direction direction) {
        //casting the latitude and longitude of the Position to BigDecimal
        BigDecimal lati = new BigDecimal(String.format("%.14f", latitude));
        BigDecimal longi = new BigDecimal(String.format("%.14f", longitude));
        
        //checks the direction the next position is in to determine the calculations needed
        if(direction == Direction.N) {
            return new Position(lati.add(r).doubleValue(), longitude);
        }
        if(direction == Direction.NNE) {
            return new Position(lati.add(h2).doubleValue(), longi.add(w2).doubleValue());
        }
        if(direction == Direction.NE) {
            return new Position(lati.add(h3).doubleValue(), longi.add(w3).doubleValue());
        }
        if(direction == Direction.ENE) {
            return new Position(lati.add(h4).doubleValue(), longi.add(w4).doubleValue());
        }
        if(direction == Direction.E) {
            return new Position(latitude, longi.add(r).doubleValue());
        }
        if(direction == Direction.ESE) {
            return new Position(lati.subtract(h2).doubleValue(), longi.add(w2).doubleValue());
        }
        if(direction == Direction.SE) {
            return new Position(lati.subtract(h3).doubleValue(), longi.add(w3).doubleValue());
        }
        if(direction == Direction.SSE) {
            return new Position(lati.subtract(h4).doubleValue(), longi.add(w4).doubleValue());
        }
        if(direction == Direction.S) {
            return new Position(lati.subtract(r).doubleValue(), longitude);
        }
        if(direction == Direction.SSW) {
            return new Position(lati.subtract(h2).doubleValue(), longi.subtract(w2).doubleValue());
        }
        if(direction == Direction.SW) {
            return new Position(lati.subtract(h3).doubleValue(), longi.subtract(w3).doubleValue());
        }
        if(direction == Direction.WSW) {
            return new Position(lati.subtract(h4).doubleValue(), longi.subtract(w4).doubleValue());
        }
        if(direction == Direction.W) {
            return new Position(latitude, longi.subtract(r).doubleValue());
        }
        if(direction == Direction.WNW) {
            return new Position(lati.add(h2).doubleValue(), longi.subtract(w2).doubleValue());
        }
        if(direction == Direction.NW) {
            return new Position(lati.add(h3).doubleValue(), longi.subtract(w3).doubleValue());
        }
        if(direction == Direction.NNW) {
            return new Position(lati.add(h4).doubleValue(), longi.subtract(w4).doubleValue());
        }
        return new Position(latitude, longitude);
    }
    
    public boolean inPlayArea() {
        //checks to see if the latitude of the position is within the set bounds
        if(latitude >= 55.946233) {
            return false;
        }
        if(latitude <= 55.942617) {
            return false;
        }
        //checks to see if the longitude of the position is within the set bounds
        if(longitude >= -3.184319) {
            return false;
        }
        if(longitude <= -3.192473) {
            return false;
        }
        return true;
    }
    /*
    public static void main(String[] args) {
        
        //Position p0 = new Position(55.944425, -3.188396);
        //Position p1 = p0.nextPosition(Direction.SSE);
        //Position p2 = p1.nextPosition(Direction.S);
        //Position stop = new Position(55.94384783614024,-3.1882811949702905);
        
        //System.out.println(p1.latitude);
        //System.out.println(p1.longitude);
        
        //System.out.println(p2.longitude);
        //System.out.println(p2.latitude);
        
        
        /*
        BigDecimal stopLat = new BigDecimal(String.format("%.14f", stop.latitude));
        BigDecimal p0Lat = new BigDecimal(String.format("%.14f", p0.latitude));
        BigDecimal p1Lat = new BigDecimal(String.format("%.14f", p1.latitude));
        BigDecimal p2Lat = new BigDecimal(String.format("%.14f", p2.latitude));
        BigDecimal difLat = stopLat.subtract(p2Lat); 
        
        BigDecimal stopLong = new BigDecimal(String.format("%.14f", stop.longitude));
        BigDecimal p2Long = new BigDecimal(String.format("%.14f", p2.longitude));
        BigDecimal difLong = stopLong.subtract(p2Long);
        
        System.out.println(difLat.doubleValue());
        System.out.println(difLong.doubleValue());
        
        
        
        BigDecimal cos225 = new BigDecimal(String.format("%.14f", Math.cos(0.3926991)));
        BigDecimal r = new BigDecimal(0.0003);
        
        System.out.println(cos225.doubleValue());
        System.out.println(r.multiply(cos225));
        System.out.println(p0Lat.add(r.multiply(cos225)));
        */
        //BigDecimal p1Long = new BigDecimal(String.format("%.14f", p1.longitude));
        //BigDecimal stopLong = new BigDecimal(String.format("%.14f", stop.longitude));
        //System.out.println(stopLong.subtract(p1Long).doubleValue());
        
    //}
    
    
    
}
