package com.cloudproject.dynamo.vector_clock.value;

/**
 * Immutable base interface of {@link ImmutableStringValue} and {@link ImmutableBinaryValue} interfaces.
 * <p/>
 * MessagePack's Raw type can represent a byte array at most 2<sup>64</sup>-1 bytes.
 *
 * @see ImmutableStringValue
 * @see ImmutableBinaryValue
 */
public interface ImmutableRawValue
        extends RawValue, ImmutableValue
{
}
