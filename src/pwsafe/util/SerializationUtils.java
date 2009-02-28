package pwsafe.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Serialization-related utility methods
 *
 * @author Nick Clarke
 */
public final class SerializationUtils {
    /**
     * Static utility methods only, no instances
     */
    private SerializationUtils() {}


    /**
     * Deserialize a single object from a byte array.
     *
     * @param data the serialized representation (from ObjectOutputStream), must not be null or empty
     * @param clazz the expected class of the deserialized object, must not be null
     * @return the deserialized object - a new copy for each call
     * @throws IllegalArgumentException if data is null or empty, or if clazz is null
     * @throws SerializationException if deserialization fails, or the deserialized object cannot be cast to clazz
     */
    public static <T extends Serializable> T deserialize(byte[] data, Class<T> clazz) throws SerializationException {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (data.length == 0) {
            throw new IllegalArgumentException("data must not be zero length");
        }
        ObjectInputStream stream = null;
        Object deserialized;
        try {
            stream = new ObjectInputStream(new ByteArrayInputStream(data));
            deserialized = stream.readObject();
        } catch (IOException e) {
            throw new SerializationException("Deserialization failed", e);
        } catch (ClassNotFoundException e) {
            throw new SerializationException("Deserialization failed", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
        try {
            return clazz.cast(deserialized);
        } catch (ClassCastException e) {
            throw new SerializationException(String.format(
                    "Deserialized data contained class '%s', expected '%s'",
                    deserialized.getClass().getName(), clazz.getName()), e);
        }
    }

    /**
     * Serialize a single object to a byte array.
     *
     * @param object the object to serialize, must not be null
     * @return the serialized representation (from ObjectOutputStream), never null or empty
     * @throws IllegalArgumentException if object is null
     * @throws SerializationException if serialization fails
     */
    public static byte[] serialize(Serializable object) throws SerializationException {
        if (object == null) {
            throw new IllegalArgumentException("object must not be null");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(output);
            stream.writeObject(object);
            stream.close();
            stream = null;
        } catch (IOException e) {
            throw new SerializationException("Serialization failed", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
        return output.toByteArray();
    }

    public static final class SerializationException extends Exception {
        /**
         * serialVersionUID for this class.
         */
        private static final long serialVersionUID = -2137627938454405879L;

        public SerializationException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
