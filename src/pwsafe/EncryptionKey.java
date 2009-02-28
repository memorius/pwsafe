package pwsafe;

import java.util.Arrays;

import pwsafe.util.CryptoUtils;

/**
 * Wrapper to make encryption key bytes from plaintext password.
 * This is the master key for locking/unlocking the encrypted PasswordStores, not the passwords stored within it.
 * Takes responsibility for zeroing the password data bytes when destroyed
 *
 * @author Nick Clarke
 */
public class EncryptionKey {

    private char[] _password;

    /**
     * Construct a EncryptionKey
     * <p>
     * <b>IMPORTANT:</b> this stores references to the supplied password, it does not make copies.
     * The caller should discard their reference to this array but leave the contents intact;
     * this EncryptionKey object assumes responsibility for clearing and discarding the secret data.
     *
     * @param password the secret password, must not be null or empty
     * @throws IllegalArgumentException if password is null or empty
     */
    public EncryptionKey(final char[] password) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (password.length == 0) {
            throw new IllegalArgumentException("password must not be zero length");
        }
        _password = password;
    }

    /**
     * @throws IllegalStateException if {@link #destroySecrets()} has already been called
     */
    private void checkNotDestroyed() {
        if (_password == null) {
            throw new IllegalStateException("destroySecrets() has already been called");
        }
    }

    /**
     * Make an encryption key (secret) by hashing the password contained in this EncryptionKey, given parameters for the
     * hashing algorithm.
     * <p>
     * <b>IMPORTANT:</b> this returns a newly-allocated array which is the caller's responsibility.
     * For security, caller should avoid storing references longer than necessary,
     * and MUST ensure it is zero-overwritten and discarded when finished.
     *
     * @return a new array containing the key bytes, never null or empty
     * @throws IllegalStateException if {@link #destroySecrets()} method has been called
     */
    public byte[] makeKey(byte[] salt, int hashIterations) {
        checkNotDestroyed();
        return CryptoUtils.hashPasswordToKey(_password, salt, hashIterations);
    }

    /**
     * Zero-overwrite and discard the secret key data.
     * This method can safely be called repeatedly.
     * Once called, getKey method is no longer available.
     */
    public void destroySecrets() {
        if (_password != null) {
            Arrays.fill(_password, (char) 0);
            _password = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroySecrets();
    }
}
