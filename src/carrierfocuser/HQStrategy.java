package carrierfocuser;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import battlecode.common.Anchor;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.ResourceType;

public class HQStrategy {
    static int adCarrierNum = 0;
    static int mnCarrierNum = 0;

    static final int MAX_CARRIER = 9;

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runHeadquarters(RobotController rc) throws GameActionException {
        if (RobotPlayer.turnCount == 1) {
            Communication.addHeadquarter(rc);
            return;
        } else if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
            RobotPlayer.scanIslands(rc);
            Communication.tryWriteMessages(rc);
            return;
        }

        // Pick a direction to build in.
        MapLocation hqLoc = rc.getLocation();

        Set<MapLocation> locations = new HashSet<MapLocation>(Arrays.asList(rc.getAllLocationsWithinRadiusSquared(hqLoc, RobotType.HEADQUARTERS.actionRadiusSquared)));
        
        locations.removeIf(location -> {
            try {
                return rc.isLocationOccupied(location) || !rc.sensePassability(location);
            } catch (GameActionException e) {
                System.out.println(e);
            }
            return true;
        });

        Iterator<MapLocation> it = locations.iterator();
        //for (int i = 0; i < 5; i++) {
            try {
                MapLocation loc = it.next();
                if ((RobotPlayer.turnCount < 300 || rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 150) && rc.canBuildRobot(RobotType.CARRIER, loc)) {
                    rc.setIndicatorString("Trying to build a resource carrier");
                    rc.buildRobot(RobotType.CARRIER, loc);
                }

                loc = it.next();
                if ((RobotPlayer.turnCount < 300 || rc.getResourceAmount(ResourceType.MANA) >= 160)
                        && rc.canBuildRobot(RobotType.LAUNCHER, loc)) {
                    rc.setIndicatorString("Trying to build a launcher");
                    rc.buildRobot(RobotType.LAUNCHER, loc);
                }
                if (rc.getNumAnchors(Anchor.STANDARD) == 0 && rc.canBuildAnchor(Anchor.STANDARD) && RobotPlayer.turnCount >= 300) {
                    rc.setIndicatorString("Trying to build an anchor");
                    rc.buildAnchor(Anchor.STANDARD);
                }
            } catch (NoSuchElementException e) {
                //break;
            }
        //}
    }
}
