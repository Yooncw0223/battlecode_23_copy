package launcherfocuser;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

enum LauncherType {
    Normal,
    Retreating,
    Formation,
    FollowEnemy,
    Reporting,
    Waiting
}

public class LauncherStrategy {
    static MapLocation hqLoc;
    static MapLocation[] enemyHqLocs;

    static LauncherType type = LauncherType.Normal;
    static LauncherType prevType = LauncherType.Normal;

    // Retreating
    static MapLocation island;
    static final double RETREAT_FRACTION = 0.65;
    static final double RETURN_FRACTION = 0.9;
    static boolean no_islands = false;

    static MapLocation restingPlace;

    // Formation
    static MapLocation destination;
    static MapLocation leaderPosition;
    static int symmetry = 0;

    static boolean surroundingEnemyHq = false;

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
        }
        if (hqLoc == null) {
            scanHQ(rc);
            restingPlace = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
            enemyHqLocs = RobotPlayer.getSymmetricLocs(rc, hqLoc);
        }

        // Try to attack someone
        RobotPlayer.fireAtAnEnemy(rc, true);

        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (type == LauncherType.Retreating) {
            rc.setIndicatorString("Retreating to heal to " + island);
            if (island == null) {
                island = Communication.getClosestControlledIsland(rc);
            }

            if (island != null) {
                RobotPlayer.moveTowards(rc, island);
                if (rc.getHealth() > RobotType.LAUNCHER.getMaxHealth() * RETURN_FRACTION) {
                    type = LauncherType.Normal;
                }
            } else {
                type = LauncherType.Normal;
            }
        }

        switch (type) {
            case Normal:
                rc.setIndicatorString("Normal attacking bot");
                if (RobotPlayer.rng.nextDouble() < 0.9) {

                    
                    RobotPlayer.moveTowards(rc, restingPlace);
                }
                else
                    RobotPlayer.moveRandom(rc);

                if (rc.getLocation().distanceSquaredTo(restingPlace) <= 4)
                    type = LauncherType.Waiting;

                for (RobotInfo enemy : visibleEnemies) {
                    if (enemy.getType() != RobotType.HEADQUARTERS) {
                        type = LauncherType.FollowEnemy;
                    }
                }
                // } else {
                //     MapLocation enemyLocation = enemy.getLocation();
                //     if (rc.canMove(rc.getLocation().directionTo(enemyLocation).opposite()))
                //         rc.move(rc.getLocation().directionTo(enemyLocation).opposite());
                // }

                break;

            case Waiting:
                int lowestId = Integer.MAX_VALUE;
                MapLocation leader = null;
                int robotCount = 1;
                RobotInfo[] visibleAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo info : visibleAllies) {
                    if (info.getType() != RobotType.LAUNCHER)
                        continue;
                    robotCount++;
                    if (info.ID < lowestId) {
                        lowestId = info.ID;
                        leader = info.location;
                    }
                }
                
                if (leader == null) {
                    // you're the leader
                    if (robotCount < 8)
                        break; 
                } else {
                    // follow the leader
                    leaderPosition = leader;
                }
                
                destination = enemyHqLocs[symmetry];
                type = LauncherType.Formation;

                break;

            case Formation:
                rc.setIndicatorString("In formation following leader at " + leaderPosition);
                if (leaderPosition == null) {
                    RobotPlayer.moveTowards(rc, leaderPosition);
                } else {
                    RobotPlayer.moveTowards(rc, destination);
                    if (rc.canSenseLocation(destination)) {
                        if (!rc.canSenseRobotAtLocation(destination)
                                || rc.senseRobotAtLocation(destination).type != RobotType.HEADQUARTERS) {
                            destination = enemyHqLocs[++symmetry];
                        }
                    }  
                }
                break;

            case Retreating: // To sky island to heal
                break;

            case FollowEnemy:
                rc.setIndicatorString("Following enemy robot");
                boolean foundEnemy = false;
                for (RobotInfo enemy : visibleEnemies) {
                    if (enemy.getType() != RobotType.HEADQUARTERS) {
                        MapLocation enemyLocation = enemy.getLocation();
                        RobotPlayer.moveTowards(rc, enemyLocation);
                        foundEnemy = true;
                        break;
                    }

                    // } else {
                    //     MapLocation enemyLocation = enemy.getLocation();
                    //     if (rc.canMove(rc.getLocation().directionTo(enemyLocation).opposite()))
                    //         rc.move(rc.getLocation().directionTo(enemyLocation).opposite());
                    // }
                }
                if (!foundEnemy) {
                    type = LauncherType.Normal;
                }
                break;

            case Reporting:
                rc.setIndicatorString("Reporting vital info");
                island = Communication.getClosestControlledIsland(rc);
                if (island != null && rc.getLocation().distanceSquaredTo(island) <= rc.getLocation().distanceSquaredTo(hqLoc)) {
                    RobotPlayer.moveTowards(rc, island);
                } else {
                    RobotPlayer.moveTowards(rc, hqLoc);
                }
                
                if (Communication.inRangetoWrite(rc))
                    Communication.tryWriteMessages(rc);

                if (!Communication.messagesLeft(rc)) {
                    type = LauncherType.Normal;
                }
                break;

            default:
        }

        RobotPlayer.scanIslands(rc);

        if (type != LauncherType.Formation && Communication.messagesLeft(rc)) {
            type = LauncherType.Reporting;
        }

        if (type != LauncherType.Retreating && rc.getHealth() <= RobotType.LAUNCHER.getMaxHealth() * RETREAT_FRACTION) {
            //prevType = type;
            type = LauncherType.Retreating;
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
}
