package com.cloudproject.dynamo.vector_clock.value;

/**
 * Immutable representation of MessagePack's Float type.
 *
 * MessagePack's Float type can represent IEEE 754 double precision floating point numbers including NaN and infinity. This is same with Java's {@code double} type.
 *
 * @see ImmutableNumberValue
 */
public interface ImmutableFloatValue
        extends FloatValue, ImmutableNumberValue
{
}
