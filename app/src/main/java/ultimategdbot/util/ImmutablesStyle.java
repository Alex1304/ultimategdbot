package ultimategdbot.util;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
@Value.Style(jdkOnly = true, allowedClasspathAnnotations = Override.class)
public @interface ImmutablesStyle {}
