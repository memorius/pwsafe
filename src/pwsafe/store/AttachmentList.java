package pwsafe.store;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper for the list of attachment entries to be encrypted / decrypted as a unit
 *
 * @author Nick Clarke
 */
public final class AttachmentList implements Serializable {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeObject(ObjectOutputStream)
     */
    private static final long serialVersionUID = 3897778972618719399L;

    /**
     * Allows backward-compatible deserialization for this class.
     * <p>
     * For backward-compatibility, increment this when changing serialization implementation,
     * and update readExternal to handle the new and old versions.
     *
     * @see #writeObject(ObjectOutputStream)
     * @see #readObject(ObjectInputStream)
     */
    private static final byte VERSION = 0x1;


    private List<Attachment> _attachments;

    /**
     * Construct an empty AttachmentList
     */
    protected AttachmentList() {
        _attachments = new ArrayList<Attachment>();
    }

    /**
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    private void checkNotDestroyed() {
        if (_attachments == null) {
            throw new IllegalStateException("destroySecrets() has already been called");
        }
    }

    public boolean isEmpty() {
        return _attachments.isEmpty();
    }

    /**
     * Get the current list of attachments
     *
     * @return non-null List (unmodifiable) in sorted order.
     *         The returned list is a new copy and will not change when {@link #addAttachment(String)}
     *         or {@link #removeAttachment(Attachment)} are called.
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    public List<Attachment> getAttachments() {
        checkNotDestroyed();
        /* We can't use a sorted collection because the attachments are mutable, so we must sort each time we want
           a snapshot sorted according to their current fields */
        Collections.sort(_attachments);
        // We make a copy because addAttachment/removeAttachment would otherwise break this returned list
        return Collections.unmodifiableList(new ArrayList<Attachment>(_attachments));
    }

    /**
     * Create a new attachment in the store.
     *
     * @param filename name of the file to appear in list of attachments,
     *         must not be null or empty, but need not be unique.
     * @return the new attachment
     * @throws IllegalArgumentException if filename is null or empty
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    public Attachment addAttachment(String filename) {
        checkNotDestroyed();
        Attachment attachment = new Attachment(filename, new byte[0], "");
        _attachments.add(attachment);
        return attachment;
    }

    /**
     * Remove an existing attachment from the store.
     *
     * @param attachment the existing attachment to remove
     * @throws IllegalArgumentException if attachment is null or is not present
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    public void removeAttachment(Attachment attachment) {
        checkNotDestroyed();
        if (attachment == null) {
            throw new IllegalArgumentException("attachment must not be null");
        }
        if (!_attachments.remove(attachment)) {
            throw new IllegalArgumentException("Attachment not present");
        }
    }

    /**
     * Explicit serialization to guarantee we can handle old versions if implementation evolves
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            checkNotDestroyed();
        } catch (IllegalStateException e) {
            IOException ioe = new IOException("Nothing to serialize - destroySecrets has already been called");
            ioe.initCause(e);
            throw ioe;
        }
        assert (_attachments != null);
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readExternal to support both old and new */
        out.writeInt(_attachments.size());
        for (Attachment attachment : _attachments) {
            out.writeObject(attachment);
        }
    }

    /**
     * Explicit deserialization to guarantee we can handle old versions if implementation evolves
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte version = in.readByte();
        // Add new versions here when changing writeExternal / VERSION field
        switch (version) {
            case 0x1:
                readObjectVersion1(in);
                break;
            default:
                throw new InvalidClassException(getClass().getName(),
                        "The VERSION '" + version + "' was read from the stream "
                        + "but is not supported by the AttachmentList.readObject implementation");
        }
    }

    private void readObjectVersion1(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int count = in.readInt();
        _attachments = new ArrayList<Attachment>();
        for (int i = 0; i < count; i++) {
            _attachments.add((Attachment) in.readObject());
        }
    }

    /**
     * Zero-overwrite and discard the contained attachments.
     * This method can safely be called repeatedly.
     * Once called, attachments are no longer available.
     */
    public void destroySecrets() {
        if (_attachments != null) {
            Iterator<Attachment> i = _attachments.iterator();
            while (i.hasNext()) {
                i.next().destroySecrets();
                i.remove();
            }
            _attachments = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroySecrets();
    }
}
