package com.cloudproject.dynamo.vector_clock.value;

/**
 * Immutable representation of MessagePack's Binary type.
 *
 * MessagePack's Binary type can represent a byte array at most 2<sup>64</sup>-1 bytes.
 *
 * @see ImmutableRawValue
 */
public interface ImmutableBinaryValue
        extends BinaryValue, ImmutableRawValue
{
}
