package com.cloudproject.dynamo.vector_clock.impl;

import com.cloudproject.dynamo.vector_clock.core.MessagePacker;
import com.cloudproject.dynamo.vector_clock.value.ImmutableBooleanValue;
import com.cloudproject.dynamo.vector_clock.value.BooleanValue;
import com.cloudproject.dynamo.vector_clock.value.Value;
import com.cloudproject.dynamo.vector_clock.value.ValueType;

import java.io.IOException;

/**
 * {@code ImmutableBooleanValueImpl} Implements {@code ImmutableBooleanValue} using a {@code boolean} field.
 *
 * This class is a singleton. {@code ImmutableBooleanValueImpl.trueInstance()} and {@code ImmutableBooleanValueImpl.falseInstance()} are the only instances of this class.
 *
 * @see BooleanValue
 */
public class ImmutableBooleanValueImpl
        extends AbstractImmutableValue
        implements ImmutableBooleanValue
{
    public static final ImmutableBooleanValue TRUE = new ImmutableBooleanValueImpl(true);
    public static final ImmutableBooleanValue FALSE = new ImmutableBooleanValueImpl(false);

    private final boolean value;

    private ImmutableBooleanValueImpl(boolean value)
    {
        this.value = value;
    }

    @Override
    public ValueType getValueType()
    {
        return ValueType.BOOLEAN;
    }

    @Override
    public ImmutableBooleanValue asBooleanValue()
    {
        return this;
    }

    @Override
    public ImmutableBooleanValue immutableValue()
    {
        return this;
    }

    @Override
    public boolean getBoolean()
    {
        return value;
    }

    @Override
    public void writeTo(MessagePacker packer)
            throws IOException
    {
        packer.packBoolean(value);
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
        Value v = (Value) o;

        if (!v.isBooleanValue()) {
            return false;
        }
        return value == v.asBooleanValue().getBoolean();
    }

    @Override
    public int hashCode()
    {
        if (value) {
            return 1231;
        }
        else {
            return 1237;
        }
    }

    @Override
    public String toJson()
    {
        return Boolean.toString(value);
    }

    @Override
    public String toString()
    {
        return toJson();
    }
}
