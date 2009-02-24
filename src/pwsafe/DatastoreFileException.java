package pwsafe;

/**
 * Thrown for problems reading or writing the datastore file
 *
 * @author Nick Clarke
 */
public class DatastoreFileException extends PWSafeException {
    /**
     * serialVersionUID for this class.
     */
    private static final long serialVersionUID = 6180465746682989622L;


    /**
     * Constructs a DatastoreFileException with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public DatastoreFileException() {
        super();
    }

    /**
     * Constructs a DatastoreFileException with the specified detail message. The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message, for later retrieval by the {@link #getMessage()} method.
     */
    public DatastoreFileException(final String message) {
        super(message);
    }

    /**
     * Constructs a DatastoreFileException with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is
     * <i>not</i> automatically incorporated in this exception's detail message.
     *
     * @param message the detail message, for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause, for later retrieval by the {@link #getCause()} method. May be null.
     */
    public DatastoreFileException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a DatastoreFileException with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the
     * class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param cause the cause, for later retrieval by the {@link #getCause()} method. May be null.
     */
    public DatastoreFileException(final Throwable cause) {
        super(cause);
    }
}
