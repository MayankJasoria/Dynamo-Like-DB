package com.cloudproject.dynamo.vector_clock.value;

/**
 * Immutable representation of MessagePack's Boolean type.
 *
 * MessagePack's Boolean type can represent {@code true} or {@code false}.
 */
public interface ImmutableBooleanValue
        extends BooleanValue, ImmutableValue
{
}
