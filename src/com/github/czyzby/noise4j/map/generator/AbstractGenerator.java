package com.github.czyzby.noise4j.map.generator;

import com.github.czyzby.noise4j.map.Grid;

/** Abstract base for map generators. Manages current {@link GenerationMode}.
 *
 * @author MJ */
public abstract class AbstractGenerator implements Generator {
    private GenerationMode mode = GenerationMode.ADD;

    @Override
    public GenerationMode getMode() {
        return mode;
    }

    @Override
    public void setMode(final GenerationMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Generation mode cannot be null.");
        }
        this.mode = mode;
    }

    /** @param grid processed grid.
     * @param x column index.
     * @param y row index.
     * @param value will modify current cell value. */
    protected void modifyCell(final Grid grid, final int x, final int y, final float value) {
        mode.modify(grid, x, y, value);
    }
}
