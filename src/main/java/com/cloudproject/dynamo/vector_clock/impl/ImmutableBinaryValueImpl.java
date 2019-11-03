package com.cloudproject.dynamo.vector_clock.impl;

import com.cloudproject.dynamo.vector_clock.core.MessagePacker;
import com.cloudproject.dynamo.vector_clock.value.ImmutableBinaryValue;
import com.cloudproject.dynamo.vector_clock.value.Value;
import com.cloudproject.dynamo.vector_clock.value.ValueType;
import com.cloudproject.dynamo.vector_clock.value.StringValue;

import java.io.IOException;
import java.util.Arrays;

/**
 * {@code ImmutableBinaryValueImpl} Implements {@code ImmutableBinaryValue} using a {@code byte[]} field.
 * This implementation caches result of {@code toString()} and {@code asString()} using a private {@code String} field.
 *
 * @see StringValue
 */
public class ImmutableBinaryValueImpl
        extends AbstractImmutableRawValue
        implements ImmutableBinaryValue
{
    public ImmutableBinaryValueImpl(byte[] data)
    {
        super(data);
    }

    @Override
    public ValueType getValueType()
    {
        return ValueType.BINARY;
    }

    @Override
    public ImmutableBinaryValue immutableValue()
    {
        return this;
    }

    @Override
    public ImmutableBinaryValue asBinaryValue()
    {
        return this;
    }

    @Override
    public void writeTo(MessagePacker pk)
            throws IOException
    {
        pk.packBinaryHeader(data.length);
        pk.writePayload(data);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Value)) {
            return false;
        }
        Value v = (Value) o;
        if (!v.isBinaryValue()) {
            return false;
        }

        if (v instanceof ImmutableBinaryValueImpl) {
            ImmutableBinaryValueImpl bv = (ImmutableBinaryValueImpl) v;
            return Arrays.equals(data, bv.data);
        }
        else {
            return Arrays.equals(data, v.asBinaryValue().asByteArray());
        }
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(data);
    }
}
