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

import pwsafe.DecryptionException;
import pwsafe.EncryptionException;
import pwsafe.EncryptionKey;
import pwsafe.util.CryptoUtils;
import pwsafe.util.SerializationUtils;

/**
 * The data to be written to the datastore file
 *
 * @author Nick Clarke
 */
public final class PasswordStore implements Serializable, Comparable<PasswordStore> {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeObject(ObjectOutputStream)
     */
    private static final long serialVersionUID = 5544414134473964927L;

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


    private String _storeName;
    private byte[] _encryptedEntries;

    private transient EncryptionKey _key;
    private transient PasswordStoreEntryList _entries;

    /**
     * Construct an empty PasswordStore. The store will initially be unlocked.
     *
     * @throws IllegalArgumentException if storeName is null or empty
     */
    public PasswordStore(final String storeName) {
        checkStoreName(storeName);
        _storeName = storeName;
        _key = null;
        _encryptedEntries = null;
        _entries = new PasswordStoreEntryList();
    }

    /**
     * @throws IllegalArgumentException if storeName is null or empty
     */
    private void checkStoreName(String storeName) {
        if (storeName == null) {
            throw new IllegalArgumentException("storeName must not be null");
        }
        if ("".equals(storeName)) {
            throw new IllegalArgumentException("storeName cannot be empty");
        }
    }

    /**
     * Get the store name.
     *
     * @return non-null, non-empty String
     */
    public String getStoreName() {
        return _storeName;
    }

    /**
     * Set the store name.
     *
     * @param storeName the new name, must not be null or empty
     * @throws IllegalArgumentException if storeName is null or empty
     */
    public void setStoreName(String storeName) {
        checkStoreName(storeName);
        _storeName = storeName;
    }

    /**
     * Is the store currently locked?
     *
     * @return true if the store is locked, false if unlocked (which includes newly-created stores)
     */
    public boolean isLocked() {
        return _entries == null;
    }

    /**
     * Check the store is currently unlocked.
     *
     * @throws IllegalStateException if store has not been successfully unlocked
     */
    private void checkUnlocked() {
        if (isLocked()) {
            throw new IllegalStateException("Store must be unlocked first");
        }
    }

    /**
     * Check the store is currently locked.
     *
     * @throws IllegalStateException if store has already been unlocked
     */
    private void checkLocked() {
        if (!isLocked()) {
            throw new IllegalStateException("Already unlocked");
        }
        assert (_encryptedEntries != null);
    }

    /**
     * Change the key to be used when locking / storing. Must be unlocked first.
     *
     * @throws IllegalStateException if store has not been successfully unlocked
     */
    public void setKey(EncryptionKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        checkUnlocked();
        _key = key;
    }

    /**
     * Decrypt the data and unlock the store.
     * This will keep a reference to the key for use when locking again.
     *
     * @throws IllegalStateException if store has already been unlocked
     * @throws DecryptionException if decryption fails (e.g. due to incorrect key)
     */
    public void unlock(EncryptionKey key) throws DecryptionException {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        checkLocked();
        assert (_key == null);
        assert (_entries == null);
        assert (_encryptedEntries != null);
        decryptEntries(key);
        assert (_entries != null);
        // Successful - store the key for locking again later
        _key = key;
    }

    /**
     * Encrypt the data, lock the store and discard the unencrypted data and the key.
     * If the store is already locked, this does nothing.
     *
     * @throws EncryptionException if encryption fails
     */
    public void lock() throws EncryptionException {
        try {
            if (isLocked()) {
                assert (_key == null);
                assert (_entries == null);
                assert (_encryptedEntries != null);
                return;
            }
            if (_key == null) {
                // Happens for new stores - must call setKey before saving
                throw new EncryptionException("Key has not been set");
            }
            assert (_entries != null);
            encryptEntries(_key);
            assert (_encryptedEntries != null);
        } finally {
            destroySecrets();
            assert (_key == null);
            assert (_entries == null);
        }
    }

    private void decryptEntries(EncryptionKey key) throws DecryptionException {
        byte[] decrypted;
        try {
            decrypted = CryptoUtils.decrypt(_encryptedEntries, key);
        } catch (CryptoUtils.CryptoException e) {
            throw new DecryptionException("Failed to decrypt datastore", e);
        }
        try {
            _entries = SerializationUtils.deserialize(decrypted, PasswordStoreEntryList.class);
        } catch (SerializationUtils.SerializationException e) {
            throw new DecryptionException("Failed to deserialize datastore", e);
        }
    }

    private void encryptEntries(EncryptionKey key) throws EncryptionException {
        byte[] serialized;
        try {
            serialized = SerializationUtils.serialize(_entries);
        } catch (SerializationUtils.SerializationException e) {
            throw new EncryptionException("Failed to serialize datastore entries", e);
        }
        try {
            _encryptedEntries = CryptoUtils.encrypt(serialized, _key);
        } catch (CryptoUtils.CryptoException e) {
            throw new EncryptionException("Failed to encrypt datastore entries", e);
        }
    }

    /**
     * Get the current entries. Must be unlocked first.
     *
     * @return the entries in the store
     * @throws IllegalStateException if store has not been successfully unlocked
     */
    public PasswordStoreEntryList getEntryList() {
        checkUnlocked();
        return _entries;
    }

    /**
     * Check whether another object is a PasswordStore (not a subclass) and has the same storeName as this object.
     *
     * @param o the object to compare to this object
     * @return true if o is equal to this object (is a PasswordStore with the same storeName)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PasswordStore other = (PasswordStore) o;
        // _storeName never null
        if (!_storeName.equals(other._storeName)) {
            return false;
        }
        return true;
    }

    /**
     * Generate a hash code for this object.
     *
     * @return the hash code, which considers only the storeName field
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        final int init = 9386;
        int hash = init;
        // _displayName never null
        hash = prime * hash + _storeName.hashCode();
        return hash;
    }

    /**
     * Sort by _storeName
     */
    public int compareTo(PasswordStore other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (this == other) {
            return 0;
        }
        // Sort by store name (which is never null)
        int compare = _storeName.compareTo(other._storeName);
        if (compare != 0) {
            return compare;
        }
        // equals() would return true, must return 0 here
        return 0;
    }

    /**
     * Zero-overwrite and discard the contained password store entries.
     * This method can safely be called repeatedly.
     * Once called, entries are no longer available and {@link #unlock(EncryptionKey)} must be called.
     */
    public void destroySecrets() {
        if (_entries != null) {
            _entries.destroySecrets();
            _entries = null;
        }
        if (_key != null) {
            _key.destroySecrets();
            _key = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroySecrets();
    }

    /**
     * Explicit serialization to guarantee we can handle old versions if implementation evolves
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Encrypt if we haven't yet
        try {
            lock();
        } catch (EncryptionException e) {
            IOException ioe = new IOException("Encryption failed");
            ioe.initCause(e);
            throw ioe;
        }
        assert (_encryptedEntries != null);
        out.writeByte(VERSION);
        out.writeObject(_storeName);
        out.writeObject(_encryptedEntries);
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
                        + "but is not supported by the PasswordStore.readObject implementation");
        }
    }

    private void readObjectVersion1(ObjectInputStream in) throws IOException, ClassNotFoundException {
        _entries = null;
        _key = null;
        _storeName = (String) in.readObject();
        _encryptedEntries = (byte[]) in.readObject();
    }
}
