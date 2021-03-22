// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.utils;

public final class TypeUtils {
    private static final int INTEGER_BITS_LENGTH = 32;
    private static final long INTEGER_BITS_MASK = 0xffffffffL;

    private TypeUtils() {
    }

    public static int getLongHighBits(long value) {
        return (int) (value >> INTEGER_BITS_LENGTH);
    }

    public static int getLongLowBits(long value) {
        return (int) value;
    }

    public static long getLongFromInts(int valueHigh, int valueLow) {
        return (((long) valueHigh) << INTEGER_BITS_LENGTH)
            | (((long) valueLow) & INTEGER_BITS_MASK);
    }
}
