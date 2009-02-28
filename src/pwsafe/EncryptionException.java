package pwsafe;

/**
 * Thrown for errors encrypting data store
 *
 * @author Nick Clarke
 */
public class EncryptionException extends PWSafeException {
    /**
     * serialVersionUID for this class.
     */
    private static final long serialVersionUID = -1153568529767974697L;


    /**
     * Constructs a EncryptionException with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public EncryptionException() {
        super();
    }

    /**
     * Constructs a EncryptionException with the specified detail message. The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message, for later retrieval by the {@link #getMessage()} method.
     */
    public EncryptionException(final String message) {
        super(message);
    }

    /**
     * Constructs a EncryptionException with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is
     * <i>not</i> automatically incorporated in this exception's detail message.
     *
     * @param message the detail message, for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause, for later retrieval by the {@link #getCause()} method. May be null.
     */
    public EncryptionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a EncryptionException with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the
     * class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param cause the cause, for later retrieval by the {@link #getCause()} method. May be null.
     */
    public EncryptionException(final Throwable cause) {
        super(cause);
    }
}
