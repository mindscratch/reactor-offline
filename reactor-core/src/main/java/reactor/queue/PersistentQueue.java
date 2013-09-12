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

package reactor.queue;

import java.util.AbstractQueue;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@literal PersistentQueue} is a {@link java.util.Queue} implementation that delegates the actual storage of the
 * elements in the queue to a {@link QueuePersistor}.
 *
 * @author Jon Brisbin
 */
public class PersistentQueue<T> extends AbstractQueue<T> {

	private final QueuePersistor<T> persistor;

	/**
	 * Create a {@literal PersistentQueue} using the given {@link QueuePersistor}.
	 *
	 * @param persistor
	 */
	public PersistentQueue(@Nullable QueuePersistor<T> persistor) {
		this.persistor = (null == persistor ? new InMemoryQueuePersistor<T>() : persistor);
	}

	@Nonnull
	public Iterator<T> iterator() {
		return persistor.iterator();
	}

	@Override
	public int size() {
		return (int)persistor.size();
	}

	@Override
	public boolean offer(T obj) {
		return (null != persistor.offer().apply(obj));
	}

	@Override
	public T poll() {
		return persistor.remove().get();
	}

	@Override
	public T peek() {
		Long lastId = persistor.lastId();
		return persistor.get().apply(lastId);
	}

}
