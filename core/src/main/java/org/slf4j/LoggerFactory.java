package org.slf4j;

/**
 * Tiny factory mirroring {@code org.slf4j.LoggerFactory} so the embedded
 * coffee-gb core sources keep compiling unchanged.
 */
public final class LoggerFactory {

    private LoggerFactory() {}

    public static Logger getLogger(Class<?> cls) {
        return new Logger(cls.getSimpleName());
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }
}
