package com.cloudproject.dynamo.vector_clock.core;

/**
 * Thrown to indicate too large message size (e.g, larger than 2^31-1).
 */
public class MessageSizeException
        extends MessagePackException
{
    private final long size;

    public MessageSizeException(long size)
    {
        super();
        this.size = size;
    }

    public MessageSizeException(String message, long size)
    {
        super(message);
        this.size = size;
    }

    public long getSize()
    {
        return size;
    }
}
