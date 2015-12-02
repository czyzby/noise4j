package com.github.czyzby.noise4j.array;

/** A simple {@link Array2D} extension storing a 1D primitive int array, treating it as a 2D array.
 *
 * @author MJ */
public class Int2dArray extends Array2D {
    private final int[] array;

    public Int2dArray(final int size) {
        this(size, size);
    }

    public Int2dArray(final int width, final int height) {
        super(width, height);
        array = new int[width * height];
    }

    /** @param x column index.
     * @param y row index.
     * @return cell value with the selected index. */
    public int get(final int x, final int y) {
        return array[toIndex(x, y)];
    }

    /** @param x column index.
     * @param y row index.
     * @param value will become the value stored in the selected cell. */
    public void set(final int x, final int y, final int value) {
        array[toIndex(x, y)] = value;
    }

    /** @param value will replace all cells' values.
     * @return this, for chaining. */
    public Int2dArray set(final int value) {
        for (int index = 0, length = array.length; index < length; index++) {
            array[index] = value;
        }
        return this;
    }
}
