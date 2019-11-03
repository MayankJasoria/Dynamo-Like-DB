package com.cloudproject.dynamo.vector_clock.value;

import java.nio.ByteBuffer;

/**
 * Base interface of {@link StringValue} and {@link BinaryValue} interfaces.
 * <p/>
 * MessagePack's Raw type can represent a byte array at most 2<sup>64</sup>-1 bytes.
 *
 * @see StringValue
 * @see BinaryValue
 */
public interface RawValue
        extends Value
{
    /**
     * Returns the value as {@code byte[]}.
     *
     * This method copies the byte array.
     */
    byte[] asByteArray();

    /**
     * Returns the value as {@code ByteBuffer}.
     *
     * Returned ByteBuffer is read-only. See also {@link java.nio.ByteBuffer#asReadOnlyBuffer()}.
     * This method doesn't copy the byte array as much as possible.
     */
    ByteBuffer asByteBuffer();

    /**
     * Returns the value as {@code String}.
     *
     * This method throws an exception if the value includes invalid UTF-8 byte sequence.
     *
     * @throws MessageStringCodingException If this value includes invalid UTF-8 byte sequence.
     */
    String asString();

    /**
     * Returns the value as {@code String}.
     *
     * This method replaces an invalid UTF-8 byte sequence with <code>U+FFFD replacement character</code>.
     */
    String toString();
}
