package org.tsd.tsdbot;

import com.google.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Joe on 3/28/2015.
 */
@Target({ElementType.TYPE})
@Retention(RUNTIME)
public @interface Function {
    public String initialRegex();
}
