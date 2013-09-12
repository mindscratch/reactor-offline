package reactor.groovy.config;

import reactor.event.support.CallbackEvent;
import reactor.core.composable.Deferred;
import reactor.core.composable.Stream;
import reactor.event.Event;
import reactor.event.registry.Registration;
import reactor.event.routing.ConsumerFilteringEventRouter;
import reactor.event.routing.ConsumerInvoker;
import reactor.filter.Filter;
import reactor.function.Consumer;

import java.util.List;

/**
 * @author Stephane Maldini
 */
public class StreamEventRouter extends ConsumerFilteringEventRouter {

	public static final String KEY_HEADER = "___key";

	private final Deferred<Event<?>, Stream<Event<?>>> stream;

	public StreamEventRouter(Filter filter, ConsumerInvoker consumerInvoker, Deferred<Event<?>,
			Stream<Event<?>>> stream) {
		super(filter, consumerInvoker);
		this.stream = stream;
	}

	@Override
	public void route(final Object key, final Event<?> event,
	                  final List<Registration<? extends Consumer<? extends Event<?>>>> consumers,
	                  final Consumer<?> completionConsumer, final Consumer<Throwable> errorConsumer) {

		try {
			event.getHeaders().set(KEY_HEADER, key.toString());
		} catch (Exception e) {
			//ignore
		}

		stream.acceptEvent(new CallbackEvent<Event<?>>(event.getHeaders(), event,
				new Consumer<Event<?>>() {
					@Override
					public void accept(Event<?> _event) {
						Event<?> hydratedEvent = event.copy(_event != null ? _event.getData() : null);
						if (null != consumers) {
							for (Registration<? extends Consumer<? extends Event<?>>> consumer : getFilter().filter(consumers, key)) {
								try {
									invokeConsumer(key, hydratedEvent, consumer);
								} catch (Throwable t) {
									if(null != hydratedEvent.getErrorConsumer()){
										hydratedEvent.consumeError(t);
									} else if (null != errorConsumer) {
										errorConsumer.accept(t);
									}
									stream.accept(t);
								}
							}
						}
						if (null != completionConsumer) {
							try {
								getConsumerInvoker().invoke(completionConsumer, Void.TYPE, hydratedEvent);
							} catch (Exception e) {
								if (null != errorConsumer) {
									errorConsumer.accept(e);
								}
								stream.accept(e);
							}
						}
					}
				}));
	}
}
