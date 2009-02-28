package pwsafe.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * IO-related utility methods
 *
 * @author Nick Clarke
 */
public final class IOUtils {
    /**
     * Static utility methods only, no instances
     */
    private IOUtils() {}


    /**
     * Read entire contents of a file and return as bytes
     *
     * @param file the file to read, must not be null
     * @return the bytes read from the file - a new copy for each call
     * @throws IllegalArgumentException if file is null
     * @throws IOException if reading fails or file is a directory, doesn't exist or is not readable
     */
    public static byte[] readFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            int pos = 0;
            int read;
            byte[] buf = new byte[4096];
            while ((read = stream.read(buf)) != -1) {
                if (read > 0) {
                    output.write(buf, 0, read);
                }
            }
            output.flush();
            stream.close();
            stream = null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
        return output.toByteArray();
    }

    /**
     * Write data to a file, replacing any existing contents.
     *
     * @param file the file to write to, must not be null
     * @param data the data to write to the file, must not be null
     * @throws IllegalArgumentException if file or data is null
     * @throws IOException if writing fails or file is a directory or is not writable
     */
    public static void writeFile(File file, byte[] data) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("fle must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            stream.write(data);
            stream.close();
            stream = null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
