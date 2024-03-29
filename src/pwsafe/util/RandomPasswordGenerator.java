package pwsafe.util;

import java.math.BigInteger;

/**
 * Generates random passwords with specified length and alphabet
 *
 * @author Nick Clarke
 */
public class RandomPasswordGenerator {

    private static final String DIGITS = "0123456789";
    private static final String LOWERCASE_ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String PUNCTUATION = "!@#$%^&*(){}[]<>,._-+=?/\\|;:'\"`~";


    // Initial field values are the defaults for the 'generate password' dialog.
    // Everything enabled with 20 character length gives > 128 bit complexity.
    private boolean _useLowercaseAlpha = true;
    private boolean _useUppercaseAlpha = true;
    private boolean _useDigits = true;
    private boolean _usePunctuation = true;
    private int _length = 20;

    /**
     * Construct a RandomPasswordGenerator
     */
    public RandomPasswordGenerator() {
    }

    public boolean getUseLowercaseAlpha() {
        return _useLowercaseAlpha;
    }

    public void setUseLowercaseAlpha(boolean useLowercaseAlpha) {
        _useLowercaseAlpha = useLowercaseAlpha;
    }

    public boolean getUseUppercaseAlpha() {
        return _useUppercaseAlpha;
    }

    public void setUseUppercaseAlpha(boolean useUppercaseAlpha) {
        _useUppercaseAlpha = useUppercaseAlpha;
    }

    public boolean getUseDigits() {
        return _useDigits;
    }

    public void setUseDigits(boolean useDigits) {
        _useDigits = useDigits;
    }

    public boolean getUsePunctuation() {
        return _usePunctuation;
    }

    public void setUsePunctuation(boolean usePunctuation) {
        _usePunctuation = usePunctuation;
    }

    /**
     * Get the length of the password to generate, in characters
     *
     * @return length the number of characters to be in the password, > 0
     */
    public int getLength() {
        return _length;
    }

    /**
     * Set the length of the password to generate, in characters
     *
     * @param length the number of characters to be in the password, must be > 0
     * @throws IllegalArgumentException if length <= 0
     */
    public void setLength(int length) {
        if (!(length >= 0)) {
            throw new IllegalArgumentException("Invalid length, must be >= 0, got " + length + ")");
        }
        _length = length;
    }

    /**
     * Get the size of the selected alphabet.
     *
     * @return number of different possible values per character with the currently-selected alphabet options, >= 0
     */
    public int getAlphabetSize() {
        return getAlphabet().length();
    }

    private String getAlphabet() {
        StringBuilder alphabet = new StringBuilder();
        if (_useDigits) {
            alphabet.append(DIGITS);
        }
        if (_useLowercaseAlpha) {
            alphabet.append(LOWERCASE_ALPHA);
        }
        if (_useUppercaseAlpha) {
            alphabet.append(UPPERCASE_ALPHA);
        }
        if (_usePunctuation) {
            alphabet.append(PUNCTUATION);
        }
        return alphabet.toString();
    }

    /**
     * @return greatest power of 2 <= (radix to the power of getLength());
     */
    public int getBitComplexity() {
        int radix = getAlphabetSize();
        if (radix == 0) {
            return 0;
        }
        BigInteger possibleValueCount = BigInteger.valueOf(radix).pow(_length);
        // Note that storing (2^4)-1 takes 4 bits, 2^4 takes 5 bits - we are rounding down to the nearest power of two
        return possibleValueCount.bitLength() - 1;
    }

    /**
     * @throws IllegalStateException if no character types are enabled
     */
    public char[] generatePassword() {
        char[] alphabet = getAlphabet().toCharArray();
        if (alphabet.length == 0) {
            throw new IllegalStateException("At least one character type must be enabled to generate passwords");
        }
        char[] password = new char[_length];
        for (int i = 0; i < password.length; i++) {
            password[i] = alphabet[CryptoUtils.randomInt(alphabet.length)];
        }
        return password;
    }

    /**
     * Create a generator with length and complexity set by examining an existing password
     * and which character alphabets appear in it.
     *
     * @param password can be null or empty. This method will not copy it or modify it, or store a reference
     */
    public static RandomPasswordGenerator configureFromPassword(char[] password) {
        RandomPasswordGenerator gen = new RandomPasswordGenerator();
        gen.setUseDigits(false);
        gen.setUseLowercaseAlpha(false);
        gen.setUseUppercaseAlpha(false);
        gen.setUsePunctuation(false);

        if (password == null) {
            gen.setLength(0);
        } else {
            gen.setLength(password.length);

            for (char c : password) {
                if (DIGITS.indexOf(c) != -1) {
                    gen.setUseDigits(true);
                } else if (LOWERCASE_ALPHA.indexOf(c) != -1) {
                    gen.setUseLowercaseAlpha(true);
                } else if (UPPERCASE_ALPHA.indexOf(c) != -1) {
                    gen.setUseUppercaseAlpha(true);
                } else if (PUNCTUATION.indexOf(c) != -1) {
                    gen.setUsePunctuation(true);
                } else {
                    /* TODO: what do we do with chars in other ranges? This includes spaces.
                             If there are any, could just treat as using our full alphabet range,
                             or as using full ascii range, or full unicode range, or just ignore it.
                             Ignoring for now. */
                }
                c = (char) 0;
            }
        }

        return gen;
    }
}
