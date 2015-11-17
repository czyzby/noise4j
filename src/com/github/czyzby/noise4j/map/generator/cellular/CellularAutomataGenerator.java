package com.github.czyzby.noise4j.map.generator.cellular;

import java.util.Random;

import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.Grid.CellConsumer;
import com.github.czyzby.noise4j.map.generator.AbstractGenerator;
import com.github.czyzby.noise4j.map.generator.util.Generators;

/** Contains a marker - a single float value; every cell below this value is considered dead, the others are alive.
 * During each iteration, if a living cell has too few living neighbors, it will die (marker will be subtracted from its
 * value). If a dead cell has enough living neighbors, it will become alive (marker will modify its current value
 * according to {@link #getMode()} - by default, marker will be added). This usually results in a cave-like map. The
 * more iterations, the smoother the map is.
 *
 * <p>
 * Since this generator creates pretty much boolean-based maps (sets each cell as dead or alive), this generator is
 * usually used first to create the general layout of the map - like a caverns system or islands.
 *
 * @author MJ */
public class CellularAutomataGenerator extends AbstractGenerator implements CellConsumer {
    private static CellularAutomataGenerator INSTANCE;

    private boolean initiate = true;
    private float marker = 1f;
    private float aliveChance = 0.5f;
    private int iterationsAmount = 3;
    private int birthLimit = 4;
    private int deathLimit = 3;
    private int radius = 1;
    private Grid temporaryGrid;

    /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or
     * obtaining an instance of the generator is generally preferred.
     *
     * @param grid its cells will be affected.
     * @param iterationsAmount {@link #setIterationsAmount(int)} */
    public static void generate(final Grid grid, final int iterationsAmount) {
        generate(grid, iterationsAmount, 1f, true);
    }

    /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or
     * obtaining an instance of the generator is generally preferred.
     *
     * @param grid its cells will be affected.
     * @param iterationsAmount {@link #setIterationsAmount(int)}
     * @param marker {@link #setMarker(float)}
     * @param initiate {@link #setInitiate(boolean)} */
    public static void generate(final Grid grid, final int iterationsAmount, final float marker,
            final boolean initiate) {
        final CellularAutomataGenerator generator = getInstance();
        generator.setIterationsAmount(iterationsAmount);
        generator.setMarker(marker);
        generator.setInitiate(initiate);
        generator.generate(grid);
    }

    /** @return static instance of the generator. Not thread-safe. */
    public static CellularAutomataGenerator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CellularAutomataGenerator();
        }
        return INSTANCE;
    }

    /** @return amount of generation iterations. */
    public int getIterationsAmount() {
        return iterationsAmount;
    }

    /** @param iterationsAmount amount of generation iterations. The more iterations, the smoother the result. */
    public void setIterationsAmount(final int iterationsAmount) {
        this.iterationsAmount = iterationsAmount;
    }

    /** @return if {@link #isInitiating()} returns true, some alive cells will be spawned before the generation. This is
     *         the change of the cell becoming alive before generating. */
    public float getAliveChance() {
        return aliveChance;
    }

    /** @param aliveChance if {@link #isInitiating()} returns true, some alive cells will be spawned before the
     *            generation. This is the change of the cell becoming alive before generating. In range from 0 to 1. */
    public void setAliveChance(final float aliveChance) {
        this.aliveChance = aliveChance;
    }

    /** @return if true, some alive cells will be spawned before the generation. */
    public boolean isInitiating() {
        return initiate;
    }

    /** @param initiate if true, some alive cells will be spawned before the generation. The others will be killed, if
     *            their value is higher than {@link #getMarker()}. */
    public void setInitiate(final boolean initiate) {
        this.initiate = initiate;
    }

    /** @return determines how far the cells can be from a cell to be considered neighbors. */
    public int getRadius() {
        return radius;
    }

    /** @param radius determines how far the cells can be from a cell to be considered neighbors. Defaults to 1 - only
     *            direct cell neighbors (sides + corners) are counted. */
    public void setRadius(final int radius) {
        this.radius = radius;
    }

    /** @return if cell is equal to or greater than this value, it is considered alive. When the cell dies, this value
     *         is subtracted from it. If the cell becomes alive, this value modifies the current cell value according to
     *         current mode. */
    public float getMarker() {
        return marker;
    }

    /** @param marker if cell is equal to or greater than this value, it is considered alive. When the cell dies, this
     *            value is subtracted from it. If the cell becomes alive, this value modifies the current cell value
     *            according to current mode.
     * @see #setMode(com.github.czyzby.noise4j.map.generator.Generator.GenerationMode) */
    public void setMarker(final float marker) {
        this.marker = marker;
    }

    /** @return dead cell becomes alive if it has more alive neighbors than this value. */
    public int getBirthLimit() {
        return birthLimit;
    }

    /** @param birthLimit dead cell becomes alive if it has more alive neighbors than this value. The lesser this value
     *            is, the more alive cells will be present. */
    public void setBirthLimit(final int birthLimit) {
        this.birthLimit = birthLimit;
    }

    /** @return living cell dies if it has less alive neighbors than this value. */
    public int getDeathLimit() {
        return deathLimit;
    }

    /** @param deathLimit living cell dies if it has less alive neighbors than this value. The higher this value is, the
     *            less smooth the map becomes. */
    public void setDeathLimit(final int deathLimit) {
        this.deathLimit = deathLimit;
    }

    @Override
    public void generate(final Grid grid) {
        if (initiate) {
            spawnLivingCells(grid);
        }
        // Grid is copied to keep the correct living neighbors count. Otherwise it would change during iterations.
        temporaryGrid = grid.copy();
        for (int iterationIndex = 0; iterationIndex < iterationsAmount; iterationIndex++) {
            grid.forEach(this);
            grid.set(temporaryGrid);
        }
    }

    /** @param grid some of its cells will become alive, according to the current chance settings. The others will die,
     *            if they were already alive.
     * @see #getAliveChance() */
    protected void spawnLivingCells(final Grid grid) {
        initiate(grid, aliveChance, marker);
    }

    /** @param grid unlike in case of noise algorithm, for example, cellular automata generator does not use an initial
     *            random seed (yet) which can be easily used to recreate the same map over and over. Unless you use a
     *            custom way of initiating the grid using a seed, the easiest solution to recreate exactly the same map
     *            is saving both generator settings and initial cell values before first iteration. By manually calling
     *            this method, you can copy the cell values before iterations begin; since the map is already initiated,
     *            it makes sense to turn off automatic initiation with {@link #setInitiate(boolean)} method.
     * @param generator its settings will be used. */
    public static void initiate(final Grid grid, final CellularAutomataGenerator generator) {
        initiate(grid, generator.getAliveChance(), generator.getMarker());
    }

    /** @param grid unlike in case of noise algorithm, for example, cellular automata generator does not use an initial
     *            random seed (yet) which can be easily used to recreate the same map over and over. Unless you use a
     *            custom way of initiating the grid using a seed, the easiest solution to recreate exactly the same map
     *            is saving both generator settings and initial cell values before first iteration. By manually calling
     *            this method, you can copy the cell values before iterations begin; since the map is already initiated,
     *            it makes sense to turn off automatic initiation with {@link #setInitiate(boolean)} method.
     * @param aliveChance see {@link #setAliveChance(float)}.
     * @param marker see {@link #setMarker(float)}. If value is already above the marker and rolled as alive, its value
     *            will not be changed. If cell's value is above the marker and it is rolled as dead, marker will be
     *            subtracted from its value. */
    public static void initiate(final Grid grid, final float aliveChance, final float marker) {
        final Random random = Generators.getRandom();
        final float[] array = grid.getArray();
        for (int index = 0, length = array.length; index < length; index++) {
            if (random.nextFloat() > aliveChance) {
                if (array[index] < marker) {
                    grid.add(grid.toX(index), grid.toY(index), marker);
                }
            } else if (array[index] >= marker) { // Is alive - killing it.
                grid.subtract(grid.toX(index), grid.toY(index), marker);
            }
        }
    }

    /** @return temporary grid, copied to preserve to correct amounts of living neighbors during iterations. */
    protected Grid getTemporaryGrid() {
        return temporaryGrid;
    }

    @Override
    public boolean consume(final Grid grid, final int x, final int y, final float value) {
        final int livingNeighbors = countLivingNeighbors(grid, x, y);
        if (isAlive(value)) {
            if (shouldDie(livingNeighbors)) {
                setDead(x, y);
            }
        } else if (shouldBeBorn(livingNeighbors)) {
            setAlive(x, y);
        }
        return CONTINUE;
    }

    /** Makes the cell alive in temporary cached grid copy.
     *
     * @param x column index of temporary grid.
     * @param y row index of temporary grid. */
    protected void setAlive(final int x, final int y) {
        modifyCell(temporaryGrid, x, y, marker);
    }

    /** Kills the cell in temporary cached grid copy.
     *
     * @param x column index of temporary grid.
     * @param y row index of temporary grid. */
    protected void setDead(final int x, final int y) {
        temporaryGrid.subtract(x, y, marker);
    }

    /** @param aliveNeighbors amount of alive tile's neighbors.
     * @return true if tile has less alive neighbors than the current death limit. */
    protected boolean shouldDie(final int aliveNeighbors) {
        return aliveNeighbors < deathLimit;
    }

    /** @param aliveNeighbors amount of alive tile's neighbors.
     * @return true if tile has more alive neighbors than the current birth limit. */
    protected boolean shouldBeBorn(final int aliveNeighbors) {
        return aliveNeighbors > birthLimit;
    }

    /** @param value current cell's value.
     * @return true if the cell is currently considered alive. */
    protected boolean isAlive(final float value) {
        return value >= marker;
    }

    /** @param grid processed grid.
     * @param x column index of a cell.
     * @param y row index of a cell.
     * @return amount of neighbor cells that are considered alive. */
    protected int countLivingNeighbors(final Grid grid, final int x, final int y) {
        int count = 0;
        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int yOffset = -radius; yOffset <= radius; yOffset++) {
                if (xOffset == 0 && yOffset == 0) {
                    continue;// Same tile.
                }
                final int neighborX = x + xOffset;
                final int neighborY = y + yOffset;
                if (grid.isIndexValid(neighborX, neighborY) && isAlive(grid.get(neighborX, neighborY))) {
                    count++;
                }
            }
        }
        return count;
    }
}
