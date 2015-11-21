package com.github.czyzby.noise4j.array;

/** Base class for containers that wrap around a single 1D array, treating it as a 2D array.
 *
 * @author MJ */
public abstract class Array2D {
    protected final int width;
    protected final int height;

    /** @param size amount of columns and rows. */
    public Array2D(final int size) {
        this(size, size);
    }

    /** @param width amount of columns.
     * @param height amount of rows. */
    public Array2D(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    /** @return amount of columns. */
    public int getWidth() {
        return width;
    }

    /** @return amount of rows. */
    public int getHeight() {
        return height;
    }

    /** @param x column index.
     * @param y row index.
     * @return true if the coordinates are valid and can be safely used with getter methods. */
    public boolean isIndexValid(final int x, final int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /** @param x column index.
     * @param y row index.
     * @return actual array index of the cell. */
    public int toIndex(final int x, final int y) {
        return x + y * width;
    }

    /** @param index actual array index of a cell.
     * @return column index. */
    public int toX(final int index) {
        return index % width;
    }

    /** @param index actual array index of a cell.
     * @return row index. */
    public int toY(final int index) {
        return index / width;
    }
}
