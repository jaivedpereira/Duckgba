package org.slf4j;

/**
 * Subset of the SLF4J 2.x fluent log builder. Only {@code log(format, args)}
 * is needed by the embedded core.
 */
public class LogBuilder {

    private final Logger logger;
    private final int level;
    private Throwable cause;

    LogBuilder(Logger logger, int level) {
        this.logger = logger;
        this.level = level;
    }

    public LogBuilder setMessage(String message) {
        logger.write(level, message, null);
        return this;
    }

    public LogBuilder addArgument(Object value) {
        // No-op for the way coffee-gb uses the API (it always calls log() at the end).
        return this;
    }

    public LogBuilder setCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public void log() { /* no-op */ }

    public void log(String msg) {
        logger.write(level, msg, cause);
    }

    public void log(String fmt, Object arg) {
        logger.write(level, Logger.format(fmt, new Object[]{arg}), cause);
    }

    public void log(String fmt, Object a, Object b) {
        logger.write(level, Logger.format(fmt, new Object[]{a, b}), cause);
    }

    public void log(String fmt, Object... args) {
        logger.write(level, Logger.format(fmt, args), cause);
    }
}
