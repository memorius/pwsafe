package pwsafe;

import java.io.Externalizable;
import java.io.InvalidClassException;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The data to be written to the datastore file
 *
 * @author Nick Clarke
 */
public final class PasswordStore implements Externalizable {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeExternal(ObjectOutput)
     */
    private static final long serialVersionUID = 5544414134473964927L;

    /**
     * Allows backward-compatible deserialization for this class.
     * <p>
     * For backward-compatibility, increment this when changing serialization implementation,
     * and update readExternal to handle the new and old versions.
     *
     * @see #writeExternal(ObjectOutput)
     * @see #readExternal(ObjectInput)
     */
    private static final byte VERSION = 0x1;


    private transient boolean _destroyed = false;

    private List<PasswordStoreEntry> _entries;

    /**
     * Construct an empty PasswordStore
     */
    public PasswordStore() {
        _entries = new ArrayList<PasswordStoreEntry>();
    }

    /**
     * Get the current list of entries
     *
     * @return non-null List (unmodifiable) in sorted order.
     *         The returned list is a new copy and will not change when {@link #addEntry(String)}
     *         or {@link #removeEntry(PasswordStoreEntry)} are called.
     */
    public List<PasswordStoreEntry> getEntries() {
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
     */
    public PasswordStoreEntry addEntry(String displayName) {
        PasswordStoreEntry entry = new PasswordStoreEntry(displayName, null, null, null);
        _entries.add(entry);
        return entry;
    }

    /**
     * Remove an existing entry from the store.
     *
     * @throws IllegalArgumentException if entry is null or is not present
     */
    public void removeEntry(PasswordStoreEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
        if (!_entries.remove(entry)) {
            throw new IllegalArgumentException("Entry not present");
        }
    }

    /**
     * Zero-overwrite and discard the contained password store entries.
     * This method can safely be called repeatedly.
     * Once called, entries are no longer available.
     */
    public void destroy() {
        Iterator<PasswordStoreEntry> i = _entries.iterator();
        while (i.hasNext()) {
            i.next().destroy();
            i.remove();
        }
        _destroyed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
    }

    /**
     * Explicit serialization to guarantee we can handle old versions if implementation evolves
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        if (_destroyed) {
            throw new NotSerializableException(getClass().getName() + " : destroy() has already been called");
        }
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readExternal to support both old and new */
        Collections.sort(_entries);
        out.writeInt(_entries.size());
        for (PasswordStoreEntry entry : _entries) {
            out.writeObject(entry);
        }
    }

    /**
     * Explicit deserialization to guarantee we can handle old versions if implementation evolves
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        byte version = in.readByte();
        // Add new versions here when changing writeExternal / VERSION field
        switch (version) {
            case 0x1:
                readExternalVersion1(in);
                break;
            default:
                throw new InvalidClassException(getClass().getName(),
                        "The VERSION '" + version + "' was read from the stream but is not supported by the readExternal implementation");
        }
    }

    private void readExternalVersion1(ObjectInput in) throws IOException, ClassNotFoundException {
        _destroyed = false;
        _entries = new ArrayList<PasswordStoreEntry>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            _entries.add((PasswordStoreEntry) in.readObject());
        }
        /* We can't use a sorted collection because the entries are mutable, so we must sort each time we want
           a snapshot sorted according to their current fields */
        Collections.sort(_entries);
    }
}
