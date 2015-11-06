package org.tsd.tsdbot.haloapi.model.metadata;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE }) @Retention(RUNTIME)
public @interface HaloMeta {
    String path();
    boolean list() default true;
}
