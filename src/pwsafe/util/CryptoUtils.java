package pwsafe.util;

import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.SerpentEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Static utility methods for encryption/decryption
 *
 * @author Nick Clarke
 */
public final class CryptoUtils {

    /**
     * Static utility methods only, no instances
     */
    private CryptoUtils() {}


    private static final long PASSWORD_HASH_ITERATION_TIME_MILLIS = 500; // min time to hash when encrypting
    private static final int PASSWORD_SALT_LENGTH_BYTES = 32; // 256-bit
    private static final int NONCE_LENGTH_BYTES         = 32; // 256-bit
    private static final int MAC_LENGTH_BYTES           = 16; // 128-bit - this EAX impl can't do > cipher block size
    private static final int BYTES_PER_INTEGER = 4;
    private static final int PREAMBLE_LENGTH = PASSWORD_SALT_LENGTH_BYTES + BYTES_PER_INTEGER + NONCE_LENGTH_BYTES;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Encrypt arbitrary data with the supplied key. To decrypt again, supply the output and the same key
     * to the {@link #decrypt(byte[], EncryptionKey)} method.
     *
     * @param data the data to encrypt, must not be null
     * @param key the key to encrypt with, must not be null
     * @throws CryptoException for any problem with performing the encryption
     */
    public static byte[] encrypt(byte[] data, EncryptionKey key) throws CryptoException {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }

        // TODO: refactor / tidy

        // Password salt info, see hashPasswordToKey
        byte[] passwordSalt = randomBytes(PASSWORD_SALT_LENGTH_BYTES);

        KeyWithIterationCount keyInfo;
        try {
            keyInfo = key.calibrateAndMakeKey(passwordSalt, PASSWORD_HASH_ITERATION_TIME_MILLIS);
        } catch (IllegalStateException e) {
            throw new CryptoException("Invalid key", e);
        }
        byte[] encryptionKey = keyInfo.getKey();
        final int passwordHashIterations = keyInfo.getIterationCount();

        try {
            /* Note the security relies on never repeating the same nonce with a given key.
               The nonce used is a 256-bit random value which should make it sufficiently unlikely.
               (We can't use a counter because we have no secure space to store the counter value:
               we must assume that the attacker could replace the encrypted data with an older version,
               which would make us reuse the subsequent counter value at next encryption.)
            */
            byte[] nonce = randomBytes(NONCE_LENGTH_BYTES);

            // Encryption-related data included in authentication
            byte[] associatedData = new byte[PASSWORD_SALT_LENGTH_BYTES + BYTES_PER_INTEGER];
            System.arraycopy(passwordSalt, 0, associatedData, 0, passwordSalt.length);

            int pos = passwordSalt.length;
            associatedData[pos++] = (byte) (((passwordHashIterations >> (3 * Byte.SIZE)) & 0xFF) + Byte.MIN_VALUE);
            associatedData[pos++] = (byte) (((passwordHashIterations >> (2 * Byte.SIZE)) & 0xFF) + Byte.MIN_VALUE);
            associatedData[pos++] = (byte) (((passwordHashIterations >> Byte.SIZE) & 0xFF) + Byte.MIN_VALUE);
            associatedData[pos++] = (byte) ((passwordHashIterations & 0xFF) + Byte.MIN_VALUE);
            assert (pos == associatedData.length);

            AEADBlockCipher cipher = new EAXBlockCipher(new SerpentEngine());
            cipher.init(true, new AEADParameters(new KeyParameter(encryptionKey),
                                                 MAC_LENGTH_BYTES * Byte.SIZE,
                                                 nonce,
                                                 associatedData));

            byte[] encrypted = new byte[PREAMBLE_LENGTH + cipher.getOutputSize(data.length)];

            // Associated data is the first part of the output
            int offset = 0;
            System.arraycopy(associatedData, 0, encrypted, offset, associatedData.length);
            offset += associatedData.length;

            // Encryption nonce becomes the next part of the output - it is required for decryption; it is not secret
            System.arraycopy(nonce, 0, encrypted, offset, nonce.length);
            offset += nonce.length;

            // Generate the encrypted output and the MAC
            try {
                assert (offset == PREAMBLE_LENGTH);
                offset += cipher.processBytes(data, 0, data.length, encrypted, offset);
                offset += cipher.doFinal(encrypted, offset);
                assert (offset == encrypted.length);
            } catch (IllegalStateException e) {
                // Shouldn't happen if we are using the cipher correctly
                throw new CryptoException("Unexpected error during encryption", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                // Shouldn't happen if we are using the cipher correctly
                throw new CryptoException("Unexpected error during encryption", e);
            } catch (DataLengthException e) {
                // Shouldn't happen if we are using the cipher correctly
                throw new CryptoException("Unexpected error during encryption", e);
            } catch (InvalidCipherTextException e) {
                // Shouldn't happen during encryption, only decryption
                throw new CryptoException("Unexpected error during encryption", e);
            }

            return encrypted;
        } finally {
            Arrays.fill(encryptionKey, (byte) 0);
            encryptionKey = null;
        }
    }

    public static byte[] decrypt(byte[] encrypted, EncryptionKey key) throws CryptoException {
        if (encrypted == null) {
            throw new IllegalArgumentException("encrypted must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (encrypted.length < (PREAMBLE_LENGTH + MAC_LENGTH_BYTES)) {
            throw new CryptoException("Encrypted data not long enough: got " + encrypted.length + " bytes");
        }

        // Encryption-related data included in authentication
        byte[] associatedData = new byte[PASSWORD_SALT_LENGTH_BYTES + BYTES_PER_INTEGER];
        System.arraycopy(encrypted, 0, associatedData, 0, associatedData.length);

        int offset = 0;

        // Password salt info, see hashPasswordToKey
        byte[] passwordSalt = new byte[PASSWORD_SALT_LENGTH_BYTES];
        System.arraycopy(associatedData, offset, passwordSalt, 0, passwordSalt.length);
        offset += passwordSalt.length;

        int passwordHashIterations = 0;
        passwordHashIterations = ((((int) associatedData[offset++]) - Byte.MIN_VALUE) << (3 * Byte.SIZE))
                               + ((((int) associatedData[offset++]) - Byte.MIN_VALUE) << (2 * Byte.SIZE))
                               + ((((int) associatedData[offset++]) - Byte.MIN_VALUE) << Byte.SIZE)
                               + (((int) associatedData[offset++]) - Byte.MIN_VALUE);
        if (passwordHashIterations <= 0) {
            throw new CryptoException("Invalid passwordHashIterations in encrypted data, got " + passwordHashIterations);
        }

        byte[] encryptionKey;
        try {
            encryptionKey = key.makeKey(passwordSalt, passwordHashIterations);
        } catch (IllegalStateException e) {
            throw new CryptoException("Invalid key", e);
        }

        try {
            // Nonce received as the next part of the input - it is required for decryption; it is not secret
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            System.arraycopy(encrypted, offset, nonce, 0, nonce.length);
            offset += nonce.length;

            AEADBlockCipher cipher = new EAXBlockCipher(new SerpentEngine());
            cipher.init(false, new AEADParameters(new KeyParameter(encryptionKey),
                                                  MAC_LENGTH_BYTES * Byte.SIZE,
                                                  nonce,
                                                  associatedData));

            int encryptedDataLength = encrypted.length - PREAMBLE_LENGTH;
            byte[] decrypted = new byte[cipher.getOutputSize(encryptedDataLength)];

            /* Decrypt and verify the MAC (which is included in the ciphertext in CCM mode),
               and return the plaintext */
            try {
                assert (offset == PREAMBLE_LENGTH);
                int outputOffset = 0;
                outputOffset += cipher.processBytes(encrypted, offset, encryptedDataLength, decrypted, outputOffset);
                outputOffset += cipher.doFinal(decrypted, outputOffset);
                assert (outputOffset == decrypted.length);
            } catch (IllegalStateException e) {
                // Shouldn't happen if we are using the cipher correctly
                throw new CryptoException("Unexpected error during decryption", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                // Shouldn't happen if we are using the cipher correctly
                throw new CryptoException("Unexpected error during decryption", e);
            } catch (DataLengthException e) {
                // Shouldn't happen if we are using the cipher correctly
                throw new CryptoException("Unexpected error during decryption", e);
            } catch (InvalidCipherTextException e) {
                throw new CryptoException("Invalid key or corrupted ciphertext", e);
            }

            return decrypted;
        } finally {
            Arrays.fill(encryptionKey, (byte) 0);
            encryptionKey = null;
        }
    }

    /**
     * Create an array of the specified length and fill with random bytes using java.security.SecureRandom.
     *
     * @param lengthBytes the length of the array to create, must be > 0
     * @return a newly-created array, filled with random bytes
     * @throws IllegalArgumentException if length <= 0
     */
    public static byte[] randomBytes(int lengthBytes) {
        if (lengthBytes <= 0) {
            throw new IllegalArgumentException("lengthBytes must be > 0, got " + lengthBytes);
        }
        byte[] output = new byte[lengthBytes];
        RANDOM.nextBytes(output);
        return output;
    }

    /**
     * Hash the password with the specified salt, then re-hash for the specified number of iterations.
     * Used during decryption.
     * <p>
     * Hashing the password many times (and using a salt) gives some degree of protection against use of
     * rainbow-table attacks. (These would work by testing each password in some pre-generated list of (password, hash),
     * by testing whether decryption succeeds using each hash.) Using a new random salt each time we encrypt forces a
     * new table to be built to attack each new encrypted output; using many iterations increases the cost of building
     * the table.
     *
     * @param password, will be transformed into its UTF-16 bytes before hashing
     * @param salt a random salt to hash with the password
     * @param hashIterations the iteration count, must be > 0
     */
    public static byte[] hashPasswordToKey(char[] password, byte[] salt, int hashIterations) {
        if (hashIterations <= 0) {
            throw new IllegalArgumentException("hashIterations must be > 0, got " + hashIterations);
        }
        KeyWithIterationCount keyInfo = doHashPasswordToKey(password, salt, hashIterations, -1);
        assert (keyInfo.getIterationCount() == hashIterations);
        return keyInfo.getKey();
    }

    /**
     * Hash the password with the specified salt, then re-hash continuously until the specified time has elapsed,
     * returning the resulting key and the actualy iteration count used.
     * Used during encryption.
     * <p>
     * Hashing the password many times (and using a salt) gives some degree of protection against use of
     * rainbow-table attacks. (These would work by testing each password in some pre-generated list of (password, hash),
     * by testing whether decryption succeeds using each hash.) Using a new random salt each time we encrypt forces a
     * new table to be built to attack each new encrypted output; using many iterations increases the cost of building
     * the table.
     *
     * @param password, will be transformed into its UTF-16 bytes before hashing
     * @param salt a random salt to hash with the password
     * @param hashIterationTimeMillis the time to hash for, must be > 0
     */
    public static KeyWithIterationCount calibrateAndHashPasswordToKey(char[] password, byte[] salt,
            long hashIterationTimeMillis) {
        if (hashIterationTimeMillis <= 0L) {
            throw new IllegalArgumentException("hashIterationTimeMillis must be > 0, got " + hashIterationTimeMillis);
        }
        KeyWithIterationCount keyInfo = doHashPasswordToKey(password, salt, -1, hashIterationTimeMillis);
        assert (keyInfo.getIterationCount() > 0);
        return keyInfo;
    }

    private static KeyWithIterationCount doHashPasswordToKey(char[] password, byte[] salt,
            int hashIterations, long hashIterationTimeMillis) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (salt == null) {
            throw new IllegalArgumentException("salt must not be null");
        }
        if (password.length == 0) {
            throw new IllegalArgumentException("password must not be zero length");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("salt must not be zero length");
        }
        assert (hashIterations > 0 || hashIterationTimeMillis > 0);

        /* Convert to a byte array according to the scheme in PKCS12 (unicode, big endian, 2 zero pad bytes at the end).
           This call avoids allocating a java.lang.String which might intern the password data,
           which would prevent us zeroing it. */
        byte[] passwordBytes = PBEParametersGenerator.PKCS12PasswordToBytes(password);

        Digest digest = new SHA256Digest();
        byte[] output = new byte[digest.getDigestSize()];
        byte[] input = new byte[digest.getDigestSize()];

        // First iteration - hash with salt, into output
        digest.update(salt, 0, salt.length);
        digest.update(passwordBytes, 0, passwordBytes.length);
        // Note that doFinal also resets the digest's internal state, like constructing a new one
        int outputLength = digest.doFinal(output, 0);
        assert (outputLength == output.length);

        // Remaining iterations - re-hash output
        if (hashIterations > 0) {
            // Fixed iteration count (decryption)
            for (int i = 1; i < hashIterations; i++) { // 0th iteration was the one with the salt
                // input this time is the output from the last iteration; then reuse (overwrite) output array
                byte[] temp = input;
                input = output;
                output = temp;
                digest.update(input, 0, input.length);
                outputLength = digest.doFinal(output, 0);
                assert (outputLength == output.length);
            }
            return new KeyWithIterationCount(output, hashIterations);
        } else {
            // Minimum iteration time (encryption)
            final int iterationIncrement = 100;
            int actualHashIterations = 1; // 0th iteration was the one with the salt
            final long stopTime = System.currentTimeMillis() + hashIterationTimeMillis;
            do {
                for (int i = 0; i < iterationIncrement; i++) {
                    // input this time is the output from the last iteration; then reuse (overwrite) output array
                    byte[] temp = input;
                    input = output;
                    output = temp;
                    digest.update(input, 0, input.length);
                    outputLength = digest.doFinal(output, 0);
                    assert (outputLength == output.length);
                }
                actualHashIterations += iterationIncrement;
            } while (System.currentTimeMillis() <= stopTime);
            return new KeyWithIterationCount(output, actualHashIterations);
        }
    }

    /**
     * Value object for returning multiple arguments from crypto methods
     */
    public static final class KeyWithIterationCount {

        private final byte[] _key;
        private final int _iterationCount;

        private KeyWithIterationCount(final byte[] key, final int iterationCount) {
            _key = key;
            _iterationCount = iterationCount;
        }

        public byte[] getKey() {
            return _key;
        }

        public int getIterationCount() {
            return _iterationCount;
        }
    }

    public static final class CryptoException extends Exception {
        /**
         * serialVersionUID for this class.
         */
        private static final long serialVersionUID = 2060162442896562948L;

        public CryptoException(final String message) {
            super(message);
        }

        public CryptoException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
