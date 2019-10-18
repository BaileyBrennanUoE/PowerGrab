package uk.ac.ed.inf.powergrab;

import java.math.BigDecimal;

public class Drone {
    private String type;
    private double charge;
    private double coins;
    
    public Drone(String droneType) {
        this.type = droneType;
        this.charge = 250.0;
        this.coins = 0.0;
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

    public void updateCoins(BigDecimal stationCoins) {
        BigDecimal preciseCoins = new BigDecimal(String.format("%.14f", coins));
        coins = Math.max(0,(preciseCoins.add(stationCoins)).doubleValue());     //Drone cannot store debt
    }

    public void updateCharge(BigDecimal stationCharge) {
        BigDecimal moveCost = new BigDecimal("1.25");
        BigDecimal preciseCharge = new BigDecimal(String.format("%.14f", charge));
        charge = Math.max(0.0, (preciseCharge.add(stationCharge).subtract(moveCost)).doubleValue());     //Drone cannot store debt
    }
   
}
