# Battlecode 2023 Scaffold

# NOTE: this is a local copy of the team repo for Battlecode 2023 (Team-based competition at MIT; the team repo is on private as it was for a competition and we did not want to expose our code). My work is mainly in this [file](https://github.com/Yooncw0223/battlecode_23_copy/blob/main/src/CYPlayer/PathFinding.java) (where I worked on path finding using BugNav and/or AStar algorithm—with a huge help from this [article](http://theory.stanford.edu/~amitp/GameProgramming/AStarComparison.html)).


This is the Battlecode 2023 scaffold, containing an `examplefuncsplayer`. Read https://play.battlecode.org/bc23/getting-started!

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client. The proper executable can be found in this folder (don't move this!)
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.

### How to get started

You are free to directly edit `examplefuncsplayer`.
However, we recommend you make a new bot by copying `examplefuncsplayer` to a new package under the `src` folder.

### Useful Commands

- `./gradlew build`
    Compiles your player
- `./gradlew run`
    Runs a game with the settings in gradle.properties
- `./gradlew update`
    Update configurations for the latest version -- run this often
- `./gradlew zipForUpdate`
    Create a submittable zip file
- `./gradlew tasks`
    See what else you can do!
