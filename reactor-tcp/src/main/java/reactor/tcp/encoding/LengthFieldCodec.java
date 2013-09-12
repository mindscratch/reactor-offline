/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.tcp.encoding;

import reactor.function.Consumer;
import reactor.function.Function;
import reactor.io.Buffer;
import reactor.util.Assert;

/**
 * A codec that uses a length-field at the start of each chunk to denote the chunk's size.
 * During decoding the delegate is used to process each chunk. During encoding the delegate
 * is used to encode each piece of output into a buffer. The buffer is then output, with its
 * length prepended.
 *
 * @param <IN> The type that will be produced by decoding
 * @param <OUT> The type that will be consumed by encoding
 *
 * @author Jon Brisbin
 */
public class LengthFieldCodec<IN, OUT> implements Codec<Buffer, IN, OUT> {

	private final int                    lengthFieldLength;
	private final Codec<Buffer, IN, OUT> delegate;

	/**
	 * Create a length-field codec that reads the first integer as the length of the
	 * remaining message.
	 *
	 * @param delegate The delegate {@link Codec}.
	 */
	public LengthFieldCodec(Codec<Buffer, IN, OUT> delegate) {
		this(4, delegate);
	}

	/**
	 * Create a length-field codec that reads either the first integer or the first long as the
	 * length of the remaining message, and prepends either an integer or long to its output.
	 *
	 * @param lengthFieldLength The size of the length field. Valid values are 4 (int) or 8 (long).
	 * @param delegate          The delegate {@link Codec}.
	 */
	public LengthFieldCodec(int lengthFieldLength, Codec<Buffer, IN, OUT> delegate) {
		Assert.state(lengthFieldLength == 4 || lengthFieldLength == 8, "lengthFieldLength should either be 4 (int) or 8 (long).");
		this.lengthFieldLength = lengthFieldLength;
		this.delegate = delegate;
	}

	@Override
	public Function<Buffer, IN> decoder(Consumer<IN> next) {
		return new LengthFieldDecoder(next);
	}

	@Override
	public Function<OUT, Buffer> encoder() {
		return new LengthFieldEncoder();
	}

	private class LengthFieldDecoder implements Function<Buffer, IN> {
		private final Function<Buffer, IN> decoder;

		private LengthFieldDecoder(Consumer<IN> next) {
			this.decoder = delegate.decoder(next);
		}

		@Override
		public IN apply(Buffer buffer) {
			while (buffer.remaining() > lengthFieldLength) {
				int expectedLen = readLen(buffer);
				if (expectedLen > buffer.remaining()) {
					// This Buffer doesn't contain a full frame of data
					// reset to the start and bail out
					buffer.rewind(lengthFieldLength);
					return null;
				}
				// We have at least a full frame of data

				// save the position and limit so we can reset it later
				int pos = buffer.position();
				int limit = buffer.limit();

				// create a view of the frame
				Buffer.View v = buffer.createView(pos, pos + expectedLen);
				// call the delegate decoder with the full frame
				IN in = decoder.apply(v.get());
				// reset the limit
				buffer.byteBuffer().limit(limit);
				if (buffer.position() == pos) {
					// the pointer hasn't advanced, advance it
					buffer.skip(expectedLen);
				}
				if (null != in) {
					// no Consumer was invoked, return this data
					return in;
				}
			}

			return null;
		}

		private int readLen(Buffer buffer) {
			if (lengthFieldLength == 4) {
				return buffer.readInt();
			} else {
				return (int) buffer.readLong();
			}
		}
	}

	private class LengthFieldEncoder implements Function<OUT, Buffer> {
		private final Function<OUT, Buffer> encoder = delegate.encoder();

		@Override
		public Buffer apply(OUT out) {
			if (null == out) {
				return null;
			}

			Buffer encoded = encoder.apply(out);
			if (null != encoded && encoded.remaining() > 0) {
				if (lengthFieldLength == 4) {
					encoded.prepend(encoded.remaining());
				} else if (lengthFieldLength == 8) {
					encoded.prepend((long) encoded.remaining());
				}
			}
			return encoded;
		}
	}

}
