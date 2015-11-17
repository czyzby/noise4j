package com.github.czyzby.noise4j.map.generator.noise;

import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.Grid.CellConsumer;
import com.github.czyzby.noise4j.map.generator.AbstractGenerator;
import com.github.czyzby.noise4j.map.generator.util.Generators;

/** Divides grid into equal regions. Assigns semi-random value to each region using a noise function. Interpolates the
 * value according to neighbor regions' values. Unless regions are too small, this usually results in a smooth map with
 * logical region transitions. During map generation, you usually trigger the {@link #getRadius()} and
 * {@link #getModifier()}, invoking generation multiple times - each time with lower modifier. This allows to generate a
 * map with logical transitions, while keeping the map interesting thanks to further iterations with lower radius.
 *
 * @author MJ */
public class NoiseGenerator extends AbstractGenerator implements CellConsumer {
    private static NoiseGenerator INSTANCE;

    private NoiseAlgorithmProvider algorithmProvider = new DefaultNoiseAlgorithmProvider();
    private int radius;
    private float modifier;
    private int seed;

    /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or
     * obtaining an instance of the generator is generally preferred.
     *
     * @param grid will contain generated values.
     * @param radius {@link #setRadius(int)}
     * @param modifier {@link #setModifier(float)} */
    public static void generate(final Grid grid, final int radius, final float modifier) {
        generate(grid, radius, modifier, Generators.rollSeed());
    }

    /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or
     * obtaining an instance of the generator is generally preferred.
     *
     * @param grid will contain generated values.
     * @param radius {@link #setRadius(int)}
     * @param modifier {@link #setModifier(float)}
     * @param seed {@link #setSeed(int)} */
    private static void generate(final Grid grid, final int radius, final float modifier, final int seed) {
        final NoiseGenerator generator = getInstance();
        generator.setRadius(radius);
        generator.setModifier(modifier);
        generator.setSeed(seed);
        generator.generate(grid);
    }

    /** @return static instance of the generator. Not thread-safe. */
    public static NoiseGenerator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoiseGenerator();
        }
        return INSTANCE;
    }

    /** @return size of a single generation region. The bigger, the more smooth the map seems. Setting it to one
     *         effectively turns the map into semi-random noise with no smoothing whatsoever. */
    public int getRadius() {
        return radius;
    }

    /** @param radius size of a single generation region. The bigger, the more smooth the map seems. Setting it to one
     *            effectively turns the map into semi-random noise with no smoothing whatsoever. */
    public void setRadius(final int radius) {
        this.radius = radius;
    }

    /** @return prime number. Random seed used by the noise function. */
    public int getSeed() {
        return seed;
    }

    /** @param seed prime number. Random seed used by the noise function. */
    public void setSeed(final int seed) {
        this.seed = seed;
    }

    /** @return relevance of the generation stage. Each grid cell will be increased with a semi-random value on scale
     *         from 0 to this modifier. */
    public float getModifier() {
        return modifier;
    }

    /** @param modifier relevance of the generation stage. Each grid cell will be increased with a semi-random value on
     *            scale from 0 to this modifier. */
    public void setModifier(final float modifier) {
        this.modifier = modifier;
    }

    /** @param algorithmProvider handles interpolation and noise math.
     * @see DefaultNoiseAlgorithmProvider */
    public void setAlgorithmProvider(final NoiseAlgorithmProvider algorithmProvider) {
        this.algorithmProvider = algorithmProvider;
    }

    @Override
    public void generate(final Grid grid) {
        if (seed == 0) {
            setSeed(Generators.rollSeed());
        }
        grid.forEach(this);
    }

    @Override
    public boolean consume(final Grid grid, final int x, final int y, final float value) {
        // Region index:
        final int regionX = x / radius;
        final int regionY = y / radius;
        // Distance from the start of the region:
        final float factorialX = x / (float) radius - regionX;
        final float factorialY = y / (float) radius - regionY;
        // Generated noises. Top and left noises are handled (already interpolated) by the other neighbors.
        final float noiseCenter = algorithmProvider.smoothNoise(this, regionX, regionY);
        final float noiseRight = algorithmProvider.smoothNoise(this, regionX + 1, regionY);
        final float noiseBottom = algorithmProvider.smoothNoise(this, regionX, regionY + 1);
        final float noiseBottomRight = algorithmProvider.smoothNoise(this, regionX + 1, regionY + 1);
        // Noise interpolations:
        final float topInterpolation = algorithmProvider.interpolate(noiseCenter, noiseRight, factorialX);
        final float bottomInterpolation = algorithmProvider.interpolate(noiseBottom, noiseBottomRight, factorialX);
        final float finalInterpolation = algorithmProvider.interpolate(topInterpolation, bottomInterpolation,
                factorialY);
        // Modifying current cell value according to the generation mode:
        modifyCell(grid, x, y, (finalInterpolation + 1f) / 2f * modifier);
        return CONTINUE;
    }

    /** Interface providing functions necessary for map generation.
     *
     * @author MJ */
    public interface NoiseAlgorithmProvider {
        /** Semi-random function. Consumes two parameters, always returning the same result for the same set of numbers.
         *
         * @param generator its settings should be honored. Random seed is usually used for the noise calculation.
         * @param x position on the X axis.
         * @param y position on the Y axis.
         * @return noise value. */
        float noise(NoiseGenerator generator, int x, int y);

        /** Semi-random function. Consumes two parameters, always returning the same result for the same set of numbers.
         * Noise is dependent on the neighbors, ensuring that drastic changes are rather rare.
         *
         * @param generator its settings should be honored. Random seed is usually used for the noise calculation.
         * @param x position on the X axis.
         * @param y position on the Y axis.
         * @return smoothed noise value. */
        float smoothNoise(NoiseGenerator generator, int x, int y);

        /** @param start range start.
         * @param end range end.
         * @param factorial [0,1), distance of the current point from the start.
         * @return interpolated current value, [start,end]. */
        float interpolate(float start, float end, float factorial);
    }

    /** Uses a custom noise function and cos interpolation.
     *
     * @author MJ */
    public static class DefaultNoiseAlgorithmProvider implements NoiseAlgorithmProvider {
        private static final float PI = (float) Math.PI;

        // HERE BE DRAGONS. AND MAGIC NUMBERS.
        @Override
        public float noise(final NoiseGenerator generator, final int x, final int y) {
            final int n = x + generator.getSeed() + y * generator.getSeed();
            return 1.0f - (n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff) / 1073741824.0f;
        }

        @Override
        public float smoothNoise(final NoiseGenerator generator, final int x, final int y) {
            return // Corners:
            (noise(generator, x - 1, y - 1) + noise(generator, x + 1, y - 1) + noise(generator, x - 1, y + 1)
                    + noise(generator, x + 1, y + 1)) / 16f
                    // Sides:
                    + (noise(generator, x - 1, y) + noise(generator, x + 1, y) + noise(generator, x, y - 1)
                            + noise(generator, x, y + 1)) / 8f
                            // Center:
                    + noise(generator, x, y) / 4f;
        }

        @Override
        public float interpolate(final float start, final float end, final float factorial) {
            final float modificator = (1f - Generators.getCalculator().cos(factorial * PI)) * 0.5f;
            return start * (1f - modificator) + end * modificator;
        }
    }
}
