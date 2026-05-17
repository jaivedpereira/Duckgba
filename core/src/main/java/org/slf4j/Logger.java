package org.slf4j;

/**
 * Minimal SLF4J-shaped logger so the embedded coffee-gb core can keep its
 * existing import statements. Forwards to {@code android.util.Log} when
 * available, and to {@code System.err} otherwise (e.g. plain JVM tests).
 *
 * Only the subset actually used by the core is implemented.
 */
public class Logger {

    /** Static log levels (higher == more important). */
    public static final int LEVEL_TRACE = 1;
    public static final int LEVEL_DEBUG = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_WARN = 4;
    public static final int LEVEL_ERROR = 5;

    /** Default threshold; bump down for very verbose runs. */
    public static volatile int minLevel = LEVEL_INFO;

    private final String tag;

    Logger(String name) {
        this.tag = abbreviate(name);
    }

    public boolean isDebugEnabled() { return LEVEL_DEBUG >= minLevel; }
    public boolean isTraceEnabled() { return LEVEL_TRACE >= minLevel; }

    // Plain messages
    public void trace(String msg) { write(LEVEL_TRACE, msg, null); }
    public void debug(String msg) { write(LEVEL_DEBUG, msg, null); }
    public void info(String msg)  { write(LEVEL_INFO,  msg, null); }
    public void warn(String msg)  { write(LEVEL_WARN,  msg, null); }
    public void error(String msg) { write(LEVEL_ERROR, msg, null); }

    public void warn(String msg, Throwable t) { write(LEVEL_WARN, msg, t); }
    public void error(String msg, Throwable t) { write(LEVEL_ERROR, msg, t); }

    // SLF4J-style {} placeholders
    public void debug(String fmt, Object arg) {
        if (isDebugEnabled()) write(LEVEL_DEBUG, format(fmt, new Object[]{arg}), null);
    }
    public void debug(String fmt, Object a, Object b) {
        if (isDebugEnabled()) write(LEVEL_DEBUG, format(fmt, new Object[]{a, b}), null);
    }
    public void debug(String fmt, Object... args) {
        if (isDebugEnabled()) write(LEVEL_DEBUG, format(fmt, args), null);
    }
    public void trace(String fmt, Object... args) {
        if (isTraceEnabled()) write(LEVEL_TRACE, format(fmt, args), null);
    }
    public void info(String fmt, Object... args)  { write(LEVEL_INFO, format(fmt, args), null); }
    public void warn(String fmt, Object... args)  { write(LEVEL_WARN, format(fmt, args), null); }
    public void error(String fmt, Object... args) { write(LEVEL_ERROR, format(fmt, args), null); }

    // Fluent API used by the core (logger.atDebug().log(...))
    public LogBuilder atDebug() { return new LogBuilder(this, LEVEL_DEBUG); }
    public LogBuilder atTrace() { return new LogBuilder(this, LEVEL_TRACE); }
    public LogBuilder atInfo()  { return new LogBuilder(this, LEVEL_INFO); }
    public LogBuilder atWarn()  { return new LogBuilder(this, LEVEL_WARN); }
    public LogBuilder atError() { return new LogBuilder(this, LEVEL_ERROR); }

    void write(int level, String msg, Throwable t) {
        if (level < minLevel) return;
        if (AndroidBridge.AVAILABLE) {
            AndroidBridge.write(level, tag, msg, t);
        } else {
            System.err.println("[" + levelTag(level) + "] " + tag + ": " + msg);
            if (t != null) t.printStackTrace(System.err);
        }
    }

    private static String levelTag(int level) {
        switch (level) {
            case LEVEL_TRACE: return "T";
            case LEVEL_DEBUG: return "D";
            case LEVEL_INFO:  return "I";
            case LEVEL_WARN:  return "W";
            case LEVEL_ERROR: return "E";
            default: return "?";
        }
    }

    static String format(String fmt, Object[] args) {
        if (fmt == null) return "null";
        if (args == null || args.length == 0) return fmt;
        StringBuilder sb = new StringBuilder(fmt.length() + 16 * args.length);
        int idx = 0;
        int argIdx = 0;
        while (idx < fmt.length()) {
            int next = fmt.indexOf("{}", idx);
            if (next < 0) {
                sb.append(fmt, idx, fmt.length());
                break;
            }
            sb.append(fmt, idx, next);
            if (argIdx < args.length) {
                sb.append(args[argIdx++]);
            } else {
                sb.append("{}");
            }
            idx = next + 2;
        }
        while (argIdx < args.length) {
            sb.append(' ').append(args[argIdx++]);
        }
        return sb.toString();
    }

    private static String abbreviate(String name) {
        return name.length() <= 23 ? name : name.substring(0, 23);
    }

    /**
     * Reflectively talks to {@code android.util.Log} so the core module can
     * also be unit-tested on a vanilla JVM (where android.util is missing).
     */
    private static final class AndroidBridge {
        static final boolean AVAILABLE;
        private static final java.lang.reflect.Method M_V;
        private static final java.lang.reflect.Method M_D;
        private static final java.lang.reflect.Method M_I;
        private static final java.lang.reflect.Method M_W;
        private static final java.lang.reflect.Method M_E;

        static {
            java.lang.reflect.Method v = null, d = null, i = null, w = null, e = null;
            boolean ok = false;
            try {
                Class<?> cls = Class.forName("android.util.Log");
                v = cls.getMethod("v", String.class, String.class);
                d = cls.getMethod("d", String.class, String.class);
                i = cls.getMethod("i", String.class, String.class);
                w = cls.getMethod("w", String.class, String.class);
                e = cls.getMethod("e", String.class, String.class, Throwable.class);
                ok = true;
            } catch (Throwable ignored) { /* plain JVM */ }
            M_V = v; M_D = d; M_I = i; M_W = w; M_E = e;
            AVAILABLE = ok;
        }

        static void write(int level, String tag, String msg, Throwable t) {
            try {
                switch (level) {
                    case LEVEL_TRACE: M_V.invoke(null, tag, msg); break;
                    case LEVEL_DEBUG: M_D.invoke(null, tag, msg); break;
                    case LEVEL_INFO:  M_I.invoke(null, tag, msg); break;
                    case LEVEL_WARN:  M_W.invoke(null, tag, msg); break;
                    case LEVEL_ERROR: M_E.invoke(null, tag, msg, t); break;
                    default: M_I.invoke(null, tag, msg); break;
                }
            } catch (Throwable ignored) { /* swallow logging failures */ }
        }
    }
}
