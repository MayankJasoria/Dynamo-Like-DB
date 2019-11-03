package com.cloudproject.dynamo.vector_clock.value;

/**
 * Representation MessagePack's Boolean type.
 *
 * MessagePack's Boolean type can represent {@code true} or {@code false}.
 */
public interface BooleanValue
        extends Value
{
    /**
     * Returns the value as a {@code boolean}.
     */
    boolean getBoolean();
}
