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

package reactor.tcp.netty

import reactor.core.Environment
import reactor.function.Consumer
import reactor.function.Function
import reactor.io.Buffer
import reactor.tcp.TcpConnection
import reactor.tcp.TcpServer
import reactor.tcp.encoding.json.JsonCodec
import reactor.tcp.spec.TcpServerSpec
import spock.lang.Specification

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Jon Brisbin
 */
class NettyTcpServerSpec extends Specification {

	static final int port = 26874
	Environment env

	def setup() {
		env = new Environment()
	}

	def "NettyTcpServer responds to requests from clients"() {
		given: "a simple TcpServer"
		def startLatch = new CountDownLatch(1)
		def stopLatch = new CountDownLatch(1)
		def dataLatch = new CountDownLatch(1)
		def server = new TcpServerSpec<Buffer, Buffer>(NettyTcpServer).
				env(env).
				listen(port).
				consume({ TcpConnection<Buffer, Buffer> conn ->
					conn.receive({ Buffer data ->
						Buffer.wrap("Hello World!")
					} as Function<Buffer, Buffer>)
				} as Consumer<TcpConnection<Buffer, Buffer>>).
				get()

		when: "the server is started"
		server.start({
			startLatch.countDown()
		} as Consumer<Void>)
		startLatch.await(5, TimeUnit.SECONDS)

		then: "the server was started"
		startLatch.count == 0

		when: "data is sent"
		def client = new SimpleClient(port, dataLatch, Buffer.wrap("Hello World!"))
		client.start()
		dataLatch.await(5, TimeUnit.SECONDS)

		then: "data was recieved"
		client.data?.remaining() == 12
		new Buffer(client.data).asString() == "Hello World!"
		dataLatch.count == 0

		when: "the server is stopped"
		server.shutdown().onSuccess({
			stopLatch.countDown()
		} as Consumer<Void>)
		stopLatch.await(5, TimeUnit.SECONDS)

		then: "the server was stopped"
		stopLatch.count == 0
	}

	def "NettyTcpServer can encode and decode JSON"() {
		given: "a TcpServer with JSON codec"
		def startLatch = new CountDownLatch(1)
		def stopLatch = new CountDownLatch(1)
		def dataLatch = new CountDownLatch(1)
		def server = new TcpServerSpec<Pojo, Pojo>(NettyTcpServer).
				env(env).
				listen(port).
				codec(new JsonCodec<Pojo, Pojo>(Pojo)).
				consume({ conn ->
					conn.receive({ pojo ->
						assert pojo.name == "John Doe"
						new Pojo(name: "Jane Doe")
					} as Function<Pojo, Pojo>)
				} as Consumer<TcpConnection<Pojo, Pojo>>).
				get()

		when: "the server is started"
		server.start({
			startLatch.countDown()
		} as Consumer<Void>)
		startLatch.await(5, TimeUnit.SECONDS)

		then: "the server was started"
		startLatch.count == 0

		when: "a pojo is written"
		def client = new SimpleClient(port, dataLatch, Buffer.wrap("{\"name\":\"John Doe\"}"))
		client.start()
		dataLatch.await(5, TimeUnit.SECONDS)

		then: "data was recieved"
		client.data?.remaining() == 19
		new Buffer(client.data).asString() == "{\"name\":\"Jane Doe\"}"
		dataLatch.count == 0

		when: "the server is stopped"
		server.shutdown().onSuccess({
			stopLatch.countDown()
		} as Consumer<Void>)
		stopLatch.await(5, TimeUnit.SECONDS)

		then: "the server was stopped"
		stopLatch.count == 0
	}

	static class SimpleClient extends Thread {
		final int port
		final CountDownLatch latch
		final Buffer output
		ByteBuffer data

		SimpleClient(int port, CountDownLatch latch, Buffer output) {
			this.port = port
			this.latch = latch
			this.output = output
		}

		@Override
		void run() {
			def ch = SocketChannel.open(new InetSocketAddress(port))
			def len = ch.write(output.byteBuffer())
			assert ch.connected
			data = ByteBuffer.allocate(len)
			int read = ch.read(data)
			assert read > 0
			data.flip()
			latch.countDown()
		}
	}

	static class Pojo {
		String name
	}

}
