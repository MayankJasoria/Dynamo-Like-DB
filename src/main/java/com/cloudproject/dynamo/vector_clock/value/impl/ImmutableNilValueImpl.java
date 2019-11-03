package com.cloudproject.dynamo.vector_clock.value.impl;

import com.cloudproject.dynamo.vector_clock.core.MessagePacker;
import com.cloudproject.dynamo.vector_clock.value.ImmutableNilValue;
import com.cloudproject.dynamo.vector_clock.value.Value;
import com.cloudproject.dynamo.vector_clock.value.ValueType;
import com.cloudproject.dynamo.vector_clock.value.NilValue;

import java.io.IOException;

/**
 * {@code ImmutableNilValueImpl} Implements {@code ImmutableNilValue}.
 *
 * This class is a singleton. {@code ImmutableNilValueImpl.get()} is the only instances of this class.
 *
 * @see NilValue
 */
public class ImmutableNilValueImpl
        extends AbstractImmutableValue
        implements ImmutableNilValue
{
    private static ImmutableNilValue instance = new ImmutableNilValueImpl();

    public static ImmutableNilValue get()
    {
        return instance;
    }

    private ImmutableNilValueImpl()
    {
    }

    @Override
    public ValueType getValueType()
    {
        return ValueType.NIL;
    }

    @Override
    public ImmutableNilValue immutableValue()
    {
        return this;
    }

    @Override
    public ImmutableNilValue asNilValue()
    {
        return this;
    }

    @Override
    public void writeTo(MessagePacker pk)
            throws IOException
    {
        pk.packNil();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Value)) {
            return false;
        }
        return ((Value) o).isNilValue();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return toJson();
    }

    @Override
    public String toJson()
    {
        return "null";
    }
}
