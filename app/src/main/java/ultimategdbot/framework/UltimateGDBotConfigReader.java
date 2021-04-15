package ultimategdbot.framework;

import botrino.api.config.ConfigReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

public class UltimateGDBotConfigReader implements ConfigReader {

    @Override
    public ObjectMapper createConfigObjectMapper() {
        return ConfigReader.super.createConfigObjectMapper().registerModule(new GuavaModule());
    }
}
