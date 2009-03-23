package pwsafe.store;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * File binary data attached to store entries
 *
 * @author Nick Clarke
 */
public class Attachment implements Serializable, Comparable<Attachment> {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeObject(ObjectOutputStream)
     */
    private static final long serialVersionUID = -6141760753724906434L;

    /**
     * Allows backward-compatible deserialization for this class.
     * <p>
     * For backward-compatibility, increment this when changing serialization implementation,
     * and update readObject to handle the new and old versions.
     *
     * @see #writeObject(ObjectOutputStream)
     * @see #readObject(ObjectInputStream)
     */
    private static final byte VERSION = 0x1;


    private transient boolean _destroyed = false;

    private String _filename;
    private Date _attachmentCreated;
    private byte[] _fileContent;
    private Date _fileContentLastChanged;
    private String _description;

    /**
     * Construct a Attachment
     * <p>
     * <b>IMPORTANT:</b> this stores references to the supplied parameters, it does not make copies.
     * The caller should discard their references to these parameters but leave their contents intact;
     * this Attachment object assumes responsibility for clearing and discarding the secret data.
     *
     * @param filename must not be null or empty
     * @param fileContent the binary content to store, must not be null, can be empty
     * @param description the description of this item, must not be null, can be empty
     * @throws IllegalArgumentException if any argument is null, or if filename is empty
     */
    public Attachment(final String filename) {
        final Date now = new Date();
        checkFilename(filename);
        _filename = filename;
        _fileContent = new byte[0];
        _description = "";
        _attachmentCreated = now;
        _fileContentLastChanged = now;
    }

    /**
     * Construct an Attachment by deep-copying another Attachment
     */
    private Attachment(final Attachment other) {
        _filename               =          other._filename;
        _attachmentCreated      = (Date)   other._attachmentCreated.clone();
        _fileContent            = (byte[]) other._fileContent.clone();
        _fileContentLastChanged = (Date)   other._fileContentLastChanged.clone();
        _description            =          other._description;
    }

    /**
     * Deep-copy this Attachment
     */
    @Override
    public Attachment clone() {
        return new Attachment(this);
    }

    /**
     * Save all fields at once, with the same timestamp for any modified fields.
     * <p>
     * <b>IMPORTANT:</b> this stores references to the supplied parameters, it does not make copies.
     * The caller should discard their references to these parameters but leave their contents intact;
     * this Attachment object assumes responsibility for clearing and discarding the secret data.
     *
     * @param filename must not be null or empty
     * @param fileContent the binary content to store, must not be null, can be empty
     * @param description the description of this item, must not be null, can be empty
     * @throws IllegalArgumentException if any argument is null, or if filename is empty
     */
    public void setAllFields(final String filename,
                             final byte[] fileContent,
                             final String description) {
        final Date now = new Date();
        if (fileContent == null) {
            throw new IllegalArgumentException("fileContent must not be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("description must not be null");
        }
        checkNotDestroyed();
        setFilename(filename);
        setFileContent(fileContent, now);
        setDescription(description);
    }

    /**
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    private void checkNotDestroyed() {
        if (_destroyed) {
            throw new IllegalStateException("destroySecrets() has already been called");
        }
    }

    /**
     * @throws IllegalArgumentException if filename is null or empty
     */
    private void checkFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }
        if ("".equals(filename)) {
            throw new IllegalArgumentException("filename cannot be an empty string");
        }
    }

    /**
     * Get the date and time which this record was first created.
     *
     * @return date and time, never null.
     */
    public Date getAttachmentCreated() {
        return (Date) _attachmentCreated.clone();
    }

    /**
     * Get the filename for this record
     *
     * @return the filename, never null or empty
     */
    public String getFilename() {
        return _filename;
    }

    /**
     * Set the filename for this record
     *
     * @param filename the new filename, must not be null or empty
     * @throws IllegalArgumentException if filename is null or empty
     */
    private void setFilename(String filename) {
        checkFilename(filename);
        _filename = filename;
    }

    /**
     * Get the description for this record.
     *
     * @return the description, can be empty
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Set the description for this record.
     *
     * @param description the new description
     */
    private void setDescription(String description) {
        _description = description;
    }

    /**
     * Get the file content (secret) for this record, if any.
     * <p>
     * <b>IMPORTANT:</b> this returns the internal array itself, not a copy,
     * so any modifications to the array will be written back to the store with this record.
     * <p>
     * For security, caller should avoid storing references longer than necessary,
     * and if the caller copies the data, it MUST ensure it is zero-overwritten and discarded when finished.
     * <p>
     * Assuming the caller does not make a copy, this Attachment retains responsibility for clearing
     * and discarding the secret data, and caller should not zero it.
     *
     * @return non-null content (plaintext), can be empty
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    public byte[] getFileContent() {
        checkNotDestroyed();
        return _fileContent;
    }

    /**
     * Set the file content (secret) for this record, if any.
     * The existing file content (if any) will be zeroed and discarded.
     * <p>
     * <b>IMPORTANT:</b> this stores a reference to the supplied array, it does not make a copy.
     * The caller should discard their reference to it but leave its contents intact;
     * this Attachment object assumes responsibility for clearing and discarding the secret data.
     *
     * @param fileContent the new file content (plaintext)
     * @param savedTime when the change was saved
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    private void setFileContent(byte[] fileContent, Date savedTime) {
        checkNotDestroyed();
        boolean changed = !Arrays.equals(fileContent, _fileContent);
        clearFileContent();
        _fileContent = fileContent;
        if (changed) {
            _fileContentLastChanged = savedTime;
        }
    }

    /**
     * Get the date and time which the file content field was last changed,
     * or the initial entry creation time if never changed.
     *
     * @return date and time, never null.
     */
    public Date getFileContentLastChanged() {
        return (Date) _fileContentLastChanged.clone();
    }

    /**
     * Sort by _filename then by _description.
     * <p>
     * NOTE: this is inconsistent with equals() in that equals() tests object equality (since it is used for
     * {@link AttachmentList#remove(Attachment)}, and there is no restriction on entries having the same
     * filename and description) but this compares filenames and descriptions so they may be sorted.
     * This should not be a problem for the intended uses of this class.
     */
    public int compareTo(Attachment other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (this == other) {
            return 0;
        }
        // Sort by filename
        int compare = _filename.compareTo(other._filename);
        if (compare != 0) {
            return compare;
        }
        // Same filename - sort by description; null entries considered greater
        String otherDescription = other._description;
        if (_description == null) {
            if (otherDescription != null) {
                return 1;
            }
        } else {
            if (otherDescription != null) {
                compare = _description.compareTo(otherDescription);
                if (compare != 0) {
                    return compare;
                }
            } else {
                return -1;
            }
        }
        return 0;
    }

    /**
     * Get String to display in the list in {@link pwsafe.gui.MainWindow}
     *
     * @return string containing filename and part of description
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(abbreviate(_filename, 20));
        if (_description.length() > 0) {
            b.append(" (");
            b.append(abbreviate(_description, 20));
            b.append(')');
        }
        return b.toString();
    }

    private String abbreviate(String s, int maxLength) {
        int actualLen = s.length();
        if (actualLen <= maxLength) {
            return s;
        }
        String continuer = "...";
        return s.substring(0, maxLength - continuer.length()) + continuer;
    }

    /**
     * Zero-overwrite and discard the secret file content field, if any.
     */
    private void clearFileContent() {
        if (_fileContent != null) {
            Arrays.fill(_fileContent, (byte) 0);
            _fileContent = null;
        }
    }

    /**
     * Explicit serialization to guarantee we can handle old versions if implementation evolves
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            checkNotDestroyed();
        } catch (IllegalStateException e) {
            IOException ioe = new IOException("Nothing to serialize - secrets already destroyed");
            ioe.initCause(e);
            throw ioe;
        }
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readObject to support both old and new */
        out.writeObject(_filename);
        out.writeObject(_attachmentCreated);
        out.writeObject(_fileContent);
        out.writeObject(_fileContentLastChanged);
        out.writeObject(_description);
    }

    /**
     * Explicit deserialization to guarantee we can handle old versions if implementation evolves
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte version = in.readByte();
        // Add new versions here when changing writeObject / VERSION field
        switch (version) {
            case 0x1:
                readObjectVersion1(in);
                break;
            default:
                throw new InvalidClassException(getClass().getName(),
                        "The VERSION '" + version + "' was read from the stream "
                        + "but is not supported by the Attachment.readObject implementation");
        }
    }

    private void readObjectVersion1(ObjectInputStream in) throws IOException, ClassNotFoundException {
        _destroyed = false;
        _filename               = (String) in.readObject();
        _attachmentCreated      = (Date)   in.readObject();
        _fileContent            = (byte[]) in.readObject();
        _fileContentLastChanged = (Date)   in.readObject();
        _description            = (String) in.readObject();
    }

    /**
     * Zero-overwrite and discard the secret file content field, if any.
     * This method can safely be called repeatedly.
     * Once called, getFileContent method is no longer available.
     */
    public void destroySecrets() {
        clearFileContent();
        _destroyed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        destroySecrets();
    }
}