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

/**
 * A simple {@link Codec} implementation that turns a {@link Buffer} into a {@code byte[]} and visa-versa.
 *
 * @author Jon Brisbin
 */
public class ByteArrayCodec implements Codec<Buffer, byte[], byte[]> {

	@Override
	public Function<Buffer, byte[]> decoder(final Consumer<byte[]> next) {
		return new Function<Buffer, byte[]>() {
			@Override
			public byte[] apply(Buffer buffer) {
				if (null != next) {
					next.accept(buffer.asBytes());
					return null;
				} else {
					return buffer.asBytes();
				}
			}
		};
	}

	@Override
	public Function<byte[], Buffer> encoder() {
		return new Function<byte[], Buffer>() {
			@Override
			public Buffer apply(byte[] bytes) {
				return Buffer.wrap(bytes);
			}
		};
	}

}
