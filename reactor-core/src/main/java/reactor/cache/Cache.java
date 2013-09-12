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

package reactor.cache;

/**
 * A cache provides access to a cache of objects.
 *
 * @param <T> The type of object in the cache
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public interface Cache<T> {

	/**
	 * Allocates a new object from the cache
	 *
	 * @return the allocated object
	 */
	T allocate();

	/**
	 * Returns an object to the cache
	 *
	 * @param obj The object to deallocate
	 */
	void deallocate(T obj);

}
