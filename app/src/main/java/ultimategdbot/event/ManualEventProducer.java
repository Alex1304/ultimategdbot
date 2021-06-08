package ultimategdbot.event;


import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import jdash.events.producer.GDEventProducer;
import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;

import java.util.Queue;

@RdiService
public final class ManualEventProducer implements GDEventProducer {

    private static final Logger LOGGER = Loggers.getLogger(ManualEventProducer.class);

    private final Queue<Object> queue = Queues.small().get();

    @Override
    public Flux<Object> produce(GDClient client) {
        return Flux.create(sink -> {
            while (!queue.isEmpty()) {
                sink.next(queue.poll());
            }
            sink.complete();
        });
    }

    public void submit(Object event) {
        if (!queue.offer(event)) {
            LOGGER.warn("Failed to submit event {}: manual event producer queue is full", event);
        }
    }
}
