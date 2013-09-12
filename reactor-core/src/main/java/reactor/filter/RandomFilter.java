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

package reactor.filter;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A {@link Filter} implementation that returns a single, randomly selected item.
 *
 * @author Andy Wilkinson
 *
 */
public final class RandomFilter extends AbstractFilter {

	private final Random random = new Random();

	@Override
	public <T> List<T> doFilter(List<T> items, Object key) {
		if (items.isEmpty()) {
			return items;
		} else {
			return Collections.singletonList(items.get(random.nextInt(items.size())));
		}
	}
}
