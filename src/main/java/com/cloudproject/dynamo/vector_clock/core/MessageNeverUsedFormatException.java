package com.cloudproject.dynamo.vector_clock.core;

/**
 * Thrown when the input message pack format is invalid
 */
public class MessageNeverUsedFormatException
        extends MessageFormatException
{
    public MessageNeverUsedFormatException(Throwable e)
    {
        super(e);
    }

    public MessageNeverUsedFormatException(String message)
    {
        super(message);
    }

    public MessageNeverUsedFormatException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
