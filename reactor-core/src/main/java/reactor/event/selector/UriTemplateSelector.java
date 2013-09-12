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
 * A {@link Selector} implementation based on a {@link UriTemplate}.
 *
 * @author Jon Brisbin
 * @author Andy Wilkinson
 *
 * @see UriTemplate
 */
public class UriTemplateSelector extends ObjectSelector<UriTemplate> {

	private final HeaderResolver headerResolver = new HeaderResolver() {
		@Nullable
		@Override
		public Map<String, String> resolve(Object key) {
			Map<String, String> headers = getObject().match(key.toString());
			if (null != headers && !headers.isEmpty()) {
				return headers;
			}
			return null;
		}
	};

	/**
	 * Create a selector from the given uri template string.
	 *
	 * @param tmpl The string to compile into a {@link UriTemplate}.
	 */
	public UriTemplateSelector(String tmpl) {
		super(new UriTemplate(tmpl));
	}

	/**
	 * Creates a {@link Selector} based on a URI template.
	 *
	 * @param uriTemplate The URI template to compile.
	 *
	 * @return The new {@link Selector}.
	 *
	 * @see UriTemplate
	 */
	public static Selector uriTemplateSelector(String uriTemplate) {
		return new UriTemplateSelector(uriTemplate);
	}

	@Override
	public boolean matches(Object key) {
		if (!(key instanceof String)) {
			return false;
		}

		String path = (String) key;
		return getObject().matches(path);
	}

	@Override
	public HeaderResolver getHeaderResolver() {
		return headerResolver;
	}

}
