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

package reactor.event.selector;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Responsible for extracting any applicable headers from a key.
 *
 * @author Jon Brisbin
 */
public interface HeaderResolver {

	/**
	 * Resolve the headers that might be encoded in a key.
	 *
	 * @param key The key to match.
	 *
	 * @return Any applicable headers. Might be {@literal null}.
	 */
	@Nullable
	Map<String, String> resolve(Object key);

}
