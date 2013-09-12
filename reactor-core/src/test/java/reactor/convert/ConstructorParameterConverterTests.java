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

package reactor.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import reactor.convert.StandardConverters.ConstructorParameterConverter;

/**
 * @author Andy Wilkinson
 */
public class ConstructorParameterConverterTests {

	private final Converter converter = ConstructorParameterConverter.INSTANCE;

	@Test
	public void canConvertWhenTargetHasSingleArgConstructorThatTakesTheSourceType() {
		assertTrue(converter.canConvert(Integer.class, Target.class));
	}

	@Test
	public void canConvertWhenTargetHasASingleArgConstructorThatTakesATypeThatTheSourceCanBeConvertedTo() {
		assertTrue(converter.canConvert(String.class, Target.class));
	}

	@Test
	public void conversionCreatesNewInstanceOfTargetUsingSource() {
		Target target = converter.convert(Integer.valueOf(47), Target.class);
		assertEquals(Integer.valueOf(47), target.value);
	}

	@Test
	public void conversionCreatesNewInstanceOfTargetUsingConvertedSource() {
		Target target = converter.convert("47", Target.class);
		assertEquals(Integer.valueOf(47), target.value);
	}

	public static final class Target {

		final Integer value;

		public Target(Integer value) {
			this.value = value;
		}
	}

}
