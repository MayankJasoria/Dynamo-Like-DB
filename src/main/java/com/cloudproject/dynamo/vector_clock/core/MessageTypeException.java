package com.cloudproject.dynamo.vector_clock.core;

/**
 * Thrown when a type mismatch error occurs
 */
public class MessageTypeException
        extends MessagePackException
{
    public MessageTypeException()
    {
        super();
    }

    public MessageTypeException(String message)
    {
        super(message);
    }

    public MessageTypeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MessageTypeException(Throwable cause)
    {
        super(cause);
    }
}
