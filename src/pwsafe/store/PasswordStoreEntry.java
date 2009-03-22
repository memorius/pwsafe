package pwsafe.store;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

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
    private Date _entryCreated;
    private String _userID;
    private Date _userIDLastChanged;
    private char[] _password;
    private Date _passwordLastChanged;
    private char[] _additionalInfo;
    private Date _additionalInfoLastChanged;

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
     * @param userID the account username / login name, can be empty but not null
     * @param password the account password, can be empty but not null
     * @param additionalInfo free-form text to record any extra login info for this account,
     *         e.g. additional security questions, can be empty but not null
     * @throws IllegalArgumentException if any argument is null, or if displayName is empty
     */
    protected PasswordStoreEntry(
            final String displayName,
            final String userID,
            final char[] password,
            final char[] additionalInfo) {
        final Date now = new Date();
        checkDisplayName(displayName);
        if (userID == null) {
            throw new IllegalArgumentException("userID must not be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (additionalInfo == null) {
            throw new IllegalArgumentException("additionalInfo must not be null");
        }
        _displayName = displayName;
        _userID = userID;
        _password = password;
        _additionalInfo = additionalInfo;
        _entryCreated = now;
        _userIDLastChanged = now;
        _passwordLastChanged = now;
        _additionalInfoLastChanged = now;
    }

    /**
     * Save all fields at once, with the same timestamp for any modified fields.
     * <p>
     * <b>IMPORTANT:</b> this stores references to the supplied password and additionalInfo arrays (if any),
     * it does not make copies.
     * The caller should discard their references to these arrays but leave their contents intact;
     * this PasswordStoreEntry object assumes responsibility for clearing and discarding the secret data.
     *
     *
     * @param displayName name to appear in list of entries, typically a website address or company name,
     *         must not be null or empty
     * @param userID the account username / login name, can be empty but not null
     * @param password the account password, can be empty but not null
     * @param additionalInfo free-form text to record any extra login info for this account,
     *         e.g. additional security questions, can be empty but not null
     * @throws IllegalArgumentException if any argument is null, or if displayName is empty
     */
    public void setAllFields(final String displayName,
                             final String userID,
                             final char[] password,
                             final char[] additionalInfo) {
        final Date now = new Date();
        if (userID == null) {
            throw new IllegalArgumentException("userID must not be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (additionalInfo == null) {
            throw new IllegalArgumentException("additionalInfo must not be null");
        }
        checkNotDestroyed();
        setDisplayName(displayName);
        setUserID(userID, now);
        setPassword(password, now);
        setAdditionalInfo(additionalInfo, now);
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

    private boolean stringsEqual(String s1, String s2) {
        if (s1 == null) {
            return (s2 == null);
        } else {
            return s1.equals(s2);
        }
    }

    /**
     * Get the date and time which this record was first created.
     *
     * @return date and time, never null.
     */
    public Date getEntryCreated() {
        return (Date) _entryCreated.clone();
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
    private void setDisplayName(String displayName) {
        checkDisplayName(displayName);
        _displayName = displayName;
    }

    /**
     * Get the account username / login name for this record.
     *
     * @return non-null username, can be empty
     */
    public String getUserID() {
        return _userID;
    }

    /**
     * Set the account username / login name for this record.
     *
     * @param userID the new account username / login name
     * @param savedTime when the change was saved
     */
    private void setUserID(String userID, Date savedTime) {
        if (!stringsEqual(userID, _userID)) {
            _userIDLastChanged = savedTime;
        }
        _userID = userID;
    }

    /**
     * Get the date and time which the userID field was last changed,
     * or the initial entry creation time if never changed.
     *
     * @return date and time, never null.
     */
    public Date getUserIDLastChanged() {
        return (Date) _userIDLastChanged.clone();
    }

    /**
     * Get the account password (secret) for this record, if any.
     * <p>
     * <b>IMPORTANT:</b> this returns the internal array itself, not a copy,
     * so any modifications to the array will be written back to the store with this record.
     * <p>
     * For security, caller should avoid storing references longer than necessary,
     * and if the caller copies the data, it MUST ensure it is zero-overwritten and discarded when finished.
     * <p>
     * Assuming the caller does not make a copy, this PasswordStoreEntry retains responsibility for clearing
     * and discarding the secret data, and caller should not zero it.
     *
     * @return non-null password (plaintext), can be empty
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
     * @param password the new password (plaintext)
     * @param savedTime when the change was saved
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    private void setPassword(char[] password, Date savedTime) {
        checkNotDestroyed();
        boolean changed = !Arrays.equals(password, _password);
        clearPassword();
        _password = password;
        if (changed) {
            _passwordLastChanged = savedTime;
        }
    }

    /**
     * Get the date and time which the password field was last changed,
     * or the initial entry creation time if never changed.
     *
     * @return date and time, never null.
     */
    public Date getPasswordLastChanged() {
        return (Date) _passwordLastChanged.clone();
    }

    /**
     * Get the additional login info (secret) for this record, if any.
     * <p>
     * <b>IMPORTANT:</b> this returns the internal array itself, not a copy,
     * so any modifications to the array will be written back to the store with this record.
     * <p>
     * For security, caller should avoid storing references longer than necessary,
     * and if the caller copies the data, it MUST ensure it is zero-overwritten and discarded when finished.
     * <p>
     * Assuming the caller does not make a copy, this PasswordStoreEntry retains responsibility for clearing
     * and discarding the secret data, and caller should not zero it.
     *
     * @return non-null additional login info (plaintext), can be empty.
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
     * @param additionalInfo the new additional login info (plaintext), can be empty
     * @param savedTime when the change was saved
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    private void setAdditionalInfo(char[] additionalInfo, Date savedTime) {
        checkNotDestroyed();
        boolean changed = !Arrays.equals(additionalInfo, _additionalInfo);
        clearAdditionalInfo();
        _additionalInfo = additionalInfo;
        if (changed) {
            _additionalInfoLastChanged = savedTime;
        }
    }

    /**
     * Get the date and time which the additional info field was last changed,
     * or the initial entry creation time if never changed.
     *
     * @return date and time, never null.
     */
    public Date getAdditionalInfoLastChanged() {
        return (Date) _additionalInfoLastChanged.clone();
    }

    /**
     * Sort by _displayName then by _userID.
     * <p>
     * NOTE: this is inconsistent with equals() in that equals() tests object equality (since it is used for
     * {@link PasswordStoreEntryList#remove(PasswordStoreEntry)}, and there is no restriction on entries having the same
     * name and userID) but this compares names and userIDs so they may be sorted.
     * This should not be a problem for the intended uses of this class.
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
        return 0;
    }

    /**
     * Get String to display in the list in {@link pwsafe.gui.MainWindow}
     *
     * @return the result of {@link #getDisplayName()}
     */
    @Override
    public String toString() {
        return getDisplayName();
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
            IOException ioe = new IOException("Nothing to serialize - secrets already destroyed");
            ioe.initCause(e);
            throw ioe;
        }
        out.writeByte(VERSION);
        /* For backward-compatible deserialization, change only the part below, and change VERSION value at top of file,
           and change readObject to support both old and new */
        out.writeObject(_displayName);
        out.writeObject(_entryCreated);
        out.writeObject(_userID);
        out.writeObject(_userIDLastChanged);
        out.writeObject(_password);
        out.writeObject(_passwordLastChanged);
        out.writeObject(_additionalInfo);
        out.writeObject(_additionalInfoLastChanged);
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
        _displayName               = (String) in.readObject();
        _entryCreated              = (Date)   in.readObject();
        _userID                    = (String) in.readObject();
        _userIDLastChanged         = (Date)   in.readObject();
        _password                  = (char[]) in.readObject();
        _passwordLastChanged       = (Date)   in.readObject();
        _additionalInfo            = (char[]) in.readObject();
        _additionalInfoLastChanged = (Date)   in.readObject();
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
