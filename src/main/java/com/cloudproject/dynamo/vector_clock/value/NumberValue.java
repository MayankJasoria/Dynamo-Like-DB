package com.cloudproject.dynamo.vector_clock.value;

import java.math.BigInteger;

/**
 * Base interface of {@link IntegerValue} and {@link FloatValue} interfaces. To extract primitive type values, call toXXX methods, which may lose some information by rounding or truncation.
 *
 * @see IntegerValue
 * @see FloatValue
 */
public interface NumberValue
        extends Value
{
    /**
     * Represent this value as a byte value, which may involve rounding or truncation of the original value.
     * the value.
     */
    byte toByte();

    /**
     * Represent this value as a short value, which may involve rounding or truncation of the original value.
     */
    short toShort();

    /**
     * Represent this value as an int value, which may involve rounding or truncation of the original value.
     * value.
     */
    int toInt();

    /**
     * Represent this value as a long value, which may involve rounding or truncation of the original value.
     */
    long toLong();

    /**
     * Represent this value as a BigInteger, which may involve rounding or truncation of the original value.
     */
    BigInteger toBigInteger();

    /**
     * Represent this value as a 32-bit float value, which may involve rounding or truncation of the original value.
     */
    float toFloat();

    /**
     * Represent this value as a 64-bit double value, which may involve rounding or truncation of the original value.
     */
    double toDouble();
}
