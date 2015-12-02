package com.github.czyzby.noise4j.map.generator.room.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.github.czyzby.noise4j.array.Int2dArray;
import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.generator.room.AbstractRoomGenerator;
import com.github.czyzby.noise4j.map.generator.util.Generators;

/** Generates a set of rooms with a maze-like system of corridors connecting them. This particular implementation
 * requires the map and rooms to have odd sizes - if the passed map is not odd, last row and column might be filled with
 * corridors.
 * <p>
 * This algorithm fills the whole map. Even if the map was not empty before, {@link #generate(Grid)} method will
 * override the previous cell settings - it's better to modify already generated dungeon rather than pass non-empty grid
 * to this generator.
 *
 * @author MJ */
/* Algorithm was based on implementation from journal.stuffwithstuff.com, translated to Java and improved (especially
 * the dead end removal and region joining parts):
 *
 * 0. Reset the grid. Set all cells as walls.
 *
 * 1. Generate rooms. Spawn random rooms across the map, honoring min/max size settings and width&height difference
 * tolerance.
 *
 * 1.1 Fill each room's cell with floor value.
 *
 * 1.2 Set each room's cell "region" value. Region value is common for all cells of currently generated part of the
 * dungeon, be a room or corridors set.
 *
 * 2. Generate corridors. Corridor regions cannot be connected to the rooms and each other (yet).
 *
 * 2.1 Fill each corridor cell with specified corridor value. Assign cell to corridor region.
 *
 * 3. Join regions. Every non-wall cell should be accessible from any other non-wall cell.
 *
 * 3.1 Find every possible connector (wall with non-wall neighbors from at least 2 separate regions). Shuffle them.
 *
 * 3.2 Until all regions are merged, iterate over connectors. If they separate two unconnected regions, replace the wall
 * with a corridor.
 *
 * 3.3 If a connector is neighbor of already connected regions, discard it - unless it passes a random test, in which
 * case replace it with a corridor to make the dungeon not perfect.
 *
 * 4. Remove dead ends.
 *
 * 4.1 Iterate over the whole map, locate all current dead ends.
 *
 * 4.2 Iterate over dead ends list until the desired dead end removal iterations amount is achieved or the dead end list
 * is cleared. Remove dead end from the list after it is converted into a wall and it has no dead end neighbors;
 * otherwise leave on the list and modify its coordinates to match the dead end neighbor. */
public class DungeonGenerator extends AbstractRoomGenerator {
    private static DungeonGenerator INSTANCE;

    // Settings.
    private int roomGenerationAttempts;
    private float wallThreshold = 1f;
    private float floorThreshold = 0.5f;
    private float corridorThreshold;
    private float windingChance = 0.15f;
    private float randomConnectorChance = 0.01f;
    private int deadEndRemovalIterations = Integer.MAX_VALUE;

    // Control variables.
    private final List<Room> rooms = new LinkedList<Room>();
    private final Queue<Point> cells = new LinkedList<Point>();
    private final List<Direction> directions = new ArrayList<Direction>();
    private Int2dArray regions;
    private int currentRegion;

    /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or
     * obtaining an instance of the generator is generally preferred.
     *
     * @param grid will be used to generate the dungeon.
     * @param roomGenerationAttempts see {@link #getRoomGenerationAttempts()}. */
    public static void generate(final Grid grid, final int roomGenerationAttempts) {
        final DungeonGenerator generator = getInstance();
        generator.setRoomGenerationAttempts(roomGenerationAttempts);
        generator.generate(grid);
    }

    /** @return static instance of the generator. Not thread-safe. */
    public static DungeonGenerator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DungeonGenerator();
        }
        return INSTANCE;
    }

    @Override
    public void generate(final Grid grid) {
        validateRoomSizes();
        reset();
        // Mirroring grid with a 2D int array - each non-wall mirrored cell will contain region index:
        regions = new Int2dArray(grid.getWidth(), grid.getHeight());
        // Filling grid with wall tiles:
        grid.set(wallThreshold);
        // Generating rooms:
        spawnRooms(grid, roomGenerationAttempts == 0 ? getDefaultRoomsAmount(grid) : roomGenerationAttempts);
        // Generating corridors:
        spawnCorridors(grid);
        // Joining spawned rooms and corridor regions:
        joinRegions(grid);
        // Removing corridors leading to nowhere:
        removeDeadEnds(grid);
        reset(); // Removing all unnecessary references.
    }

    /** Resets control variables. */
    protected void reset() {
        currentRegion = -1;
        rooms.clear();
        cells.clear();
        directions.clear();
        regions = null;
    }

    /** Increases current region index. */
    protected void nextRegion() {
        currentRegion++;
    }

    /** @throws IllegalStateException if room sizes are not odd. */
    protected void validateRoomSizes() {
        if (getMinRoomSize() % 2 == 0 || getMaxRoomSize() % 2 == 0) {
            throw new IllegalStateException("Min and max room sizes have to be odd.");
        }
    }

    /** @param grid will contain generated dungeon.
     * @return maximum amount of placed rooms. Used if {@link #roomGenerationAttempts} is not set. */
    private int getDefaultRoomsAmount(final Grid grid) {
        return grid.getWidth() / getMaxRoomSize() * (grid.getHeight() / getMaxRoomSize());
    }

    /** @param grid is being generated.
     * @param attempts amount of attempts of placing rooms before the generator gives up. */
    protected void spawnRooms(final Grid grid, final int attempts) {
        for (int index = 0; index < attempts; index++) {
            final Room newRoom = getRandomRoom(grid);
            if (!overlapsAny(newRoom)) {
                rooms.add(newRoom);
                carveRoom(grid, newRoom);
                nextRegion();
                newRoom.fill(regions, currentRegion); // Assigning region values to all cells.
            }
        }
    }

    /** @param grid contains the room.
     * @param newRoom was just spawned. Should fill its values in the grid. */
    protected void carveRoom(final Grid grid, final Room newRoom) {
        newRoom.fill(grid, floorThreshold);
    }

    /** @param room validated room.
     * @return true if passed room overlaps with any of the current rooms. */
    protected boolean overlapsAny(final Room room) {
        for (final Room currentRoom : rooms) {
            if (currentRoom.overlaps(room)) {
                return true;
            }
        }
        return false;
    }

    /** @param grid will contain mazes spawned on the non-grid cells. */
    protected void spawnCorridors(final Grid grid) {
        for (int x = 1, width = grid.getWidth(); x < width; x += 2) {
            for (int y = 1, height = grid.getHeight(); y < height; y += 2) {
                if (isWall(grid, x, y)) {
                    carveMaze(grid, new Point(x, y));
                }
            }
        }
    }

    /** @param grid contains the cell.
     * @param x column index.
     * @param y row index.
     * @return true if the selected cell is a wall. */
    protected boolean isWall(final Grid grid, final int x, final int y) {
        return grid.get(x, y) >= wallThreshold;
    }

    /** @param grid contains the point.
     * @param point will start carving maze at this point. Stops when it reaches a dead end. */
    protected void carveMaze(final Grid grid, final Point point) {
        nextRegion();
        Direction lastDirection = null;
        while (true) {
            // Carving current point:
            carveCorridor(grid, point);
            regions.set(point.x, point.y, currentRegion);

            directions.clear();
            // Checking neighbors - getting possible carving directions:
            for (final Direction direction : Direction.values()) {
                if (isCarveable(point, grid, direction)) {
                    directions.add(direction);
                }
            }
            if (directions.isEmpty()) {
                return;
            }
            Direction carvingDirection;
            // Getting actual carving direction:
            if (lastDirection != null && directions.contains(lastDirection)
                    && Generators.randomPercent() > windingChance) {
                carvingDirection = lastDirection;
            } else {
                carvingDirection = Generators.randomElement(directions);
            }
            lastDirection = carvingDirection;
            // Carving "ignored" even-indexed corridor cell:
            carvingDirection.next(point);
            carveCorridor(grid, point);
            regions.set(point.x, point.y, currentRegion);
            // Switching to next odd-index cell, repeating until no viable neighbors left:
            carvingDirection.next(point);
        }
    }

    /** @param grid contains the point.
     * @param point a point representing a part of a corridor. Should set its value in the grid. */
    protected void carveCorridor(final Grid grid, final Point point) {
        grid.set(point.x, point.y, corridorThreshold);
    }

    /** @param point part of the corridor.
     * @param grid contains the point.
     * @param direction possible carving direction.
     * @return true if can carve in the selected direction. */
    protected boolean isCarveable(final Point point, final Grid grid, final Direction direction) {
        final int x = direction.nextX(point.x, 2); // Omitting 1 field, checking the next odd one.
        final int y = direction.nextY(point.y, 2);
        // Checking if index within grid bounds and not in a region yet:
        return grid.isIndexValid(x, y) && isWall(grid, x, y);
    }

    /** @param grid contains unconnected room and corridor regions. */
    protected void joinRegions(final Grid grid) { // DRAGON.
        nextRegion();
        // Working on boxed primitives, because lawl, Java generics and collections.
        final Map<Point, Set<Integer>> connectorsToRegions = findConnectors(grid);
        final List<Point> connectors = new ArrayList<Point>(connectorsToRegions.keySet());
        final Integer[] merged = new Integer[currentRegion]; // Keeps track of merged regions.
        final Set<Integer> unjoined = new HashSet<Integer>(); // Keeps track of unconnected regions.
        for (int index = 0; index < currentRegion; index++) {
            // All regions point to themselves at first:
            merged[index] = index;
            // All regions start unjoined:
            unjoined.add(index);
        }
        Generators.shuffle(connectors);
        final Set<Integer> tempSet = new HashSet<Integer>();
        // Looping until all regions point to one source:
        for (final Iterator<Point> connectorIterator = connectors.iterator(); connectorIterator.hasNext()
                && unjoined.size() > 1;) {
            final Point connector = connectorIterator.next();
            // These are the regions that the connector originally pointed to - we need to convert them to the "new",
            // merged region indexes:
            final Set<Integer> regions = connectorsToRegions.get(connector);
            tempSet.clear();
            for (final Integer region : regions) {
                tempSet.add(merged[region]);
            }
            if (tempSet.size() <= 1) { // All connector's regions point to the same region group...
                if (Generators.randomPercent() < randomConnectorChance) {
                    // This connector is not actually needed, but it got lucky - carving:
                    carveConnector(grid, connector.x, connector.y);
                }
                continue;
            }
            carveConnector(grid, connector.x, connector.y);
            regions.clear();
            regions.addAll(tempSet);
            final Iterator<Integer> regionsIterator = regions.iterator();
            // Using first region as our "source":
            final Integer source = regionsIterator.next(); // Safe, has at least 2 regions.
            // Using the rest of the region as destinations - they will point to the source region in merged array:
            final Integer[] destinations = getDestinations(regions, regionsIterator, merged);
            // Changing merged status - all regions that currently point to destinations will now point to source:
            for (int regionIndex = 0; regionIndex < currentRegion; regionIndex++) {
                for (final Integer destination : destinations) {
                    if (merged[regionIndex].equals(destination)) {
                        // This region was previously connected with one of our joined regions (or itself). Now pointing
                        // to our source.
                        merged[regionIndex] = source;
                    }
                }
            }
            // Removing destinations - which were clearly joined - from unjoined regions:
            for (final Integer destination : destinations) {
                unjoined.remove(destination);
            }
        }
    }

    /** Should change selected point's value. Note that connector might connect both two corridors and two rooms -
     * spawning a door (for example) might not be always desired; check cell neighbors first.
     *
     * @param grid contains the point.
     * @param x column index.
     * @param y row index. */
    protected void carveConnector(final Grid grid, final int x, final int y) {
        grid.set(x, y, corridorThreshold);
    }

    /** @param grid contains unconnected room and corridor regions.
     * @return map of points that are neighbors to at least 2 different regions mapped to set of IDs of their
     *         neighbors. */
    protected Map<Point, Set<Integer>> findConnectors(final Grid grid) {
        final Map<Point, Set<Integer>> connectorsToRegions = new HashMap<Point, Set<Integer>>();
        for (int x = 1, width = grid.getWidth() - 1; x < width; x++) {
            for (int y = 1, height = grid.getHeight() - 1; y < height; y++) {
                addConnector(grid, connectorsToRegions, x, y);
            }
        }
        return connectorsToRegions;
    }

    /** @param grid contains the regions.
     * @param connectorsToRegions map of possible connectors to the collection of regions that are their neighbors.
     * @param x column index of possible connector.
     * @param y row index of possible connector. */
    protected void addConnector(final Grid grid, final Map<Point, Set<Integer>> connectorsToRegions, final int x,
            final int y) {
        if (isWall(grid, x, y)) {
            final Set<Integer> regions = new HashSet<Integer>(4, 1f);
            for (final Direction direction : Direction.values()) {
                final int region = getRegion(direction.nextX(x), direction.nextY(y));
                if (region >= 0 && !isWall(grid, direction.nextX(x), direction.nextY(y))) {
                    regions.add(region);
                }
            }
            if (regions.size() > 1) { // At least 2 regions.
                connectorsToRegions.put(new Point(x, y), regions);
            }
        }
    }

    /** @param regions all regions of a connector.
     * @param regionsIterator regions' iterator. Should have one value skipped (source).
     * @param merged contains mapping of regions to the IDs of their supergroups.
     * @return regions marked as destinations.
     * @see #joinRegions(Grid) */
    protected Integer[] getDestinations(final Set<Integer> regions, final Iterator<Integer> regionsIterator,
            final Integer[] merged) {
        final Integer[] destinations = new Integer[regions.size() - 1];
        int index = 0;
        while (regionsIterator.hasNext()) {
            destinations[index++] = merged[regionsIterator.next()];
        }
        return destinations;
    }

    /** @param x column index.
     * @param y row index.
     * @return region index of the cell. -1 if not in a region. */
    protected int getRegion(final int x, final int y) {
        if (regions.isIndexValid(x, y)) {
            return regions.get(x, y);
        }
        return -1;
    }

    /** @param grid will have its cells with 3 or 4 wall neighbors removed. */
    protected void removeDeadEnds(final Grid grid) {
        if (deadEndRemovalIterations <= 0) {
            return; // The user wants us to leave all dead ends. No need to waste time searching for them.
        }
        final List<Point> deadEnds = new LinkedList<Point>();
        for (int x = 0, width = grid.getWidth(); x < width; x++) {
            for (int y = 0, height = grid.getHeight(); y < height; y++) {
                if (isDeadEnd(grid, x, y)) {
                    deadEnds.add(new Point(x, y));
                }
            }
        }
        // Removing dead ends until there are none left or we've done enough iterations:
        for (int index = 0; index < deadEndRemovalIterations && !deadEnds.isEmpty(); index++) {
            for (final Iterator<Point> iterator = deadEnds.iterator(); iterator.hasNext();) {
                final Point deadEnd = iterator.next();
                // Closing dead end:
                grid.set(deadEnd.x, deadEnd.y, wallThreshold);
                // Checking dead end neighbors - one (and only one) of them can be a dead end too:
                if (!findDeadEndNeighbor(grid, deadEnd)) {
                    // No dead end neighbors found - removing dead end from list:
                    iterator.remove();
                } // else { Point becomes its neighbor - will be removed on next iteration (or never). }
            }
        }
    }

    /** @param grid contains the cell.
     * @param deadEnd a currently closed dead end that can possibly have a single dead end neighbor.
     * @return true if dead end neighbor present. */
    private boolean findDeadEndNeighbor(final Grid grid, final Point deadEnd) {
        for (final Direction direction : Direction.values()) {
            if (isDeadEnd(grid, direction.nextX(deadEnd.x), direction.nextY(deadEnd.y))) {
                // Setting dead end as its neighbor:
                deadEnd.x = direction.nextX(deadEnd.x);
                deadEnd.y = direction.nextY(deadEnd.y);
                return true;
            }
        }
        return false;
    }

    /** @param grid contains the cell.
     * @param x column index.
     * @param y row index.
     * @return true if the cell has at least 3 wall neighbors. */
    protected boolean isDeadEnd(final Grid grid, final int x, final int y) {
        if (grid.isIndexValid(x, y) && !isWall(grid, x, y)) {
            int wallNeighbors = 0;
            int nextX;
            int nextY;
            for (final Direction direction : Direction.values()) {
                nextX = direction.nextX(x);
                nextY = direction.nextY(y);
                if (grid.isIndexValid(nextX, nextY) && isWall(grid, nextX, nextY)) {
                    wallNeighbors++;
                }
            }
            return wallNeighbors >= 3;
        }
        return false;
    }

    @Override // Room position has to be odd.
    protected int normalizePosition(final int position) {
        if (position == 0) {
            return 1;
        }
        return position % 2 == 0 ? position - 1 : position;
    }

    @Override // Room size has to be odd.
    protected int normalizeSize(int size) {
        if (size % 2 != 1) {
            return Generators.getRandom().nextBoolean() ? --size : ++size;
        }
        return size;
    }

    /** @return amount of attempts of placing a new room during dungeon generation. */
    public int getRoomGenerationAttempts() {
        return roomGenerationAttempts;
    }

    /** @param roomGenerationAttempts amount of attempts of placing a new room during dungeon generation. Changing this
     *            value allows to modify density of the rooms. */
    public void setRoomGenerationAttempts(final int roomGenerationAttempts) {
        this.roomGenerationAttempts = roomGenerationAttempts;
    }

    /** @return cells are considered walls if they are equal to or bigger than this value. */
    public float getWallThreshold() {
        return wallThreshold;
    }

    /** @param wallThreshold cells are considered walls if they are equal to or bigger than this value. Should be higher
     *            than {@link #getCorridorThreshold()} and {@link #getFloorThreshold()}. */
    public void setWallThreshold(final float wallThreshold) {
        this.wallThreshold = wallThreshold;
    }

    /** @return cells are considered room floor if they are equal this value. */
    public float getFloorThreshold() {
        return floorThreshold;
    }

    /** @param floorThreshold cells are considered room floor if they are equal this value. Should be lower than
     *            {@link #getWallThreshold()}. */
    public void setFloorThreshold(final float floorThreshold) {
        this.floorThreshold = floorThreshold;
    }

    /** @return cells are considered corridors if they are equal this value. */
    public float getCorridorThreshold() {
        return corridorThreshold;
    }

    /** @param corridorThreshold cells are considered corridors if they are equal this value. Should be lower than
     *            {@link #getWallThreshold()}. */
    public void setCorridorThreshold(final float corridorThreshold) {
        this.corridorThreshold = corridorThreshold;
    }

    /** @return chance to wind the currently generated corridor in range of 0 to 1. */
    public float getWindingChance() {
        return windingChance;
    }

    /** @param windingChance chance to wind the currently generated corridor in range of 0 to 1. Anything below (or
     *            equal to) 0 results in winding the corridor only if cannot continue carving in the same direction (not
     *            the best idea to make a playable game). Anything above or equal to 1 results in completely random
     *            corridor carving, resulting in highly chaotic mazes. The higher the value, the more chaotic the
     *            result. */
    public void setWindingChance(final float windingChance) {
        this.windingChance = windingChance;
    }

    /** @return chance of a random carved cell between two regions (rooms and corridors) in range of 0 to 1. */
    public float getRandomConnectorChance() {
        return randomConnectorChance;
    }

    /** @param randomConnectorChance chance of a random carved cell between two regions (rooms and corridors) in range
     *            of 0 to 1. The higher this value, the more passages will appear between the rooms and different
     *            corridors. Setting this value to 0 (or lower) will result in a "perfect" dungeon, which basically has
     *            only one way of solving (as in getting from 1 point to another, treating rooms as one cell). This
     *            might make the dungeon crawling linear, so this is usually not desired; however, using too high value
     *            might result in broken dungeons with a lot of unnecessary passages and doors. */
    public void setRandomConnectorChance(final float randomConnectorChance) {
        this.randomConnectorChance = randomConnectorChance;
    }

    /** @return amount of iterations performed to remove all dead ends in the corridors. */
    public int getDeadEndRemovalIterations() {
        return deadEndRemovalIterations;
    }

    /** @param deadEndRemovalIterations amount of iterations performed to remove all dead ends in the corridors. Each
     *            iteration removes only the last dead end's tile, not the whole branch. This should not cause
     *            significant performance penalties, but if you do want to leave some dead ends in the final dungeon or
     *            need the generation to be blazing fast, you can limit this value. Note that also generating more rooms
     *            - {@link #setRoomGenerationAttempts(int)}, creating more passages -
     *            {@link #setRandomConnectorChance(float)} or making the dungeon more random -
     *            {@link #setWindingChance(float)} - might decrease the amount of dead ends (or overall corridors
     *            amount). */
    public void setDeadEndRemovalIterations(final int deadEndRemovalIterations) {
        this.deadEndRemovalIterations = deadEndRemovalIterations;
    }

    /** A simple container class, storing 2 values.
     *
     * @author MJ */ // Avoids extra dependencies and classes not available on Android/GWT.
    protected static class Point {
        // Note that its interface makes this class effectively final if used externally, but it can be mutated
        // internally for object reuse.
        private int x, y;

        public Point(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        /** @param point another point.
         * @return true if the passed point is a direct (non-diagonal) neighbor of this point. */
        public boolean isNeighbor(final Point point) {
            final int xDifference = Math.abs(x - point.x);
            if (xDifference == 0) {
                return Math.abs(y - point.y) == 1;
            } else if (xDifference == 1) {
                return Math.abs(y - point.y) == 0;
            }
            return false;
        }

        /** @return column index. */
        public int x() {
            return x;
        }

        /** @return row index. */
        public int y() {
            return y;
        }

        @Override
        public boolean equals(final Object object) {
            return this == object || object instanceof Point && ((Point) object).x == x && ((Point) object).y == y;
        }

        @Override
        public int hashCode() {
            return x + y * 653;
        }

        @Override
        public String toString() {
            return "[" + x + "," + y + "]";
        }
    }

    /** Contains all possible corridor carving directions.
     *
     * @author MJ */
    protected static enum Direction {
        UP {
            @Override
            public void next(final Point point) {
                point.y++;
            }

            @Override
            public int nextY(final int y, final int amount) {
                return y + amount;
            }
        },
        DOWN {
            @Override
            public void next(final Point point) {
                point.y--;
            }

            @Override
            public int nextY(final int y, final int amount) {
                return y - amount;
            }
        },
        LEFT {
            @Override
            public void next(final Point point) {
                point.x--;
            }

            @Override
            public int nextX(final int x, final int amount) {
                return x - amount;
            }
        },
        RIGHT {
            @Override
            public void next(final Point point) {
                point.x++;
            }

            @Override
            public int nextX(final int x, final int amount) {
                return x + amount;
            }
        };

        /** @param point a point in the grid. Its coordinates will be modified to represent the next cell in the chosen
         *            direction. */
        public abstract void next(Point point);

        /** @param x current column index.
         * @return column index of the next cell. */
        public int nextX(final int x) {
            return nextX(x, 1);
        }

        /** @param y current row index.
         * @return row index of the next cell. */
        public int nextY(final int y) {
            return nextY(y, 1);
        }

        /** @param x current column index.
         * @param amount distance from the selected cell.
         * @return column index of the selected cell. */
        public int nextX(final int x, final int amount) {
            return x;
        }

        /** @param y current row index.
         * @param amount distance from the selected cell.
         * @return row index of the selected cell. */
        public int nextY(final int y, final int amount) {
            return y;
        }
    }
}
