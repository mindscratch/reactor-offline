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

package reactor.core.dynamic.reflect;

import reactor.function.Function;
import reactor.event.selector.Selector;
import reactor.support.Supports;

import java.lang.reflect.Method;

/**
 * When given a {@link Method}, a {@code MethodSelectorResolver} will attempt to return a
 * {@link Selector} for the method.
 *
 * @author Jon Brisbin
 */
public interface MethodSelectorResolver extends Supports<Method>, Function<Method, Selector> {

}
