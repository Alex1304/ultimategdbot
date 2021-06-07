package ultimategdbot.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandCategory {

    String GENERAL = "General commands";

    String GD = "Geometry Dash commands";

    String value();
}
