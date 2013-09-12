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

package reactor.tcp.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.Promises;
import reactor.core.spec.Reactors;
import reactor.function.Consumer;
import reactor.io.Buffer;
import reactor.support.NamedDaemonThreadFactory;
import reactor.tcp.TcpConnection;
import reactor.tcp.TcpServer;
import reactor.tcp.config.ServerSocketOptions;
import reactor.tcp.config.SslOptions;
import reactor.tcp.encoding.Codec;
import reactor.tcp.ssl.SSLEngineSupplier;
import reactor.util.Assert;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Netty-based {@code TcpServer} implementation
 *
 * @param <IN>  The type that will be received by this server
 * @param <OUT> The type that will be sent by this server
 * @author Jon Brisbin
 */
public class NettyTcpServer<IN, OUT> extends TcpServer<IN, OUT> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ServerBootstrap     bootstrap;
	private final Reactor             eventsReactor;
	private final ServerSocketOptions options;
	private final EventLoopGroup      selectorGroup;
	private final EventLoopGroup      ioGroup;

	protected NettyTcpServer(Environment env,
													 Reactor reactor,
													 InetSocketAddress listenAddress,
													 final ServerSocketOptions opts,
													 final SslOptions sslOpts,
													 Codec<Buffer, IN, OUT> codec,
													 Collection<Consumer<TcpConnection<IN, OUT>>> connectionConsumers) {
		super(env, reactor, listenAddress, opts, sslOpts, codec, connectionConsumers);
		this.eventsReactor = reactor;
		Assert.notNull(opts, "ServerSocketOptions cannot be null");
		this.options = opts;

		int selectThreadCount = env.getProperty("reactor.tcp.selectThreadCount", Integer.class, Environment.PROCESSORS / 2);
		int ioThreadCount = env.getProperty("reactor.tcp.ioThreadCount", Integer.class, Environment.PROCESSORS);
		selectorGroup = new NioEventLoopGroup(selectThreadCount, new NamedDaemonThreadFactory("reactor-tcp-select"));
		ioGroup = new NioEventLoopGroup(ioThreadCount, new NamedDaemonThreadFactory("reactor-tcp-io"));

		this.bootstrap = new ServerBootstrap()
				.group(selectorGroup, ioGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, options.backlog())
				.option(ChannelOption.SO_RCVBUF, options.rcvbuf())
				.option(ChannelOption.SO_SNDBUF, options.sndbuf())
				.option(ChannelOption.SO_REUSEADDR, options.reuseAddr())
				.localAddress((null == listenAddress ? new InetSocketAddress(3000) : listenAddress))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(final SocketChannel ch) throws Exception {
						SocketChannelConfig config = ch.config();
						config.setReceiveBufferSize(options.rcvbuf());
						config.setSendBufferSize(options.sndbuf());
						config.setKeepAlive(options.keepAlive());
						config.setReuseAddress(options.reuseAddr());
						config.setSoLinger(options.linger());
						config.setTcpNoDelay(options.tcpNoDelay());

						log.debug("CONNECT {}", ch.remoteAddress());

						if (null != sslOpts) {
							SSLEngine ssl = new SSLEngineSupplier(sslOpts, false).get();
							log.debug("SSL enabled using keystore {}", (null != sslOpts.keystoreFile() ? sslOpts.keystoreFile() : "<DEFAULT>"));
							ch.pipeline().addLast(new SslHandler(ssl));
						}
						if (options instanceof NettyServerSocketOptions && null != ((NettyServerSocketOptions) options).pipelineConfigurer()) {
							((NettyServerSocketOptions) options).pipelineConfigurer().accept(ch.pipeline());
						}
						ch.pipeline().addLast(createChannelHandlers(ch));
						ch.closeFuture().addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								close(ch);
							}
						});
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						NettyTcpServer.this.notifyError(cause);
					}
				});
	}

	@Override
	public NettyTcpServer<IN, OUT> start(final Consumer<Void> started) {
		ChannelFuture bindFuture = bootstrap.bind();
		if (null != started) {
			bindFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					log.info("BIND {}", future.channel().localAddress());
					notifyStart(started);
				}
			});
		}

		return this;
	}

	@Override
	public Promise<Void> shutdown() {
		final Deferred<Void, Promise<Void>> d = Promises.<Void>defer().env(env).dispatcher(getReactor().getDispatcher()).get();
		Reactors.schedule(
				new Consumer<Void>() {
					@SuppressWarnings({"rawtypes", "unchecked"})
					@Override
					public void accept(Void v) {
						final AtomicInteger groupsToShutdown = new AtomicInteger(2);
						GenericFutureListener listener = new GenericFutureListener() {

							@Override
							public void operationComplete(Future future) throws Exception {
								if (groupsToShutdown.decrementAndGet() == 0) {
									notifyShutdown();
									d.accept((Void) null);
								}
							}
						};
						selectorGroup.shutdownGracefully().addListener(listener);
						ioGroup.shutdownGracefully().addListener(listener);
					}
				},
				null,
				getReactor()
		);

		return d.compose();
	}

	@Override
	protected <C> NettyTcpConnection<IN, OUT> createConnection(C channel) {
		SocketChannel ch = (SocketChannel) channel;
		int backlog = env.getProperty("reactor.tcp.connectionReactorBacklog", Integer.class, 128);

		return new NettyTcpConnection<IN, OUT>(
				env,
				getCodec(),
				new NettyEventLoopDispatcher(ch.eventLoop(), backlog),
				eventsReactor,
				ch
		);
	}

	protected ChannelHandler[] createChannelHandlers(SocketChannel ch) {
		NettyTcpConnection<IN, OUT> conn = (NettyTcpConnection<IN, OUT>) select(ch);
		return new ChannelHandler[]{new NettyTcpConnectionChannelInboundHandler(conn)};
	}

}
