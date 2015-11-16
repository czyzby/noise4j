package com.github.czyzby.noise4j.map.generator;

import com.github.czyzby.noise4j.map.Grid;

/** Common interface for all map generators.
 *
 * @author MJ */
public interface Generator {
    /** @param grid all (or most) of its cells will be affected, usually by adding or subtracting their current cell
     *            value. */
    void generate(Grid grid);

    /** @param mode decides how values modify current grid's cells. */
    void setMode(GenerationMode mode);

    /** Decides how values modify current grid's cells.
     *
     * @author MJ */
    public static enum GenerationMode {
        /** Adds value to the current cell's value. Default. */
        ADD {
            @Override
            public void modify(final Grid grid, final int x, final int y, final float value) {
                grid.add(x, y, value);
            }
        },
        /** Subtracts value from the current cell's value. */
        SUBTRACT {
            @Override
            public void modify(final Grid grid, final int x, final int y, final float value) {
                grid.subtract(x, y, value);
            }
        },
        /** Multiplies current cell's value. */
        MULTIPLY {
            @Override
            public void modify(final Grid grid, final int x, final int y, final float value) {
                grid.multiply(x, y, value);
            }
        },
        /** Divides current cell's value. */
        DIVIDE {
            @Override
            public void modify(final Grid grid, final int x, final int y, final float value) {
                grid.divide(x, y, value);
            }
        },
        /** Replaces current cell's value. */
        REPLACE {
            @Override
            public void modify(final Grid grid, final int x, final int y, final float value) {
                grid.set(x, y, value);
            }
        };

        /** @param grid contains a cell.
         * @param x cell column index.
         * @param y cell row index.
         * @param value will modify current cell value. */
        public abstract void modify(Grid grid, int x, int y, float value);
    }
}
