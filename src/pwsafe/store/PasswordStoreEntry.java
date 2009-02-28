package pwsafe.store;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/**
 * A single account record.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 *
 * @author Nick Clarke
 */
public final class PasswordStoreEntry implements Serializable, Comparable<PasswordStoreEntry> {
    /**
     * serialVersionUID for this class.
     * <p>
     * For backward-compatibility, do NOT change this when changing serialization implementation:
     * change VERSION field instead.
     *
     * @see #writeObject(ObjectOutputStream)
     */
    private static final long serialVersionUID = 4653207431849795142L;

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

    private String _displayName;
    private String _userID;
    private char[] _password;
    private char[] _additionalInfo;

    /**
     * Construct a PasswordStoreEntry
     * <p>
     * <b>IMPORTANT:</b> this stores references to the supplied password and additionalInfo arrays (if any),
     * it does not make copies.
     * The caller should discard their references to these arrays but leave their contents intact;
     * this PasswordStoreEntry object assumes responsibility for clearing and discarding the secret data.
     *
     * @param displayName name to appear in list of entries, typically a website address or company name,
     *         must not be null or empty
     * @param userID the account username / login name, can be null or empty
     * @param password the account password, can be null or empty
     * @param additionalInfo free-form text to record any extra login info for this account,
     *         e.g. additional security questions, can be null or empty
     * @throws IllegalArgumentException if displayName is null or empty
     */
    protected PasswordStoreEntry(
            final String displayName,
            final String userID,
            final char[] password,
            final char[] additionalInfo) {
        checkDisplayName(displayName);
        _displayName = displayName;
        _userID = userID;
        _password = password;
        _additionalInfo = additionalInfo;
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
     * @throws IllegalArgumentException if displayName is null or empty
     */
    private void checkDisplayName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }
        if ("".equals(displayName)) {
            throw new IllegalArgumentException("displayName cannot be an empty string");
        }
    }

    /**
     * Get the display name for this record, to appear in list of password store entries.
     * This is typically a website address or company name.
     *
     * @return the display name, never null or empty
     */
    public String getDisplayName() {
        return _displayName;
    }

    /**
     * Set the display name for this record, to appear in list of password store entries.
     * This is typically a website address or company name.
     *
     * @param displayName the new display name, must not be null or empty
     * @throws IllegalArgumentException if displayName is null or empty
     */
    public void setDisplayName(String displayName) {
        checkDisplayName(displayName);
        _displayName = displayName;
    }

    /**
     * Get the account username / login name for this record.
     *
     * @return the username, can be null or empty
     */
    public String getUserID() {
        return _userID;
    }

    /**
     * Set the account username / login name for this record.
     *
     * @param userID the new account username / login name, can be null or empty
     */
    public void setUserID(String userID) {
        _userID = userID;
    }

    /**
     * Get the account password (secret) for this record, if any.
     * <p>
     * <b>IMPORTANT:</b> this returns the internal array itself, not a copy,
     * so any modifications to the array will be written back to the store with this entry.
     * <p>
     * For security, caller should avoid storing references longer than necessary,
     * and if the caller copies the data, it MUST ensure it is zero-overwritten and discarded when finished.
     * <p>
     * Assuming the caller does not make a copy, this PasswordStoreEntry retains responsibility for clearing
     * and discarding the secret data, and caller should not zero it.
     *
     * @return the password (plaintext), can be null or empty.
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    public char[] getPassword() {
        checkNotDestroyed();
        return _password;
    }

    /**
     * Set the account password (secret) for this record, if any.
     * The existing password (if any) will be zeroed and discarded.
     * <p>
     * <b>IMPORTANT:</b> this stores a reference to the supplied array, it does not make a copy.
     * The caller should discard their reference to it but leave its contents intact;
     * this PasswordStoreEntry object assumes responsibility for clearing and discarding the secret data.
     *
     * @param password the new password (plaintext), can be null or empty
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    public void setPassword(char[] password) {
        checkNotDestroyed();
        clearPassword();
        _password = password;
    }

    /**
     * Get the additional login info (secret) for this record, if any.
     * <p>
     * <b>IMPORTANT:</b> this returns the internal array itself, not a copy,
     * so any modifications to the array will be written back to the store with this entry.
     * <p>
     * For security, caller should avoid storing references longer than necessary,
     * and if the caller copies the data, it MUST ensure it is zero-overwritten and discarded when finished.
     * <p>
     * Assuming the caller does not make a copy, this PasswordStoreEntry retains responsibility for clearing
     * and discarding the secret data, and caller should not zero it.
     *
     * @return the additional login info (plaintext), can be null or empty.
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    public char[] getAdditionalInfo() {
        checkNotDestroyed();
        return _additionalInfo;
    }

    /**
     * Set the additional login info (secret) for this record, if any.
     * The existing additional login info (if any) will be zeroed and discarded.
     * <p>
     * <b>IMPORTANT:</b> this stores a reference to the supplied array, it does not make a copy.
     * The caller should discard their reference to it but leave its contents intact;
     * this PasswordStoreEntry object assumes responsibility for clearing and discarding the secret data.
     *
     * @param additionalInfo the new additional login info (plaintext), can be null or empty
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    public void setAdditionalInfo(char[] additionalInfo) {
        checkNotDestroyed();
        clearAdditionalInfo();
        _additionalInfo = additionalInfo;
    }

    /**
     * Check whether another object is a PasswordStoreEntry (not a subclass) and represents the same account as
     * this object. Two objects are for the same account if they have the same displayName and userID.
     *
     * @param o the object to compare to this object
     * @return true if o is equal to this object (is a PasswordStoreEntry with the same displayName and userID)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PasswordStoreEntry other = (PasswordStoreEntry) o;
        // _displayName never null
        if (!_displayName.equals(other._displayName)) {
            return false;
        }
        if (_userID == null) {
            if (other._userID != null) {
                return false;
            }
        } else if (!_userID.equals(other._userID)) {
            return false;
        }
        return true;
    }

    /**
     * Generate a hash code for this object.
     *
     * @return the hash code, which considers only the displayName and userID fields
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        final int init = 4708;
        int hash = init;
        // _displayName never null
        hash = prime * hash + _displayName.hashCode();
        hash = prime * hash + ((_userID == null) ? 0 : _userID.hashCode());
        return hash;
    }

    /**
     * Sort by _displayName then by _userID.
     */
    public int compareTo(PasswordStoreEntry other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (this == other) {
            return 0;
        }
        // Sort by display name
        int compare = _displayName.compareTo(other._displayName);
        if (compare != 0) {
            return compare;
        }
        // Same displayname - sort by userID; null entries considered greater
        String otherUserID = other._userID;
        if (_userID == null) {
            if (otherUserID != null) {
                return 1;
            }
        } else {
            if (otherUserID != null) {
                compare = _userID.compareTo(otherUserID);
                if (compare != 0) {
                    return compare;
                }
            } else {
                return -1;
            }
        }
        // equals() would return true, must return 0 here
        return 0;
    }

    /**
     * Zero-overwrite and discard the secret password field, if any.
     */
    private void clearPassword() {
        if (_password != null) {
            Arrays.fill(_password, (char) 0);
            _password = null;
        }
    }

    /**
     * Zero-overwrite and discard the secret additional-info field, if any.
     */
    private void clearAdditionalInfo() {
        if (_additionalInfo != null) {
            Arrays.fill(_additionalInfo, (char) 0);
            _additionalInfo = null;
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
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readObject to support both old and new */
        out.writeObject(_displayName);
        out.writeObject(_userID);
        out.writeObject(_password);
        out.writeObject(_additionalInfo);
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
                        + "but is not supported by the PasswordStoreEntry.readObject implementation");
        }
    }

    private void readObjectVersion1(ObjectInputStream in) throws IOException, ClassNotFoundException {
        _destroyed = false;
        _displayName    = (String) in.readObject();
        _userID         = (String) in.readObject();
        _password       = (char[]) in.readObject();
        _additionalInfo = (char[]) in.readObject();
    }

    /**
     * Zero-overwrite and discard the secret password and additional-info fields, if any.
     * This method can safely be called repeatedly.
     * Once called, getPassword / getAdditionalInfo methods are no longer available.
     */
    public void destroySecrets() {
        clearPassword();
        clearAdditionalInfo();
        _destroyed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        destroySecrets();
    }
}
