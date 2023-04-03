package donothing;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.ArrayList;


import battlecode.common.*;

public class PathFinding {

    static Direction currentDirection = null;
    static MapLocation origin = null;
    static double bestLineM; 
    static boolean onBestLine = true;
    static final double permittedError = 0.1; // temporary value 


    /**
     * This is an implementation of bug2
     * 
     * @param rc Robotcontroller for the current robot
     * @param destination Destination that the robot is trying to reach
     * @throws GameActionException
     */
    public static boolean findPath(RobotController rc, MapLocation destination) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();

        if (currentLocation.equals(destination)) {
            currentDirection = null;
            origin = null;
            onBestLine = true;
            return true;
        }

        if (!rc.isActionReady()) {
            return false;
        }

        int deltaX = destination.x - currentLocation.x;
        int deltaY = destination.y - currentLocation.x;
        double bestLine = (deltaX == 0) ? (deltaY > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY) : deltaY / deltaX;
        double permittedError = 0.1;

        // start of new path
        if (origin == null) {
            origin = currentLocation;
            bestLineM = bestLine;          
            currentDirection = currentLocation.directionTo(destination);
        }

        // let's check if I am on the best line
        if (Math.abs(bestLineM - bestLine) < permittedError && currentLocation.distanceSquaredTo(destination) <= origin.distanceSquaredTo(destination)) {
            onBestLine = true;
        }

        if (onBestLine) {
            currentDirection = currentLocation.directionTo(destination);
            if (rc.canMove(currentDirection)) {
                rc.move(currentDirection);
            } else {
                // now there is an obstacle in front that I must go around -> set the boolean false
                onBestLine = false;
            } 
        }
       
        // If I am here, then I am going around an obstacle; primarily keeping our obstacle to our right
        if (!onBestLine) {
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
        return false;
    }

    /**
     * Moves the bot correpsonding to @param{rc} to the @param{destination} using A* algorithm.
     *
     * @param rc The robot controller corresponding to the robot.
     * @param destination The destination that the robot is headed to.
     */
    public static void findPathAStar(RobotController rc, MapLocation destination) throws GameActionException {
        // Location is a wrapper class that implements Comparator interface to allow usage of custom priority metric in our priority queue (which is needed for A*)
        Location currentLocation = new Location(rc.getLocation(), 0);
        Location goal = new Location(destination, 0);
        PriorityQueue<Location> queue = new PriorityQueue<>(new LocationComparator(goal));
        queue.add(currentLocation);
        // This is similar to the path algorithm typically stored in Dijkstra's algorithm (dictionary)
        // e.g., predecessor.get(locationA) => DirectionA such that [ LocationA.move(DirectionA yields the node that led to it ]
        HashMap<Location, Direction> predecessor = new HashMap<>();
        predecessor.put(currentLocation, null);
        HashMap<Location, Double> costSoFar = new HashMap<>();
        costSoFar.put(currentLocation, 0.0);
        while (!queue.isEmpty()) {
            // get the most "best" option avaialable to explore
            Location current = queue.poll();

            // the next "best" option from above could be at the opposite side of the map, when the bot is not there
            // so we physically have to move the bot first
            if (!rc.getLocation().equals(current.getLocation())) {
                // TLDR: you can imagine all the paths the bot has been as a tree-like structure
                // the place the bot needs to be and the place that it is at now can be thought of as two leaves (maybe of different branches)
                // so I have to find the most recent intersection of the two branches
                Location traceback = current;
                // TODO: I don't need to use the set here to store information; there is a way to do it in O(1) space, but I'll leave this as a place for improvement for now
                HashSet<Location> history = new HashSet<>();
                history.add(traceback);
                boolean performBugNav = false;
                
                // let's first trace back from where the robot "SHOULD" be at the moment and use the set to check later
                while (predecessor.get(traceback) != null) { // going down to the root
                    traceback = traceback.add(predecessor.get(traceback));
                    // this if-statement means a cycle, which shouldn't happen hopefully, but in that case, let's run our bugnav for now
                    if (history.contains(traceback)) {
                        performBugNav = true;
                        break;
                    }
                    history.add(traceback);
                }

                // if the algorithm is acting weird like mentioned above, let's performa bugnav and terminate the current function afterwards
                if (performBugNav) {
                    PathFinding.findPath(rc, current.getLocation());
                    break;
                }
                
                // now let's trace back from our bot's current physical location to the intersection
                traceback = new Location(rc.getLocation(), 0);

                while (predecessor.get(traceback) != null || !history.contains(traceback)) {
                    traceback = traceback.add(predecessor.get(traceback));
                    Direction dir = predecessor.get(traceback);
                    if (!rc.canMove(dir)) {
                        Clock.yield();
                    }
                    if (rc.canMove(dir)) {
                        rc.move(predecessor.get(traceback));    
                    }
                }

                // now we are at the intersection, we need to go up the first branch to the supposed start location
                // first, we have to retrieve our directions
                Location point = current;
                ArrayList<Direction> nav = new ArrayList<Direction>();
                while (point != traceback) {
                    Direction dir = predecessor.get(traceback);
                    nav.add(dir);
                    traceback = traceback.add(dir); // moving back to the branching point
                }

                // now let's have the bot actually move "upstream"
                
                for (int i = nav.size() - 1; i >= 0; i--) {
                    Direction dir = nav.get(i).opposite();
                    if (!rc.canMove(dir)) {
                        Clock.yield();
                    }
                    rc.move(dir);
                }
            }

            // now the bot is physically at the location
            if (current.equals(goal))  // then I am there
                break;

            // if not, let's scan our neighbor and put them in the queue for future
            for (int i = 0; i < 8; i++) {
                Direction dir = RobotPlayer.directions[i];
                Location neighbor = current.add(dir);
                
                if (neighbor == null) {
                    break;
                }

                // IMPORTANT: the heurstic function is the key of this algorithm
                // if heuristic function is not used, then this algorithm is just equivalent Dijkstra's algorithm
                double newCost = costSoFar.get(current) + neighbor.distanceSquaredTo(current);
                if (!costSoFar.containsKey(neighbor) || newCost < costSoFar.get(neighbor)) {
                    costSoFar.put(neighbor, newCost);
                    queue.add(neighbor);
                    predecessor.put(neighbor, dir.opposite()); // tracking the opposite direction
                }
            }
        }
    } 


    private static Location dest = null;
    private static PriorityQueue<Location> queue = null;
    private static HashMap<Location, Direction> predecessor = null;
    private static HashMap<Location, Double> costSoFar = null;

    private static HashSet<Location> history = null;

    private static int progress = 0;
    private static ArrayList<Direction> nav = null;

    private static Location traceback = null;
    private static int progressIndex = -1;

    private static Location last = null;

    private static boolean broken = false;
    /**
     * Moves the bot correpsonding to @param{rc} to the @param{destination} using A* algorithm.
     *
     * @param rc The robot controller corresponding to the robot.
     * @param destination The destination that the robot is headed to.
     */
    public static boolean move(RobotController rc, MapLocation destination) throws GameActionException {
        // Location is a wrapper class that implements Comparator interface to allow usage of custom priority metric in our priority queue (which is needed for A*)
        
        // at the beginning

        System.out.println(queue);
        
        Location currentLocation = new Location(rc.getLocation(), Double.POSITIVE_INFINITY);
        boolean reset = dest == null || !dest.getLocation().equals(destination);
        if (reset) {
            System.out.println("resetting");
            dest = new Location(destination, 0);
            queue = new PriorityQueue<>(new LocationComparator(dest));
            predecessor = new HashMap<>();
            costSoFar = new HashMap<>();
            
            queue.add(currentLocation);
            predecessor.put(dest, null);
            costSoFar.put(currentLocation, 0.0);

            history = new HashSet<>();
            progress = 0;

            traceback = null;
            progressIndex = -1;

            last = null;
        }

        // then we are in progress; from this point, I expect the variables to be intact
        Location current = (last == null) ? queue.poll() : last;
        if (progress == 0) {
            System.out.println("hello I am at progress 0");

            // if the priority location is not equal to the physical location of bot
            if (!currentLocation.equals(current)) {
                traceback = current;

                history = new HashSet<>();
                history.add(traceback);
                boolean performBugNav = false;
                
                // let's first trace back from where the robot "SHOULD" be at the moment and use the set to check later
                while (predecessor.get(traceback) != null) { // going down to the root
                    traceback = traceback.add(predecessor.get(traceback));
                    // this if-statement means a cycle, which shouldn't happen hopefully, but in that case, let's run our bugnav for now
                    if (history.contains(traceback)) {
                        System.out.println("reached here in traceback");
                        performBugNav = true;
                        break;
                    }
                    history.add(traceback);
                }

                // if the algorithm is acting weird like mentioned above, let's performa bugnav and terminate the current function afterwards
                if (performBugNav) {
                    return PathFinding.findPath(rc, current.getLocation());
                }
         
            } 

            progress = 1; 
        }
 
        if (progress == 1) {
            traceback = currentLocation;
            System.out.println("hello I am at progress 1");
            if (predecessor.get(traceback) != null && !history.contains(traceback)) {

                Direction dir = predecessor.get(traceback);

                System.out.println("direction: " + dir);
                if (!rc.canMove(dir)) {
                    last = current;
                    return false;
                }

                rc.move(dir);    
            } else {
                progress = 2;
            }
        }

        if (progress == 2) {
            System.out.println("hello I am at progress 2");
            // now we are at the intersection, we need to go up the first branch to the supposed start location
            // first, we have to retrieve our directions
            Location point = current;
            nav = new ArrayList<Direction>();
            while (point != traceback) {
                System.out.println("here");
                Direction dir = predecessor.get(traceback);
                if (dir == null) break;
                nav.add(dir);
                traceback = traceback.add(dir); // moving back to the branching point
            }
            progress = 3;
        }
        
        if (progress == 3) {
            System.out.println("hello I am at progress 3");
            // now let's have the bot actually move "upstream"
            if (progressIndex == -1) {
                progressIndex = nav.size() - 1;
            }
            
            if (progressIndex >= 0 && progressIndex <= nav.size() - 1) {
                Direction dir = nav.get(progressIndex).opposite();

                if (dir == null || !rc.canMove(dir)) {
                    queue.add(new Location(rc.getLocation(), 0));
                    return false;
                }
                rc.move(dir);
                queue.add(new Location(rc.getLocation(), 0));
                progressIndex -= 1;
            }

            if (progressIndex < 0) {
                progress = 4;
            }
        }


        if (current != null)
            if (current.equals(dest)) {
                return true; 
            }


        if (progress == 4) {
            System.out.println("hello I am at progress 4");
            for (int i = 0; i < 8; i++) {
                Direction dir = RobotPlayer.directions[i];
                Location neighbor = current.add(dir);
                int x = neighbor.getLocation().x;
                int y = neighbor.getLocation().y;
                int mapHeight = rc.getMapHeight();
                int mapWidth = rc.getMapWidth();

                if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) {
                    continue;
                }
               
                // IMPORTANT: the heurstic function is the key of this algorithm
                // if heuristic function is not used, then this algorithm is just equivalent to Dijkstra's algorithm
                double newCost = costSoFar.get(current) + heuristics(rc, neighbor, dest);
                System.out.println("cost" + newCost);
                if (!costSoFar.containsKey(neighbor) || newCost < costSoFar.get(neighbor)) {
                    costSoFar.put(neighbor, newCost);
                    neighbor.updateCost(newCost);
                    queue.add(neighbor);
                    predecessor.put(neighbor, dir.opposite()); // tracking the opposite direction
                }
            }
            progress = 0;
            last = null;
        }
        return false;
    } 

    /**
     * Returns a heuristic value needed by A* based on various game conditions
     *
     * TODO: Tweak this function if you want to customize + improve the pathfinding algorithm
     *
     * @param rc A RobotController instance corresponding to the robot
     * @param current A grid on the map whose heuristic value is getting calculated; may not be necessarily the block the bot is on (usually a neighboring block)
     * @param goal A grid on the map that the bot is trying to reach
     * @return A custom heuristic value
     */
    private static double heuristics(RobotController rc, Location current, Location goal) {
        try {
            MapLocation currentMapLocation = current.getLocation();

            if(rc.canSenseRobotAtLocation(currentMapLocation)) {
                return 100;
            }

            System.out.println("inside heuristics");
            System.out.println(currentMapLocation);
            if(!rc.sensePassability(currentMapLocation)) {
                System.out.println("cannot pass");
                return Double.POSITIVE_INFINITY;
            }


            // cloud -> 1.8
            // current against -> 5
            // current toward -> 0.5
            // current sideway -> 1.2-1.5
            // goal -> -NEGATIVE_INFINITY
            //
            // then we have the goal -> greatest priority
            if (current.equals(goal)) {
                return -1;
            }

            
            RobotInfo[] nearbyBots = rc.senseNearbyRobots(current.getLocation(), 16, rc.getTeam().opponent());
            for (RobotInfo enemyBots : nearbyBots) {
                // TODO: Check if this is ok
                if (enemyBots.getType() == RobotType.HEADQUARTERS) {
                    return Double.POSITIVE_INFINITY;
                }
            }

        } catch (Exception e) {
            System.out.println("exception handled infinity" + 
                    e);
            return 1000;
        }

        return 1;
        
    }
    
}



/**
 * Similar to MapLocation but made to allow the use of comparator, etc without worrying about how MapLocation instances are made and returnd
 */
class Location {

    private double cost;
    private MapLocation location;

    public Location(MapLocation loc, double cost) {
        this.location = loc;
        this.cost = cost;
    }

    public int distanceSquaredTo(Location otherLocation) {
        return this.location.distanceSquaredTo(otherLocation.getLocation());
    }

    public boolean isAdjacentTo(Location otherLocation) {
        // is adjacent if the other location is within 1 block (diagonally as well)
        return this.location.isAdjacentTo(otherLocation.getLocation());
    }

    public int getAbsDistanceTo(Location otherLocation) {
        return Math.max(Math.abs(otherLocation.getLocation().x - this.location.x), Math.abs(otherLocation.getLocation().y - this.location.y));
    }

    public void updateCost(double cost) {
        this.cost = cost;
    }

    public double getCost() {
        return cost;
    }

    public MapLocation getLocation() {
        return this.location;
    }

    @Override
    public int hashCode() {
        // there is no way to access the dimension of the map, but I need unique hashcode; 61 is just one above the max size of the map
        return this.location.y * 61 + this.location.x;
    }

    public Location subtract(Direction dir) {
        return new Location(this.location.subtract(dir), 1 + this.cost);
    }

    public Location add(Direction dir) {
        return new Location(this.location.add(dir), 1 + this.cost);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other.getClass() != this.getClass())
            return false;
        Location otherLoc = (Location) other;
        return this.location.x == otherLoc.getLocation().x && this.location.y == otherLoc.getLocation().y;
    }


    @Override
    public String toString() {
        return this.location.toString();
    }
}

class LocationComparator implements Comparator<Location> {

    private Location destination;
    public LocationComparator(Location destination) {
        this.destination = destination;
    }

    public int compare(Location locationA, Location locationB) {
        int diff1 = locationA.getAbsDistanceTo(this.destination);
        int diff2 = locationB.getAbsDistanceTo(this.destination);
        int priority1 = (int) (diff1 + locationA.getCost());
        int priority2 = (int) (diff2 + locationB.getCost());

        return (priority1 > priority2) ? 1 : ((priority1 == priority2) ? 0 : -1);
    }

}
