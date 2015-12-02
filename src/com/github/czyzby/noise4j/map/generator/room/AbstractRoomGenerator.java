package com.github.czyzby.noise4j.map.generator.room;

import java.util.Random;

import com.github.czyzby.noise4j.array.Int2dArray;
import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.generator.AbstractGenerator;
import com.github.czyzby.noise4j.map.generator.util.Generators;

/** Abstract base for room-generating algorithms.
 *
 * @author MJ */
public abstract class AbstractRoomGenerator extends AbstractGenerator {
    private int minRoomSize = 3;
    private int maxRoomSize = 7;
    private int tolerance = 2;

    /** @param grid will be used to generate bounds of the room.
     * @return a new random-sized room within grid's bounds. */
    protected Room getRandomRoom(final Grid grid) {
        final int width = randomSize();
        final int height = randomSize(width);
        if (width > grid.getWidth() || height > grid.getHeight()) {
            throw new IllegalStateException(
                    "maxRoomSize is higher than grid's size, which resulted in spawning a room bigger than the whole map. Set maxRoomSize to a lower value.");
        }
        final Random random = Generators.getRandom();
        final int x = normalizePosition(random.nextInt(grid.getWidth() - width));
        final int y = normalizePosition(random.nextInt(grid.getHeight() - height));
        return new Room(x, y, width, height);
    }

    /** @param position row or column index.
     * @return validated and normalized position. */
    protected int normalizePosition(final int position) {
        return position;
    }

    /** @param size random room size value.
     * @return validated and normalized room size. */
    protected int normalizeSize(final int size) {
        return size;
    }

    /** @return random odd room size within {@link #minRoomSize} and {@link #maxRoomSize} range. */
    protected int randomSize() {
        return normalizeSize(minRoomSize == maxRoomSize ? minRoomSize : Generators.randomInt(minRoomSize, maxRoomSize));
    }

    /** @param bound second size variable.
     * @return random odd room size within {@link #minRoomSize} and {@link #maxRoomSize} range, respecting
     *         {@link #tolerance} */
    protected int randomSize(final int bound) {
        final int size = Generators.randomInt(Math.max(minRoomSize, bound - tolerance),
                Math.min(maxRoomSize, bound + tolerance));
        return normalizeSize(size);
    }

    /** @return minimum room's width and height. */
    public int getMinRoomSize() {
        return minRoomSize;
    }

    /** @param minRoomSize minimum room's width and height. Some algorithms might require this value to be odd - even
     *            values might be normalized or ignored. */
    public void setMinRoomSize(final int minRoomSize) {
        if (minRoomSize <= 0 || minRoomSize > maxRoomSize) {
            throw new IllegalArgumentException("minRoomSize cannot be bigger than max or lower than 1.");
        }
        this.minRoomSize = minRoomSize;
    }

    /** @return maximum room's width and height. */
    public int getMaxRoomSize() {
        return maxRoomSize;
    }

    /** @param maxRoomSize maximum room's width and height. Some algorithms might require this value to be odd - even
     *            values might be normalized or ignored. */
    public void setMaxRoomSize(final int maxRoomSize) {
        if (maxRoomSize <= 0 || minRoomSize > maxRoomSize) {
            throw new IllegalArgumentException("maxRoomSize cannot be lower than min or 1.");
        }
        this.maxRoomSize = maxRoomSize;
    }

    /** @param tolerance maximum difference between room's width and height. The bigger the tolerance, the more
     *            rectangular the rooms can be. */
    public void setTolerance(final int tolerance) {
        this.tolerance = tolerance;
    }

    /** @return maximum difference between room's width and height. */
    public int getTolerance() {
        return tolerance;
    }

    /** Basic rectangle class. Contains position and size of a single room. Provides simple, common math operations.
     *
     * @author MJ */
    protected static class Room {
        private final int x, y;
        private final int width, height;

        public Room(final int x, final int y, final int width, final int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /** @param room another room instance.
         * @return true if the two rooms overlap with each other. */
        public boolean overlaps(final Room room) {
            return x < room.x + room.width && x + width > room.x && y < room.y + room.height && y + height > room.y;
        }

        @Override // Auto-generated.
        public String toString() {
            return "Room [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
        }

        /** @param grid its cells will be modified.
         * @param value will be used to fill all cells contained by the room. */
        public void fill(final Grid grid, final float value) {
            for (int x = this.x, sizeX = this.x + width; x < sizeX; x++) {
                for (int y = this.y, sizeY = this.y + height; y < sizeY; y++) {
                    grid.set(x, y, value);
                }
            }
        }

        /** @param grid its cells will be modified.
         * @param value will be used to fill all cells contained by the room. */
        public void fill(final Int2dArray grid, final int value) {
            for (int x = this.x, sizeX = this.x + width; x < sizeX; x++) {
                for (int y = this.y, sizeY = this.y + height; y < sizeY; y++) {
                    grid.set(x, y, value);
                }
            }
        }

        /** @param x column index.
         * @param y row index.
         * @return true if the passed position is on the bounds of the room. */
        public boolean isBorder(final int x, final int y) {
            return this.x == x || this.y == y || this.x + width - 1 == x || this.y + height - 1 == y;
        }
    }
}
