package com.cloudproject.dynamo.vector_clock.core.buffer;

import com.cloudproject.dynamo.vector_clock.core.Preconditions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;


/**
 * {@link MessageBufferInput} adapter for {@link InputStream}
 */
public class InputStreamBufferInput
        implements MessageBufferInput
{
    private InputStream in;
    private final byte[] buffer;

    public static MessageBufferInput newBufferInput(InputStream in)
    {
        Preconditions.checkNotNull(in, "InputStream is null");
        if (in instanceof FileInputStream) {
            FileChannel channel = ((FileInputStream) in).getChannel();
            if (channel != null) {
                return new ChannelBufferInput(channel);
            }
        }
        return new InputStreamBufferInput(in);
    }

    public InputStreamBufferInput(InputStream in)
    {
        this(in, 8192);
    }

    public InputStreamBufferInput(InputStream in, int bufferSize)
    {
        this.in = Preconditions.checkNotNull(in, "input is null");
        this.buffer = new byte[bufferSize];
    }

    /**
     * Reset Stream. This method doesn't close the old resource.
     *
     * @param in new stream
     * @return the old resource
     */
    public InputStream reset(InputStream in)
            throws IOException
    {
        InputStream old = this.in;
        this.in = in;
        return old;
    }

    @Override
    public MessageBuffer next()
            throws IOException
    {
        int readLen = in.read(buffer);
        if (readLen == -1) {
            return null;
        }
        return MessageBuffer.wrap(buffer, 0, readLen);
    }

    @Override
    public void close()
            throws IOException
    {
        in.close();
    }
}
