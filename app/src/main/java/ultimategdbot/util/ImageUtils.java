package ultimategdbot.util;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ImageUtils {

    public static Mono<ByteArrayInputStream> imageStream(BufferedImage img) {
        return Mono.fromCallable(() -> {
            final var os = new ByteArrayOutputStream(100_000);
            ImageIO.write(img, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public static BufferedImage makeSquare(BufferedImage img) {
        final var width = img.getWidth();
        final var height = img.getHeight();
        final var size = Math.max(width, height);

        final var square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        final var x = (size - width) / 2;
        final var y = (size - height) / 2;

        final var g2d = square.createGraphics();
        g2d.drawImage(img, x, y, null);
        g2d.dispose();

        return square;
    }
}
