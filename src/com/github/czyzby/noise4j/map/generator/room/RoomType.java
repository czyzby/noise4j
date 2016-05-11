package com.github.czyzby.noise4j.map.generator.room;

import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.generator.room.AbstractRoomGenerator.Room;

/** Represents a single room type. Determines how the room is carved in the map.
 * <p>
 * Note that even through some room types create non-rectangle rooms, room collisions during map generating are still
 * checked with room's original rectangle bounds to simplify calculations. So, for example, two
 * {@link DefaultRoomType#DIAMOND} rooms cannot be places next to each other as long as their rectangles overlap, even
 * though their cells would be completely separated. However, corridors (or roads) are using actual tile states rather
 * than rooms' bounds, so - for example - diamond rooms can have corridors around them, even though they would normally
 * overlap with a plain square room.
 *
 * @author MJ
 * @see Interceptor */
public interface RoomType {
    /** @param room should be filled.
     * @param grid should contain the room in its selected position.
     * @param value value with which the room should be filled. */
    void carve(Room room, Grid grid, float value);

    /** @param room is about to be filled.
     * @return true if this type can handle this room. Returns false if the room has invalid properties and cannot be
     *         properly created with this type. */
    boolean isValid(Room room);

    /** Wraps around an existing type, allowing to slightly modify its behavior. A common usage can be changing of tile
     * value in carve method to a custom one, allowing different room types to use different tile sets, for example.
     * Extend this class and override isValid method if you want to add custom conditions.
     *
     * @author MJ */
    public static class Interceptor implements RoomType {
        protected final RoomType type;
        protected final float value;

        /** @param type wrapped type. Will delegate method calls to this type. Cannot be null.
         * @param value custom value passed to carving method. */
        public Interceptor(final RoomType type, final float value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public void carve(final Room room, final Grid grid, final float value) {
            type.carve(room, grid, this.value);
        }

        @Override
        public boolean isValid(final Room room) {
            return type.isValid(room);
        }
    }

    /** Contains default implementations of {@link RoomType}.
     *
     * @author MJ */
    public static enum DefaultRoomType implements RoomType {
        /** Fills all of room's cells. Default behavior if room types are not used. Works with any room size. */
        SQUARE {
            @Override
            public void carve(final Room room, final Grid grid, final float value) {
                room.fill(grid, value);
            }
        },
        /** Uses a very simple algorithm to round room's corners. Works best for about 5 to 25 room size. */
        ROUNDED {
            @Override
            public void carve(final Room room, final Grid grid, final float value) {
                final int halfSize = (room.getWidth() + room.getHeight()) / 2;
                final int maxDistanceFromCenter = halfSize * 9 / 10;
                for (int x = 0, width = room.getWidth(); x < width; x++) {
                    for (int y = 0, height = room.getHeight(); y < height; y++) {
                        final int distanceFromCenter = Math.abs(x - width / 2) + Math.abs(y - height / 2);
                        if (distanceFromCenter < maxDistanceFromCenter) {
                            grid.set(x + room.getX(), y + room.getY(), value);
                        }
                    }
                }
            }
        },
        /** Instead of carving a simple rectangle, forms a rectangle with four "towers" in rooms' corners. Works with
         * pretty much any room size, but requires the room to be at least 7x7 squares big (and small/wide rooms do look
         * like bones instead of castles). */
        CASTLE {
            public static final int MIN_SIZE = 7, MIN_TOWER = 3;

            @Override
            public void carve(final Room room, final Grid grid, final float value) {
                final int size = Math.min(room.getWidth(), room.getHeight());
                final int towerSize = Math.max((size - 1) / 4, MIN_TOWER);
                final int offset = Math.max(towerSize / 4, towerSize == MIN_TOWER ? 1 : 2);
                // Main room:
                for (int x = offset, width = room.getWidth() - offset; x < width; x++) {
                    for (int y = offset, height = room.getHeight() - offset; y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
                // Towers:
                for (int x = 0, width = towerSize; x < width; x++) {
                    for (int y = 0, height = towerSize; y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
                for (int x = room.getWidth() - towerSize, width = room.getWidth(); x < width; x++) {
                    for (int y = 0, height = towerSize; y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
                for (int x = 0, width = towerSize; x < width; x++) {
                    for (int y = room.getHeight() - towerSize, height = room.getHeight(); y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
                for (int x = room.getWidth() - towerSize, width = room.getWidth(); x < width; x++) {
                    for (int y = room.getHeight() - towerSize, height = room.getHeight(); y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
            }

            @Override
            public boolean isValid(final Room room) {
                return room.getWidth() >= MIN_SIZE && room.getHeight() >= MIN_SIZE;
            }
        },
        /** Forms a pyramid-like structures. Can handle only square rooms with side size bigger than 2. Works best on
         * odd room sizes. Since equal width and height rooms can be relatively rare if you use a big tolerance, it is a
         * good idea to add this type multiple times to the possible types list to even its chances. */
        DIAMOND {
            @Override
            public void carve(final Room room, final Grid grid, final float value) {
                final int halfSize = room.getWidth() / 2;
                for (int x = 0, width = room.getWidth(); x < width; x++) {
                    for (int y = 0, height = room.getHeight(); y < height; y++) {
                        final int distanceFromCenter = Math.abs(x - halfSize) + Math.abs(y - halfSize);
                        if (distanceFromCenter <= halfSize) {
                            grid.set(x + room.getX(), y + room.getY(), value);
                        }
                    }
                }
            }

            @Override
            public boolean isValid(final Room room) {
                return room.getWidth() > 2 && room.getWidth() == room.getHeight();
            }
        },
        /** Forms a cross-shaped room, dividing the room into 9 (usually) equal parts and removing the corner ones.
         * Requires the room to have at least 3x3 size. Works best with square rooms. */
        CROSS {
            public static final int MIN_SIZE = 3;

            @Override
            public void carve(final Room room, final Grid grid, final float value) {
                final int offsetX = room.getWidth() / 3;
                final int offsetY = room.getHeight() / 3;
                for (int x = 0, width = room.getWidth(); x < width; x++) {
                    for (int y = offsetY, height = room.getHeight() - offsetY; y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
                for (int x = offsetX, width = room.getWidth() - offsetX; x < width; x++) {
                    for (int y = 0, height = room.getHeight(); y < height; y++) {
                        grid.set(x + room.getX(), y + room.getY(), value);
                    }
                }
            }

            @Override
            public boolean isValid(final Room room) {
                return room.getWidth() >= MIN_SIZE && room.getHeight() >= MIN_SIZE;
            }
        };

        @Override
        public boolean isValid(final Room room) {
            return true;
        }
    }
}
