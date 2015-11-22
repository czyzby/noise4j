package com.github.czyzby.noise4j.array;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Stores a 1D object array of objects, treating it as a 2D array.
 *
 * @author MJ
 * @param <Type> class of stored objects. */
public abstract class Object2dArray<Type> extends Array2D implements Iterable<Type> {
    private final Type[] array;

    /** @param size amount of columns and rows. */
    public Object2dArray(final int size) {
        super(size, size);
        this.array = getArray(size);
    }

    /** @param width amount of columns.
     * @param height amount of rows. */
    public Object2dArray(final int width, final int height) {
        super(width, height);
        this.array = getArray(width * height);
    }

    /** @param array will be wrapped. Has to be valid - it cannot be too small (and it should not be too big either).
     * @param size amount of columns and rows. */
    public Object2dArray(final Type[] array, final int size) {
        this(array, size, size);
    }

    /** @param array will be wrapped. Has to be valid - it cannot be too small (and it should not be too big either).
     * @param width amount of columns.
     * @param height amount of rows. */
    public Object2dArray(final Type[] array, final int width, final int height) {
        super(width, height);
        if (array.length < width * height) {
            throw new IllegalArgumentException(
                    "Passed array is too small. Expected length: " + width * height + ", received: " + array.length);
        }
        this.array = array;
    }

    /** @param width amount of columns.
     * @param height amount of rows.
     * @return a new {@link Object2dArray} wrapping around a simple object array. Note that if you use this method to
     *         create an array, {@link #getArray()} will throw a {@link ClassCastException}. As long as you use safe
     *         methods - like {@link #iterator()}, {@link #get(int, int)}, {@link #set(int, int, Object)} or
     *         {@link #getObjectArray()} - this is perfectly OK to use this factory method.
     * @param <Type> class of stored objects. */
    public static <Type> Object2dArray<Type> newNotTyped(final int width, final int height) {
        return new Object2dArray<Type>(width, height) {
            @Override
            @SuppressWarnings("unchecked")
            protected Type[] getArray(final int size) {
                return (Type[]) new Object[size];
            }
        };
    }

    /** @param size size of the array to create.
     * @return a new instance of typed array. */
    protected abstract Type[] getArray(int size);

    /** @return direct reference to internal object array.
     * @see #toIndex(int, int) */
    public Type[] getArray() {
        return array;
    }

    /** @return direct reference to internal object array with stripped type. This method never throws
     *         {@link ClassCastException}, even if the array was created with a simple, not-typed object array.
     * @see #toIndex(int, int) */
    public Object[] getObjectArray() {
        return array;
    }

    /** @param x valid column index.
     * @param y valid row index.
     * @return value stored in the selected cell.
     * @see #isIndexValid(int, int) */
    public Type get(final int x, final int y) {
        return array[toIndex(x, y)];
    }

    /** @param x column index.
     * @param y row index.
     * @return value stored in the selected cell or null if cell coordinates are invalid. */
    public Type getOrNull(final int x, final int y) {
        return getOrElse(x, y, null);
    }

    /** @param x column index.
     * @param y row index.
     * @param alternative will be returned if index is invalid.
     * @return value stored in the selected cell or the passed alternative. Can be null if alternative object is null or
     *         value stored in the cell is null. */
    public Type getOrElse(final int x, final int y, final Type alternative) {
        if (isIndexValid(x, y)) {
            return get(x, y);
        }
        return alternative;
    }

    /** @param x valid column index.
     * @param y valid row index.
     * @param value will be stored in the selected cell. */
    public void set(final int x, final int y, final Type value) {
        array[toIndex(x, y)] = value;
    }

    /** Swaps two cells' values.
     *
     * @param x column index of first value.
     * @param y column index of first value.
     * @param x2 column index of second value.
     * @param y2 row index of second value. */
    public void swap(final int x, final int y, final int x2, final int y2) {
        final Type first = get(x, y);
        set(x, y, get(x2, y2));
        set(x2, y2, first);
    }

    /** @param value will be stored in all array's cells. */
    public void fill(final Type value) {
        for (int index = 0, length = width * height; index < length; index++) {
            array[index] = value;
        }
    }

    @Override
    public Iterator<Type> iterator() {
        return new ArrayIterator();
    }

    /** Allows to iterate over wrapped array. {@link #remove()} method clears the value of the current cell, replacing
     * it with null, but it does not modify the size of the array.
     *
     * @author MJ */
    protected class ArrayIterator implements Iterator<Type> {
        private final int size = width * height; // Does not have to match array.length.
        private int index;

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public Type next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return array[index++];
        }

        @Override
        public void remove() {
            if (index == 0) {
                throw new IllegalStateException("#next() has to be called before using #remove() method.");
            }
            array[index - 1] = null;
        }

        /** Allows to reuse the iterator. Begins iteration from the first cell. */
        public void reset() {
            index = 0;
        }
    }
}
