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

package reactor.spring.core.convert;

import org.springframework.core.convert.ConversionService;
import reactor.convert.Converter;

/**
 * An adapter that allows a Spring {@link ConversionService} to be used as a {@link Converter}.
 *
 * @author Stephane Maldini
 */
public class ConversionServiceConverter implements Converter {

	private ConversionService conversionService;

	/**
	 * Creates a new ConversionServiceConverter that will delegate conversion to the given
	 * {@code conversionService}
	 *
	 * @param conversionService
	 * 		The conversion service to delegate to
	 */
	public ConversionServiceConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return conversionService.canConvert(sourceType, targetType);
	}

	@Override
	public <T> T convert(Object source, Class<T> targetType) {
		return conversionService.convert(source, targetType);
	}

}
