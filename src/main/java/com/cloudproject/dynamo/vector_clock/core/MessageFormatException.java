package com.cloudproject.dynamo.vector_clock.core;

/**
 * Thrown when the input message pack format is invalid
 */
public class MessageFormatException
        extends MessagePackException
{
    public MessageFormatException(Throwable e)
    {
        super(e);
    }

    public MessageFormatException(String message)
    {
        super(message);
    }

    public MessageFormatException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
