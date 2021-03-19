package ultimategdbot.util;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Properties;

public final class VersionUtils {
	
	public static final String API_GIT_RESOURCE = "META-INF/git/ultimategdbot.git.properties";

	/**
	 * Extracts the git properties from a properties file which location is given.
	 * 
	 * @param resourceLocation the location of the properties resource containing
	 *                         git information
	 * @return the git properties
	 */
	public static Mono<Properties> getGitProperties(String resourceLocation) {
		return Mono.fromCallable(() -> {
			var props = new Properties();
			try (var stream = ClassLoader.getSystemResourceAsStream(resourceLocation)) {
				if (stream != null) {
					props.load(stream);
				}
			}
			return props;
		}).subscribeOn(Schedulers.boundedElastic());
	}
	
	private VersionUtils() {
		throw new AssertionError();
	}
}
