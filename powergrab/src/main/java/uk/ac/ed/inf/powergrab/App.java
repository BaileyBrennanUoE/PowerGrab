package uk.ac.ed.inf.powergrab;
import java.net.*;
import java.io.*;
import com.mapbox.geojson.*;

public class App 
{
    public static void main( String[] args ) throws IOException{
        
        String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/" + args[2] + "/" + 
                args[1] + "/" + args[0] + "/powergrabmap.geojson";
        URL mapURL = new URL(mapString);    //creates URL from where the map will be downloaded from 
        Position startPoint = new Position(Double.parseDouble(args[3]), Double.parseDouble(args[4]));   //start point of the drone
        Integer pseudoSeed = Integer.parseInt(args[5]);     //seed for the pseudo-random generator
        String droneType = args[6];
        
        PrintWriter movesFile = new PrintWriter(droneType + "-" + args[0] + "-" + args[1] + "-" + args[2] + ".txt");
        PrintWriter mapFile = new PrintWriter(droneType + "-" + args[0] + "-" + args[1] + "-" + args[2] + ".geojson");
        
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
        while ((line = reader.readLine()) != null) {
            mapSource.append(line);
        }
        FeatureCollection fc = FeatureCollection.fromJson(mapSource.toString());
        
        Game powerGrab = new Game(droneType, startPoint, pseudoSeed, fc, movesFile, mapFile);
        long startTime = System.currentTimeMillis();
        powerGrab.playGame();
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time for game: " + (endTime-startTime) + "ms");
        
        //System.out.println(fc.features().get(0).getProperty("id"));
    }
    
    
    
}
