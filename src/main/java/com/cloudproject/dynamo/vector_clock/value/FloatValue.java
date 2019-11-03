package com.cloudproject.dynamo.vector_clock.value;

/**
 * Representation of MessagePack's Float type.
 *
 * MessagePack's Float type can represent IEEE 754 double precision floating point numbers including NaN and infinity. This is same with Java's {@code double} type.
 *
 * @see NumberValue
 */
public interface FloatValue
        extends NumberValue
{
}
