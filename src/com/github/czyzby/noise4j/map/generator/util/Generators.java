package com.github.czyzby.noise4j.map.generator.util;

import java.math.BigInteger;
import java.util.Random;

/** Utilities for map generators.
 *
 * <p>
 * When used in LibGDX applications, it is a good idea to replace {@link Random} and {@link Calculator} instance with
 * values and methods of MathUtils, which provides both more efficient random implementation and sin/cos look-up tables.
 * See {@link #setRandom(Random)} and {@link #setCalculator(Calculator)}.
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
