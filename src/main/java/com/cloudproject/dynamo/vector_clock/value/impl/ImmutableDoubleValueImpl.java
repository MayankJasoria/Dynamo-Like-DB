package com.cloudproject.dynamo.vector_clock.value.impl;

import com.cloudproject.dynamo.vector_clock.value.*;
import com.cloudproject.dynamo.vector_clock.core.MessagePacker;
import com.cloudproject.dynamo.vector_clock.value.*;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * {@code ImmutableDoubleValueImpl} Implements {@code ImmutableFloatValue} using a {@code double} field.
 *
 * @see FloatValue
 */
public class ImmutableDoubleValueImpl
        extends AbstractImmutableValue
        implements ImmutableFloatValue
{
    private final double value;

    public ImmutableDoubleValueImpl(double value)
    {
        this.value = value;
    }

    @Override
    public ValueType getValueType()
    {
        return ValueType.FLOAT;
    }

    @Override
    public ImmutableDoubleValueImpl immutableValue()
    {
        return this;
    }

    @Override
    public ImmutableNumberValue asNumberValue()
    {
        return this;
    }

    @Override
    public ImmutableFloatValue asFloatValue()
    {
        return this;
    }

    @Override
    public byte toByte()
    {
        return (byte) value;
    }

    @Override
    public short toShort()
    {
        return (short) value;
    }

    @Override
    public int toInt()
    {
        return (int) value;
    }

    @Override
    public long toLong()
    {
        return (long) value;
    }

    @Override
    public BigInteger toBigInteger()
    {
        return new BigDecimal(value).toBigInteger();
    }

    @Override
    public float toFloat()
    {
        return (float) value;
    }

    @Override
    public double toDouble()
    {
        return value;
    }

    @Override
    public void writeTo(MessagePacker pk)
            throws IOException
    {
        pk.packDouble(value);
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

        if (!v.isFloatValue()) {
            return false;
        }
        return value == v.asFloatValue().toDouble();
    }

    @Override
    public int hashCode()
    {
        long v = Double.doubleToLongBits(value);
        return (int) (v ^ (v >>> 32));
    }

    @Override
    public String toJson()
    {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "null";
        }
        else {
            return Double.toString(value);
        }
    }

    @Override
    public String toString()
    {
        return Double.toString(value);
    }
}
