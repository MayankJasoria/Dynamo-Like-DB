package com.cloudproject.dynamo.vector_clock.value.impl;

import com.cloudproject.dynamo.vector_clock.core.MessagePacker;
import com.cloudproject.dynamo.vector_clock.value.ImmutableStringValue;
import com.cloudproject.dynamo.vector_clock.value.Value;
import com.cloudproject.dynamo.vector_clock.value.ValueType;
import com.cloudproject.dynamo.vector_clock.value.StringValue;

import java.io.IOException;
import java.util.Arrays;

/**
 * {@code ImmutableStringValueImpl} Implements {@code ImmutableStringValue} using a {@code byte[]} field.
 * This implementation caches result of {@code toString()} and {@code asString()} using a private {@code String} field.
 *
 * @see StringValue
 */
public class ImmutableStringValueImpl
        extends AbstractImmutableRawValue
        implements ImmutableStringValue
{
    public ImmutableStringValueImpl(byte[] data)
    {
        super(data);
    }

    public ImmutableStringValueImpl(String string)
    {
        super(string);
    }

    @Override
    public ValueType getValueType()
    {
        return ValueType.STRING;
    }

    @Override
    public ImmutableStringValue immutableValue()
    {
        return this;
    }

    @Override
    public ImmutableStringValue asStringValue()
    {
        return this;
    }

    @Override
    public void writeTo(MessagePacker pk)
            throws IOException
    {
        pk.packRawStringHeader(data.length);
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
        if (!v.isStringValue()) {
            return false;
        }

        if (v instanceof ImmutableStringValueImpl) {
            ImmutableStringValueImpl bv = (ImmutableStringValueImpl) v;
            return Arrays.equals(data, bv.data);
        }
        else {
            return Arrays.equals(data, v.asStringValue().asByteArray());
        }
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(data);
    }
}
