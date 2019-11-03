package com.cloudproject.dynamo.vector_clock.value;

/**
 * Representation of MessagePack's Binary type.
 *
 * MessagePack's Binary type can represent a byte array at most 2<sup>64</sup>-1 bytes.
 *
 * @see RawValue
 */
public interface BinaryValue
        extends RawValue
{
}
