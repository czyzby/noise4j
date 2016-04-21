package com.github.czyzby.noise4j.map.generator.util;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

/** Utilities for map generators.
 *
 * <p>
 * When used in LibGDX applications, it is a good idea to replace {@link Random} and {@link Calculator} instances with
 * values and methods of {@code MathUtils} class, which provides both more efficient random implementation and sin/cos
 * look-up tables. See {@link #setRandom(Random)} and {@link #setCalculator(Calculator)}. This should be done before
 * using any generators - see example below:
 *
 * <blockquote>
 *
 * <pre>
 * Generators.setRandom(MathUtils.random);
 * Generators.setCalculator(new Calculator() {
 *     public float sin(float radians) {
 *         return MathUtils.sin(radians);
 *     }
 *
 *     public float cos(float radians) {
 *         return MathUtils.cos(radians);
 *     }
 * });
 * </pre>
 *
 * </blockquote>
 *
 * @author MJ */
public class Generators {
    /** Length of generated random seeds with {@link #rollSeed()}. Depending on the algorithm, this value might vary -
     * in which case {@link #rollSeed(int)} method should be used instead. */
    public static final int DEFAULT_SEED_BIT_LENGTH = 16;

    private static Random RANDOM;
    private static Calculator CALCULATOR;

    private Generators() {
    }

    /** @return {@link Random} instance shared by the generators. Not thread-safe, unless modified with
     *         {@link #setRandom(Random)}. */
    public static Random getRandom() {
        if (RANDOM == null) {
            RANDOM = new Random();
        }
        return RANDOM;
    }

    /** @param random will be available through {@link #getRandom()} instance. This method allows to provide a
     *            thread-safe, secure or specialized random instance. In LibGDX applications, you'd generally want to
     *            use MathUtils.random instance. */
    public static void setRandom(final Random random) {
        RANDOM = random;
    }

    /** @return static instance of immutable, thread-safe {@link Calculator}, providing common math functions. */
    public static Calculator getCalculator() {
        if (CALCULATOR == null) {
            CALCULATOR = new Calculator() {
                @Override
                public float sin(final float radians) {
                    return (float) Math.sin(radians);
                }

                @Override
                public float cos(final float radians) {
                    return (float) Math.cos(radians);
                }
            };
        }
        return CALCULATOR;
    }

    /** @param calculator instance of immutable, thread-safe {@link Calculator}, providing common math functions. */
    public static void setCalculator(final Calculator calculator) {
        CALCULATOR = calculator;
    }

    /** @return a random probable prime with {@link #DEFAULT_SEED_BIT_LENGTH} bits.
     * @see BigInteger#probablePrime(int, Random) */
    public static int rollSeed() {
        return rollSeed(DEFAULT_SEED_BIT_LENGTH);
    }

    /** @param seedBitLength bits lenths of the generated seed.
     * @return a random probable prime.
     * @see BigInteger#probablePrime(int, Random) */
    public static int rollSeed(final int seedBitLength) {
        return BigInteger.probablePrime(seedBitLength, getRandom()).intValue();
    }

    /** @param min minimum possible random value.
     * @param max maximum possible random value.
     * @return random value in the specified range. */
    public static int randomInt(final int min, final int max) {
        return min + getRandom().nextInt(max - min + 1);
    }

    /** @param list a list of elements. Cannot be null or empty.
     * @return random list element.
     * @param <Type> type of stored elements. */
    public static <Type> Type randomElement(final List<Type> list) {
        return list.get(randomIndex(list));
    }

    /** @param list a list of elements. Cannot be null or empty.
     * @return random index of an element stored in the list. */
    public static int randomIndex(final List<?> list) {
        return getRandom().nextInt(list.size());
    }

    /** @return a random float in range of 0f (inclusive) to 1f (exclusive). */
    public static float randomPercent() {
        return getRandom().nextFloat();
    }

    /** GWT-compatible collection shuffling method. Use only for lists with quick random access; use
     * {@link java.util.Collections#shuffle(List)} if not targeting GWT.
     *
     * @param list its elements will be shuffled.
     * @return passed list, for chaining.
     * @param <Type> type of elements stored in the list. */
    public static <Type> List<Type> shuffle(final List<Type> list) {
        final Random random = getRandom();
        int swap;
        for (int i = list.size(); i > 1; i--) {
            swap = random.nextInt(i);
            list.set(swap, list.set(i - 1, list.get(swap)));
        }
        return list;
    }

    /** Allows to calculate common generators' functions. By implementing this interface, you can replace these common
     * functions with a more efficient solutions - for example, a look-up table. In LibGDX applications, MathUtils class
     * can be used for these operations.
     *
     * <p>
     * Default implementation uses {@link Math} methods.
     *
     * @author MJ
     * @see Generators#setCalculator(Calculator) */
    public static interface Calculator {
        /** @param radians angle in radians.
         * @return sin value.
         * @see Math#sin(double) */
        float sin(float radians);

        /** @param radians angle in radians.
         * @return cos value.
         * @see Math#cos(double) */
        float cos(float radians);
    }
}
