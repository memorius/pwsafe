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
 * Wrapper for the list of password store entries to be encrypted / decrypted as a unit
 *
 * @author Nick Clarke
 */
public final class PasswordStoreEntryList implements Serializable {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeObject(ObjectOutputStream)
     */
    private static final long serialVersionUID = -4524716653895245787L;

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


    private List<PasswordStoreEntry> _entries;

    /**
     * Construct an empty PasswordStoreEntryList
     */
    protected PasswordStoreEntryList() {
        _entries = new ArrayList<PasswordStoreEntry>();
    }

    /**
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    private void checkNotDestroyed() {
        if (_entries == null) {
            throw new IllegalStateException("destroySecrets() has already been called");
        }
    }

    /**
     * Get the current list of entries
     *
     * @return non-null List (unmodifiable) in sorted order.
     *         The returned list is a new copy and will not change when {@link #addEntry(String)}
     *         or {@link #removeEntry(PasswordStoreEntry)} are called.
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    public List<PasswordStoreEntry> getEntries() {
        checkNotDestroyed();
        /* We can't use a sorted collection because the entries are mutable, so we must sort each time we want
           a snapshot sorted according to their current fields */
        Collections.sort(_entries);
        // We make a copy because addEntry/removeEntry would otherwise break this returned list
        return Collections.unmodifiableList(new ArrayList<PasswordStoreEntry>(_entries));
    }

    /**
     * Create a new record in the store.
     *
     * @param displayName name to appear in list of entries, typically a website address or company name,
     *         must not be null or empty, but need not be unique.
     * @throws IllegalArgumentException if displayName is null or empty
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    public PasswordStoreEntry addEntry(String displayName) {
        checkNotDestroyed();
        PasswordStoreEntry entry = new PasswordStoreEntry(displayName, null, null, null);
        _entries.add(entry);
        return entry;
    }

    /**
     * Remove an existing entry from the store.
     *
     * @param entry the existing entry to remove
     * @throws IllegalArgumentException if entry is null or is not present
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    public void removeEntry(PasswordStoreEntry entry) {
        checkNotDestroyed();
        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
        if (!_entries.remove(entry)) {
            throw new IllegalArgumentException("Entry not present");
        }
    }

    /**
     * Explicit serialization to guarantee we can handle old versions if implementation evolves
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            checkNotDestroyed();
        } catch (IllegalStateException e) {
            IOException ioe = new IOException("Nothing to serialize");
            ioe.initCause(e);
            throw ioe;
        }
        assert (_entries != null);
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readExternal to support both old and new */
        out.writeInt(_entries.size());
        for (PasswordStoreEntry entry : _entries) {
            out.writeObject(entry);
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
                        + "but is not supported by the PasswordStoreEntryList.readObject implementation");
        }
    }

    private void readObjectVersion1(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int count = in.readInt();
        _entries = new ArrayList<PasswordStoreEntry>();
        for (int i = 0; i < count; i++) {
            _entries.add((PasswordStoreEntry) in.readObject());
        }
    }

    /**
     * Zero-overwrite and discard the contained password store entries.
     * This method can safely be called repeatedly.
     * Once called, entries are no longer available.
     */
    public void destroySecrets() {
        if (_entries != null) {
            Iterator<PasswordStoreEntry> i = _entries.iterator();
            while (i.hasNext()) {
                i.next().destroySecrets();
                i.remove();
            }
            _entries = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroySecrets();
    }
}
