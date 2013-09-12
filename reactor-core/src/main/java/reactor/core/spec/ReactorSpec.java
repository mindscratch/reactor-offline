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
package reactor.core.spec;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.support.EventRoutingComponentSpec;

/**
 * A helper class for configuring a new {@link Reactor}.
 *
 * @author Jon Brisbin
 */
public class ReactorSpec extends EventRoutingComponentSpec<ReactorSpec, Reactor> {

	private Reactor linked;

	/**
	 * Configured the Reactor to be linked to the given reactor such that events that the
	 * reactor being configured is notified of events that {@code linked} is notified of.
	 *
	 * @param linked The reactor to link this reactor to
	 *
	 * @return {@code this}
	 */
	public ReactorSpec link(Reactor linked) {
		this.linked = linked;
		return this;
	}

	@Override
	protected final Reactor configure(Reactor reactor, Environment environment) {
		if (null != linked) {
			linked.link(reactor);
		}
		return reactor;
	}

}
