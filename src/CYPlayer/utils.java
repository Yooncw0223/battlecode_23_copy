package CYPlayer;

import battlecode.common.*;

public class utils {
    public static double distance(MapLocation a, MapLocation b) {
        int aX = a.x, aY = a.y;
        int bX = b.x, bY = b.y;
        
        return Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2));
    }
    
}
