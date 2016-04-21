package com.github.czyzby.noise4j.map.generator.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.github.czyzby.noise4j.array.Int2dArray;
import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.generator.AbstractGenerator;
import com.github.czyzby.noise4j.map.generator.util.Generators;

/** Abstract base for room-generating algorithms.
 *
 * @author MJ */
public abstract class AbstractRoomGenerator extends AbstractGenerator {
    private final List<RoomType> roomTypes = new ArrayList<RoomType>();
    private int minRoomSize = 3;
    private int maxRoomSize = 7;
    private int tolerance = 2;
    private int maxRoomsAmount;

    /** @return direct reference to internal list of accepted room types. */
    public List<RoomType> getRoomTypes() {
        return roomTypes;
    }

    /** @param roomType determines how the room is carved. Room type is chosen at random during room generating. Note
     *            that you can add a single room type multiple times to make it more likely for the type to be
     *            chosen. */
    public void addRoomType(final RoomType roomType) {
        roomTypes.add(roomType);
    }

    /** @param roomType determines how the room is carved. Room type is chosen at random during room generating. Note
     *            that you can add a single room type multiple times to make it more likely for the type to be chosen.
     * @param times this is how many times the room will be added to the list. The bigger this value, the more likely
     *            this room time gets rolled. */
    public void addRoomType(final RoomType roomType, final int times) {
        for (int index = 0; index < times; index++) {
            addRoomType(roomType);
        }
    }

    /** @param roomTypes determine how the rooms are carved. Room type is chosen at random during room generating. Note
     *            that you can add a single room type multiple times to make it more likely for the type to be
     *            chosen. */
    public void addRoomTypes(final RoomType... roomTypes) {
        for (final RoomType roomType : roomTypes) {
            addRoomType(roomType);
        }
    }

    /** @param grid contains the room.
     * @param room was just spawned. Should fill its values in the grid.
     * @param value value used to fill the room. */
    protected void carveRoom(final Grid grid, final Room room, final float value) {
        if (roomTypes.isEmpty()) { // No types specified: carving whole room:
            room.fill(grid, value);
        } else {
            int index = Generators.randomIndex(roomTypes);
            final int originalIndex = index;
            RoomType type = roomTypes.get(index);
            while (!type.isValid(room)) {
                index = (index + 1) % roomTypes.size();
                if (index == originalIndex) { // No valid types found for the room:
                    room.fill(grid, value);
                    return;
                }
                type = roomTypes.get(index);
            }
            type.carve(room, grid, value);
        }
    }

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

    /** @return maximum amount of generated rooms. If 0 or negative, there is no limit. */
    public int getMaxRoomsAmount() {
        return maxRoomsAmount;
    }

    /** @param maxRoomsAmount maximum amount of generated rooms. While achieving a certain amount of rooms is not always
     *            possible (due to collisions or simply the map being too small to store chosen amount), you can limit
     *            the maximum amount of rooms that will be generated. If set to negative or zero (the default value),
     *            there is no limit. */
    public void setMaxRoomsAmount(final int maxRoomsAmount) {
        this.maxRoomsAmount = maxRoomsAmount;
    }

    /** Basic rectangle class. Contains position and size of a single room. Provides simple, common math operations.
     *
     * @author MJ */
    public static class Room {
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

        /** @return column index of the room's start. */
        public int getX() {
            return x;
        }

        /** @return row index of the room's start. */
        public int getY() {
            return y;
        }

        /** @return amount of columns taken by the room. */
        public int getWidth() {
            return width;
        }

        /** @return amount of rows taken by the room. */
        public int getHeight() {
            return height;
        }

        @Override // Auto-generated.
        public String toString() {
            return "Room [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
        }
    }
}
