package ultimategdbot.util;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class Misc {
    public static Mono<ByteArrayInputStream> imageStream(BufferedImage img) {
        return Mono.fromCallable(() -> {
            final var os = new ByteArrayOutputStream(100_000);
            ImageIO.write(img, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
