package uk.ac.ed.inf.powergrab;
import java.net.*;
import java.io.*;
import com.mapbox.geojson.*;

public class App 
{
    public static void main( String[] args ) throws IOException{
    	
        //creates the URL from which the map will be retrieved from
        String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/" + args[2] + "/" + 
                args[1] + "/" + args[0] + "/powergrabmap.geojson";
        URL mapURL = new URL(mapString);
        //start point of the drone
        Position startPoint = new Position(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        //seed for the pseudo-random generator
        Integer pseudoSeed = Integer.parseInt(args[5]);
        //gets the drone type
        String droneType = args[6];
        
        //creates files for the map and moves output
        PrintWriter movesFile = new PrintWriter(droneType + "-" + args[0] + "-" + args[1] + "-" + args[2] + ".txt");
        PrintWriter mapFile = new PrintWriter(droneType + "-" + args[0] + "-" + args[1] + "-" + args[2] + ".geojson");
        
        //downloads the map from
        URLConnection urlConn = mapURL.openConnection();
        HttpURLConnection conn = (HttpURLConnection) urlConn;
        conn.setReadTimeout(10000); // milliseconds
        conn.setConnectTimeout(15000); // milliseconds
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect(); 
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder mapSource = new StringBuilder();
        String line;
        //reads the map
        while ((line = reader.readLine()) != null) {
            mapSource.append(line);
        }
        //creates a Feature Collection from the features read from the map
        FeatureCollection fc = FeatureCollection.fromJson(mapSource.toString());
        
        //creates a new game with the parameters from command line
        Game powerGrab = new Game(droneType, startPoint, pseudoSeed, fc, movesFile, mapFile);
        //runs through the game start to finish
        powerGrab.playGame();
        
    }
    
    
    
}