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

package reactor.tcp.config;

/**
 * Encapsulates configuration options for server sockets.
 *
 * @author Jon Brisbin
 */
public class ServerSocketOptions extends CommonSocketOptions<ServerSocketOptions> {

	private int     backlog   = 1000;
	private boolean reuseAddr = true;

	/**
	 * Returns the configured pending connection backlog for the socket.
	 *
	 * @return The configured connection backlog size
	 */
	public int backlog() {
		return backlog;
	}

	/**
	 * Configures the size of the pending connection backlog for the socket.
	 *
	 * @param backlog The size of the backlog
	 *
	 * @return {@code this}
	 */
	public ServerSocketOptions backlog(int backlog) {
		this.backlog = backlog;
		return this;
	}

	/**
	 * Returns a boolean indicating whether or not {@code SO_REUSEADDR} is enabled
	 *
	 * @return {@code true} if {@code SO_REUSEADDR} is enabled, {@code false} if it is not
	 */
	public boolean reuseAddr() {
		return reuseAddr;
	}

	/**
	 * Enables or disables {@code SO_REUSEADDR}.
	 *
	 * @param reuseAddr {@code true} to enable {@code SO_REUSEADDR}, {@code false} to disable it
	 *
	 * @return {@code this}
	 */
	public ServerSocketOptions reuseAddr(boolean reuseAddr) {
		this.reuseAddr = reuseAddr;
		return this;
	}
}
