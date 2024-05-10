package ultimategdbot;

import botrino.api.Botrino;
import reactor.core.publisher.Hooks;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Locale;

public final class Main {

    private static final Logger LOGGER = Loggers.getLogger(Main.class);

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        Hooks.onErrorDropped(t -> LOGGER.warn("Dropped error: {}", t.toString()));
        Botrino.run(args);
    }
}
