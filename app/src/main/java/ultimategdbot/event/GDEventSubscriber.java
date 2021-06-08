package ultimategdbot.event;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

class GDEventSubscriber extends BaseSubscriber<Object> {

    private static final Logger LOGGER = Loggers.getLogger(GDEventSubscriber.class);
	
	private volatile Subscription subscription;
	private final GDEventService gdEventService;
	private final Scheduler scheduler = Schedulers.boundedElastic();
	
	GDEventSubscriber(GDEventService gdEventService) {
		this.gdEventService = gdEventService;
	}

	@Override
	public void hookOnSubscribe(Subscription s) {
		this.subscription = s;
		s.request(1);
	}

	@Override
	public void hookOnNext(Object t) {
        LOGGER.info("GD event fired: {}", t);
		gdEventService.process(t)
				.subscribeOn(scheduler)
				.doFinally(__ -> subscription.request(1))
				.subscribe(null,
                        e -> LOGGER.error("An error occurred while dispatching GD event", e),
                        () -> LOGGER.info("Successfully processed event {}", t));
	}
}
