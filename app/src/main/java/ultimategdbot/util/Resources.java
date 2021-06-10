package ultimategdbot.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Resources {
	
	private static final String UGDB_GIT_RESOURCE = "META-INF/git/ultimategdbot.git.properties";

	public static Properties ugdbGitProperties() {
	    try {
            var props = new Properties();
            try (var stream = ClassLoader.getSystemResourceAsStream(UGDB_GIT_RESOURCE)) {
                if (stream != null) {
                    props.load(stream);
                }
            }
            return props;
        } catch (IOException e) {
	        throw new UncheckedIOException(e);
        }
	}

	public static String about() {
	    try {
	        return Files.readString(Path.of(".", "about.txt"));
        } catch (IOException e) {
	        throw new UncheckedIOException(e);
        }
    }
	
	private Resources() {
		throw new AssertionError();
	}
}
