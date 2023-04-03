package launcherfocuser;

import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.WellInfo;
import battlecode.common.Anchor;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;

enum CarrierType {
    StartingAd,
    StartingMn,
    Explorer,
    Anchorer,
    Reporter,
    Attacker,
    Unassigned
}

public class CarrierStrategy {
    static CarrierType type = CarrierType.Unassigned;
    static CarrierType prevType = CarrierType.Unassigned;

    static int prevHealth = RobotType.CARRIER.health;

    static MapLocation hqLoc;

    // Collector
    static MapLocation wellLoc;
    static ResourceType collectorType;
    
    // Anchorer
    static MapLocation islandLoc;
    static int islandId = -1;
    static boolean hasAnchor = false;

    static void runCarrier(RobotController rc) throws GameActionException {
        if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
            return;
        }
        if (hqLoc == null) {
            RobotPlayer.rng.setSeed(rc.getID());
            RobotPlayer.rng.nextBoolean();

            scanHQ(rc);
            
            if (rc.canTakeAnchor(hqLoc, Anchor.STANDARD) && RobotPlayer.rng.nextDouble() < 0.2) {
                type = CarrierType.Anchorer;
                rc.setIndicatorString("Anchorer");
            } else if (RobotPlayer.rng.nextDouble() < 0.5) {
                type = CarrierType.StartingAd;
                collectorType = ResourceType.ADAMANTIUM;
                rc.setIndicatorString("Adamantium");
            } else {
                type = CarrierType.StartingMn;
                collectorType = ResourceType.MANA;
                rc.setIndicatorString("Mana");
            }
        }
        
        switch (type) {
            case Anchorer:
                rc.setIndicatorString("Anchorer, going to " + islandLoc);
                if (!hasAnchor) {
                    if (rc.canTakeAnchor(hqLoc, Anchor.STANDARD)) {
                        rc.takeAnchor(hqLoc, Anchor.STANDARD);
                        hasAnchor = true;
                    } else {
                        RobotPlayer.moveTowards(rc, hqLoc);
                    }
                    break;
                }

                if (islandLoc == null) {
                    for (int i = 1; i <= GameConstants.MAX_NUMBER_ISLANDS; i++) {
                        MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                        if (islandNearestLoc != null
                                && Communication.readTeamHoldingIsland(rc, i) != rc.getTeam()
                                && (islandLoc == null
                                        || islandNearestLoc.distanceSquaredTo(rc.getLocation()) < islandLoc
                                                .distanceSquaredTo(rc.getLocation()))) {
                            islandLoc = islandNearestLoc;
                            islandId = i;
                            //break;
                        }
                    }
                } else {
                    RobotPlayer.moveTowards(rc, islandLoc);
                }

                if (rc.canPlaceAnchor()) {
                    //rc.setIndicatorString("placeable");
                    if (rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) == Team.NEUTRAL) {
                        rc.placeAnchor();
                        RobotPlayer.scanIslands(rc);
                        hasAnchor = false;
                        islandId = -1;
                        islandLoc = null;
                        Communication.tryWriteMessages(rc);
                    } else if (islandId == rc.senseIsland(rc.getLocation())) {
                        islandLoc = null;
                        islandId = -1;
                    }
                }

                break;

            case StartingAd:
            case StartingMn:
                rc.setIndicatorString("economy: " + collectorType);
                if (wellLoc == null) scanWell(rc, collectorType);
            
                if (wellLoc == null) {
                    RobotPlayer.moveRandom(rc);
                    return;
                }

                if (rc.canCollectResource(wellLoc, -1)) {
                    rc.collectResource(wellLoc, -1);
                } else if (getTotalResources(rc) < GameConstants.CARRIER_CAPACITY) {
                    RobotPlayer.moveTowards(rc, wellLoc);
                } else {
                    int amount = rc.getResourceAmount(collectorType);
                    if (rc.canTransferResource(hqLoc, collectorType, amount))
                        rc.transferResource(hqLoc, collectorType, amount);
                    else
                        RobotPlayer.moveTowards(rc, hqLoc);
                }
                break;

            case Reporter:
                rc.setIndicatorString("saw something interesting");
                if (Communication.messagesLeft(rc)) {
                    RobotPlayer.moveTowards(rc, hqLoc);
                    if (Communication.inRangetoWrite(rc))
                        Communication.tryWriteMessages(rc);
                } else {
                    type = prevType;
                } 
                break;

            case Attacker:
                rc.setIndicatorString("hey you hit me!");
                RobotPlayer.fireAtAnEnemy(rc, false);
                type = prevType;
                break;

            case Unassigned:
            default:
                rc.setIndicatorString("what's my purpose?");
                RobotPlayer.moveRandom(rc);
                Communication.tryWriteMessages(rc);
        }

        if (prevHealth != rc.getHealth()) {
            prevHealth = rc.getHealth();
            prevType = type;
            type = CarrierType.Attacker;
        }
    }

    static void scanHQ(RobotController rc) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.HEADQUARTERS) {
                hqLoc = robot.getLocation();
                break;
            }
        }
    }

    static void scanWell(RobotController rc, ResourceType type) throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            if (well.getResourceType() == type) {
                wellLoc = well.getMapLocation();  // Alert hq about location once it brings back resources? (HQ can also time duration)
                break;
            }
        }
    }

    static void depositResource(RobotController rc, ResourceType type) throws GameActionException {
        int amount = rc.getResourceAmount(type);
        if (amount > 0) {
            if (rc.canTransferResource(hqLoc, type, amount))
                rc.transferResource(hqLoc, type, amount);
        }
    }

    static int getTotalResources(RobotController rc) {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) 
            + rc.getResourceAmount(ResourceType.MANA) 
            + rc.getResourceAmount(ResourceType.ELIXIR);
    }
}
