package com.cloudproject.dynamo.vector_clock.value;

/**
 * Immutable base interface of {@link ImmutableIntegerValue} and {@link ImmutableFloatValue} interfaces. To extract primitive type values, call toXXX methods, which may lose some information by rounding or truncation.
 *
 * @see ImmutableIntegerValue
 * @see ImmutableFloatValue
 */
public interface ImmutableNumberValue
        extends NumberValue, ImmutableValue
{
}
