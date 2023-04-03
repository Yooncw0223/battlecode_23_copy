package launcherfocuser;

import java.util.Comparator;

import battlecode.common.*;

public class PathFinding {

    static Direction currentDirection = null;
    static MapLocation origin = null;
    static double bestLineM;
    static Direction bestDirection;
    static boolean onBestLine = true;
    static final double permittedError = 0.1; // temporary value 

    static MapLocation stored;

    /**
     * This is an implementation of bug2
     * 
     * @param rc Robotcontroller for the current robot
     * @param destination Destination that the robot is trying to reach
     * @throws GameActionException
     */
    public static void findPath(RobotController rc, MapLocation destination) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();

        if (currentLocation.equals(destination)) {
            currentDirection = null;
            origin = null;
            onBestLine = true;
            return;
        }

        if (!rc.isMovementReady()) {
            return;
        }

        int deltaX = destination.x - currentLocation.x;
        int deltaY = destination.y - currentLocation.y;
        double bestLine = (deltaX == 0) ? (deltaY > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY) : deltaY / deltaX;
        //double permittedError = 0.1;

        // start of new path
        if (origin == null) {
            origin = currentLocation;
            bestLineM = bestLine;
            currentDirection = currentLocation.directionTo(destination);
            bestDirection = currentDirection;
        }

        Direction direction = currentLocation.directionTo(destination);

        //rc.setIndicatorString(bestLineM + " " + bestLine);
        // let's check if I am on the best line
        if (Math.abs(direction.dx - bestDirection.dx) + Math.abs(direction.dy - bestDirection.dy) <= 1
                && currentLocation.distanceSquaredTo(destination) <= origin.distanceSquaredTo(destination)) {
            onBestLine = true;
        }
        
        // if (currentLocation.distanceSquaredTo(destination) <= 8) {
        //     onBestLine = true;
        // }

        RobotInfo[] hqInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        MapLocation[] hqLocs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];

        int j = 0;
        for (RobotInfo info : hqInfos) {
            if (info.getType() == RobotType.HEADQUARTERS) {
                hqLocs[j] = info.location;
                j++;
            }
        }

        //if (onBestLine) {
            currentDirection = currentLocation.directionTo(destination);
            if (canMoveAvoidingHq(rc, currentDirection, hqLocs)) {
                rc.move(currentDirection);
                onBestLine = true;
            } else {
                // now there is an obstacle in front that I must go around -> set the boolean false
                onBestLine = false;
            } 
        //}
       
        // If I am here, then I am going around an obstacle; primarily keeping our obstacle to our right
        if (!onBestLine) {
            for (int i = 0; i < 8; i++) {
                if (canMoveAvoidingHq(rc, currentDirection, hqLocs)) {
                    rc.move(currentDirection);
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
    }

    private static boolean canMoveAvoidingHq(RobotController rc, Direction dir, MapLocation[] hqLocs) {
        if (!rc.canMove(dir))
            return false;
        
        MapLocation currentLocation = rc.getLocation();
        MapLocation newLocation = new MapLocation(currentLocation.x + dir.dx, currentLocation.y + dir.dy);

        for (MapLocation loc : hqLocs) {
            if (loc == null)
                continue;
                
            if (currentLocation.distanceSquaredTo(loc) > RobotType.HEADQUARTERS.actionRadiusSquared
                && newLocation.distanceSquaredTo(loc) <= RobotType.HEADQUARTERS.actionRadiusSquared) {
                return false;
            }
        }

        return true;
    }

    // public static void findPathAStar(RobotController rc, MapLocation destination) throws GameActionException {
    //     MapLocation curLoc = rc.getLocation();
    //     Location currentLocation = new Location(curLoc, 0);
    //     Location goal = new Location(destination, 0);
    //     LocationComparator comparator = new LocationComparator(goal);
    //     PriorityQueue<Location> queue = new PriorityQueue<>(comparator);
    //     queue.add(currentLocation);
    //     // this is to track 
    //     HashMap<Location, Location> predecessor = new HashMap<>();
    //     predecessor.put(currentLocation, null);
    //     HashMap<Location, Double> costSoFar = new HashMap<>();
    //     costSoFar.put(currentLocation, 0.0);

    //     // HashSet<Location> goals = new HashSet<>();
        
    //     // // get the nearby 8 cells
    //     // for (int i = 0; i < RobotPlayer.directions.length; i++) {
    //     //     goals.add(new Location(destination.subtract(RobotPlayer.directions[i]), 0));
    //     // }
        
    //     while (!queue.isEmpty()) {
    //         Location current = queue.poll();
    //         // if I am within 1 block of the goal, then I have arrived at the destination

    //         if (current.isAdjacentTo(goal)) 
    //             break;
            
    //         while (!queue.isEmpty()) {
    //             Location neighbor = queue.poll();
    //             while (!current.isAdjacentTo(neighbor)) {
    //                 // if the bot is not adjacent, I have to move toward the location
    //                 current =  
                
    //             }
    //             double newCost = costSoFar.get(current) + 1;
    //             if (!costSoFar.containsKey(neighbor) || newCost < costSoFar.get(neighbor)) {
    //                 costSoFar.put(neighbor, newCost);
    //                 queue.add(neighbor);
    //                 predecessor.put(neighbor, current);
    //             }
    //         }
    //     }
    // }
   
    
}


/**
 * Similar to MapLocation but made to allow the use of comparator, etc without worrying about how MapLocation instances are made and returnd
 */
class Location {

    public final int x;
    public final int y;
    private double cost;

    public Location(int x, int y, double cost) {
        this.x = x;
        this.y = y;
        this.cost = cost;
    }

    public Location(MapLocation loc, double cost) {
        this.x = loc.x;
        this.y = loc.y;
        this.cost = cost;
    }

    public int distanceSquaredTo(Location otherLocation) {
        return (int) (Math.pow(this.x - otherLocation.x, 2) + Math.pow(this.y - otherLocation.y, 2));
    }

    public boolean isAdjacentTo(Location otherLocation) {
        // is adjacent if the other location is within 1 block (diagonally as well)
        return this.distanceSquaredTo(otherLocation) <= 2;
    }

    public double getCost() {
        return cost;
    }

    @Override
    public int hashCode() {
        return this.x * this.y;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other.getClass() != this.getClass())
            return false;
        Location otherLoc = (Location) other;
        return this.x == otherLoc.x && this.y == otherLoc.y;
    }
}

class LocationComparator implements Comparator<Location> {

    private Location destination;
    public LocationComparator(Location destination) {
        this.destination = destination;
    }

    public int compare(Location locationA, Location locationB) {
        int diff1 = locationA.distanceSquaredTo(this.destination);
        int diff2 = locationB.distanceSquaredTo(this.destination);
        int priority1 = (int) (diff1 + locationA.getCost());
        int priority2 = (int) (diff2 + locationB.getCost());

        return (priority1 > priority2) ? 1 : ((priority1 == priority2) ? 0 : -1);
    }

}