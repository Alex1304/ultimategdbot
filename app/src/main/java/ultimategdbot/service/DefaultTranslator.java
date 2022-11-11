package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.api.i18n.Translator;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;

import java.util.Locale;

@RdiService
public final class DefaultTranslator implements Translator {

    private final Locale locale;

    @RdiFactory
    public DefaultTranslator(ConfigContainer configContainer) {
        this.locale = Locale.ENGLISH;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}
