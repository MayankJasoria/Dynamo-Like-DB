package com.cloudproject.dynamo.vector_clock.core;

public class MessageTypeCastException
        extends MessageTypeException
{
    public MessageTypeCastException()
    {
        super();
    }

    public MessageTypeCastException(String message)
    {
        super(message);
    }

    public MessageTypeCastException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MessageTypeCastException(Throwable cause)
    {
        super(cause);
    }
}
