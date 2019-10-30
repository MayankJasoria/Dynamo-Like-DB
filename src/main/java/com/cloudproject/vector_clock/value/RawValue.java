/*
 * MIT License
 *
 * Copyright (c) 2017 Distributed clocks
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

//
// MessagePack for Java
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
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
