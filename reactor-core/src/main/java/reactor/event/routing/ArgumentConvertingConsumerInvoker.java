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

package reactor.event.routing;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import reactor.convert.Converter;
import reactor.event.Event;
import reactor.function.Consumer;

/**
 * This implementation of a {@link reactor.event.routing.ConsumerInvoker} will attempt to invoke
 * a {@link Consumer} as-is and, if that fails with a {@link ClassCastException} because the
 * argument declared in the {@literal Consumer} isn't of the correct type, it tries to find an
 * object of that type in the array of possible arguments passed to the invoker. If that fails,
 * it will attempt to use a {@link Converter} to convert the argument into a form acceptable to
 * the {@literal Consumer}. If the argument is of type {@link Event} and the data inside that
 * event is of a compatible type with the argument to the consumer, this invoker will unwrap that
 * {@literal Event} and try to invoke the consumer using the data itself.
 * <p/>
 * Finally, if the {@literal Consumer} also implements {@link Callable}, then it will invoke the {@link
 * Callable#call()} method to obtain a return value and return that. Otherwise it will return
 * {@literal null} or throw any raised exceptions.
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public final class ArgumentConvertingConsumerInvoker implements ConsumerInvoker {

	private static final ReentrantReadWriteLock           CACHE_LOCK       = new ReentrantReadWriteLock();
	private static final ReentrantReadWriteLock.ReadLock  CACHE_READ_LOCK  = CACHE_LOCK.readLock();
	private static final ReentrantReadWriteLock.WriteLock CACHE_WRITE_LOCK = CACHE_LOCK.writeLock();
	private static final Map<String, Class<?>>            ARG_TYPE_CACHE   = new WeakHashMap<String, Class<?>>();

	private final Converter converter;

	/**
	 * Creates a new {@code ArgumentConvertingConsumerInvoker} that will use the given
	 * {@code converter} for any necessary argument conversion.
	 *
	 * @param converter
	 * 		The converter to be used
	 */
	public ArgumentConvertingConsumerInvoker(Converter converter) {
		this.converter = converter;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T> T invoke(Consumer<?> consumer,
	                    Class<? extends T> returnType,
	                    Object... possibleArgs) throws Exception {
		try {
			((Consumer)consumer).accept((possibleArgs.length > 0 ? possibleArgs[0] : null));
		} catch(ClassCastException e) {
			Class<?> argType = resolveArgType(consumer);
			if(argType == Object.class) {
				throw e;
			}

			// Try and find an argument when the list of possible arguments past the 1st
			for(int i = 1; i < possibleArgs.length; i++) {
				Object o = possibleArgs[i];
				if(null == o) {
					continue;
				}
				if(argType.isInstance(o)) {
					// arg type matches a possible arg
					return invoke(consumer, returnType, o);
				} else if(null != converter && converter.canConvert(o.getClass(), argType)) {
					// arg is convertible
					return invoke(consumer, returnType, converter.convert(o, argType));
				} else if(Event.class.isInstance(o)
						&& null != ((Event<?>)o).getData()
						&& argType.isInstance(((Event<?>)o).getData())) {
					// Try unwrapping the Event data
					return invoke(consumer, returnType, ((Event<?>)o).getData());
				}
			}

			// Try unwrapping the Event data
			if(possibleArgs.length == 1 && Event.class.isInstance(possibleArgs[0])) {
				return invoke(consumer, returnType, ((Event)possibleArgs[0]).getData());
			}

			throw e;
		}

		if(Void.TYPE == returnType) {
			return null;
		}

		if(consumer instanceof Callable) {
			Object o = ((Callable<Object>)consumer).call();

			if(null == o) {
				return null;
			}

			if(returnType.isAssignableFrom(o.getClass())) {
				return (T)o;
			} else if(null != converter && converter.canConvert(o.getClass(), returnType)) {
				return converter.convert(o, returnType);
			} else {
				throw new IllegalArgumentException("Cannot convert object of type " + o.getClass()
				                                                                       .getName() + " to " + returnType.getName());
			}
		}
		return null;
	}

	@Override
	public boolean supports(Consumer<?> consumer) {
		return true;
	}


	/**
	 * Resolves the type of argument that can be {@link Consumer#accept accepted} by the
	 * given {@code consumer}.
	 *
	 * @param consumer
	 * 		The consumer to examine
	 *
	 * @return The type that can be accepted by the consumer
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> Class<? extends T> resolveArgType(Consumer<?> consumer) {
		Class<? extends T> clazz;
		CACHE_READ_LOCK.lock();
		try {
			clazz = (Class<? extends T>)ARG_TYPE_CACHE.get(consumer.getClass().getName());
			if(null != clazz) {
				return clazz;
			}
		} finally {
			CACHE_READ_LOCK.unlock();
		}

		if(Event.class.isInstance(consumer) && null != ((Event<?>)consumer).getData()) {
			return (Class<? extends T>)((Event<?>)consumer).getData().getClass();
		}

		for(Type t : consumer.getClass().getGenericInterfaces()) {
			if(t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType)t;
				Type t1 = pt.getActualTypeArguments()[0];
				if(t1 instanceof ParameterizedType) {
					clazz = (Class<? extends T>)((ParameterizedType)t1).getRawType();
				} else if(t1 instanceof Class) {
					clazz = (Class<? extends T>)t1;
				}
			}
			if(null != clazz) {
				CACHE_WRITE_LOCK.lock();
				try {
					ARG_TYPE_CACHE.put(consumer.getClass().getName(), clazz);
				} finally {
					CACHE_WRITE_LOCK.unlock();
				}
				break;
			}
		}

		if(null == clazz) {
			for(Method m : consumer.getClass().getDeclaredMethods()) {
				if("accept".equals(m.getName()) && m.getParameterTypes().length == 1) {
					clazz = (Class<? extends T>)m.getParameterTypes()[0];
					CACHE_WRITE_LOCK.lock();
					try {
						ARG_TYPE_CACHE.put(consumer.getClass().getName(), clazz);
					} finally {
						CACHE_WRITE_LOCK.unlock();
					}
					return clazz;
				}
			}
		}

		return clazz;
	}

}
