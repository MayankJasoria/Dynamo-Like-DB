package com.cloudproject.dynamo.vector_clock.buffer;

import com.cloudproject.dynamo.vector_clock.core.Preconditions;

import java.nio.ByteBuffer;

import static com.cloudproject.dynamo.vector_clock.core.Preconditions.checkNotNull;

/**
 * {@link MessageBufferInput} adapter for {@link java.nio.ByteBuffer}
 */
public class ByteBufferInput
        implements MessageBufferInput
{
    private ByteBuffer input;
    private boolean isRead = false;

    public ByteBufferInput(ByteBuffer input)
    {
        this.input = Preconditions.checkNotNull(input, "input ByteBuffer is null").slice();
    }

    /**
     * Reset buffer.
     *
     * @param input new buffer
     * @return the old buffer
     */
    public ByteBuffer reset(ByteBuffer input)
    {
        ByteBuffer old = this.input;
        this.input = Preconditions.checkNotNull(input, "input ByteBuffer is null").slice();
        isRead = false;
        return old;
    }

    @Override
    public MessageBuffer next()
    {
        if (isRead) {
            return null;
        }

        MessageBuffer b = MessageBuffer.wrap(input);
        isRead = true;
        return b;
    }

    @Override
    public void close()
    {
        // Nothing to do
    }
}
