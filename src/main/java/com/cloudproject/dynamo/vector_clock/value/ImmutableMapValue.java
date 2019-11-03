package com.cloudproject.dynamo.vector_clock.value;

/**
 * Immutable representation of MessagePack's Map type.
 *
 * MessagePack's Map type can represent sequence of key-value pairs.
 */
public interface ImmutableMapValue
        extends MapValue, ImmutableValue
{
}
