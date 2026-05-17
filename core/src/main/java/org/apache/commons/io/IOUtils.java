package org.apache.commons.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Thin replacement covering only the {@code IOUtils} entry points referenced by
 * the embedded coffee-gb core.
 */
public final class IOUtils {

    private static final int BUFFER_SIZE = 8 * 1024;

    private IOUtils() {}

    /** Reads the entire stream into a fresh {@code byte[]}. The hint is ignored. */
    public static byte[] toByteArray(InputStream input, int sizeHint) throws IOException {
        return toByteArray(input);
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(BUFFER_SIZE, 32));
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = input.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Reads as many bytes as possible into {@code buffer}, returning the
     * number of bytes effectively read (matches Commons IO behaviour).
     */
    public static int read(InputStream input, byte[] buffer) throws IOException {
        return read(input, buffer, 0, buffer.length);
    }

    public static int read(InputStream input, byte[] buffer, int offset, int length) throws IOException {
        if (length < 0) throw new IllegalArgumentException("length < 0");
        int remaining = length;
        while (remaining > 0) {
            int read = input.read(buffer, offset + (length - remaining), remaining);
            if (read == -1) break;
            remaining -= read;
        }
        return length - remaining;
    }
}
