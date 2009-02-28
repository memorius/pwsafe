package pwsafe.store;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper for a list of PasswordStore objects to be serialized as a unit
 *
 * @author Nick Clarke
 */
public class PasswordStoreList implements Serializable {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeObject(ObjectOutputStream)
     */
    private static final long serialVersionUID = 3569512750172916679L;

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


    private List<PasswordStore> _stores;

    /**
     * Construct a PasswordStoreList
     */
    public PasswordStoreList() {
        _stores = new ArrayList<PasswordStore>();
    }

    /**
     * Get the current list of stores
     *
     * @return non-null List (unmodifiable) in sorted order.
     *         The returned list is a new copy and will not change when {@link #addStore(String)}
     *         or {@link #removeStore(PasswordStore)} are called.
     */
    public List<PasswordStore> getStores() {
        /* We can't use a sorted collection because the entries are mutable, so we must sort each time we want
           a snapshot sorted according to their current fields */
        Collections.sort(_stores);
        // We make a copy because addEntry/removeEntry would otherwise break this returned list
        return Collections.unmodifiableList(new ArrayList<PasswordStore>(_stores));
    }

    /**
     * Add a new store.
     * <p>
     * Note that caller must call setKey on the new store before it can be saved.
     *
     * @param storeName the name for the new store, must not be null or empty
     * @throws IllegalArgumentException if storeName is null or empty
     */
    public PasswordStore addStore(String storeName) {
        PasswordStore newStore = new PasswordStore(storeName);
        _stores.add(newStore);
        return newStore;
    }

    /**
     * Remove an existing store.
     *
     * @param store the existing store to remove
     * @throws IllegalArgumentException if store is null or is not present
     */
    public void removeStore(PasswordStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        if (!_stores.remove(store)) {
            throw new IllegalArgumentException("Store not present");
        }
    }

    /**
     * Explicit serialization to guarantee we can handle old versions if implementation evolves
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readExternal to support both old and new */
        out.writeInt(_stores.size());
        for (PasswordStore store : _stores) {
            out.writeObject(store);
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
                        + "but is not supported by the PasswordStoreList.readObject implementation");
        }
    }

    private void readObjectVersion1(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int count = in.readInt();
        _stores = new ArrayList<PasswordStore>();
        for (int i = 0; i < count; i++) {
            _stores.add((PasswordStore) in.readObject());
        }
    }
}
