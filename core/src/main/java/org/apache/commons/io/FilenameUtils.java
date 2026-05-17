package org.apache.commons.io;

/**
 * Slim drop-in replacement for the subset of Apache Commons IO's
 * {@code FilenameUtils} that the embedded coffee-gb core uses.
 */
public final class FilenameUtils {

    private FilenameUtils() {}

    /** Returns the extension (without the dot) or empty string when there is none. */
    public static String getExtension(String fileName) {
        if (fileName == null) return "";
        int dot = indexOfExtensionStart(fileName);
        if (dot < 0) return "";
        return fileName.substring(dot + 1);
    }

    /** Returns {@code fileName} stripped of its extension. */
    public static String removeExtension(String fileName) {
        if (fileName == null) return null;
        int dot = indexOfExtensionStart(fileName);
        if (dot < 0) return fileName;
        return fileName.substring(0, dot);
    }

    private static int indexOfExtensionStart(String name) {
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        int dot = name.lastIndexOf('.');
        return (dot > slash) ? dot : -1;
    }
}
