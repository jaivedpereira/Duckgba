package org.jetbrains.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Source-level marker mirroring JetBrains' {@code @NotNull}. The embedded
 * coffee-gb core uses it for documentation only, so a stub is enough.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD,
         ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface NotNull {
    String value() default "";
}
