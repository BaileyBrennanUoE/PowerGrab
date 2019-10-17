package uk.ac.ed.inf.powergrab;
import java.net.*;

public class App 
{
    public static void main( String[] args )
    {
        String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/" + args[2] + "/" + 
                args[1] + "/" + args[0] + "/powergrabmap.geojson";
        geoMap map = new geoMap(new URL(mapString));
        Position startPoint = new Position(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        Integer psuedoSeed = Integer.parseInt(args[5]);
        String droneType = args[6];
        Game powerGrab = new Game(droneType, startPoint);
        
        powerGrab.playGame();
    }
    
    
}
