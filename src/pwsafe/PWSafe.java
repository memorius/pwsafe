package pwsafe;

import java.io.File;
import java.io.IOException;

/**
 * Main lifecycle class - an instance of this encapsulates the password datastore file and dialogs etc
 *
 * @author Nick Clarke
 */
public class PWSafe {

    private final File _datastoreFile;

    /**
     * Construct a PWSafe
     */
    public PWSafe(final File datastoreFile) throws InitializationException {
        if (datastoreFile == null) {
            throw new IllegalArgumentException("datastoreFile must not be null");
        }
        _datastoreFile = datastoreFile;
        try {
            checkDatastoreFileAccessAndCreate();
        } catch (DatastoreFileException e) {
            throw new InitializationException(String.format("Invalid datastore file path '%s'", datastoreFile), e);
        }
    }

    /**
     * Check datastore file is readable, writable, not a directory. It need not actually exist yet.
     *
     * @throws DatastoreFileException if invalid
     */
    private void checkDatastoreFileAccessAndCreate() throws DatastoreFileException {
        if (_datastoreFile.isDirectory()) {
            throw new DatastoreFileException(String.format(
                    "A directory exists at the datastore file path '%s'", _datastoreFile));
        }
        if (!_datastoreFile.exists()) {
            try {
                if (!_datastoreFile.createNewFile()) {
                    throw new DatastoreFileException(String.format(
                            "Another process already created the new datastore file '%s'", _datastoreFile));
                }
            } catch (IOException e) {
                    throw new DatastoreFileException(String.format(
                            "Failed to create new datastore file '%s'", _datastoreFile), e);
            }
        } else if (!_datastoreFile.canRead()) {
            throw new DatastoreFileException(String.format(
                    "File permissions don't allow reading datastore file path '%s'", _datastoreFile));
        } else if (!_datastoreFile.canWrite()) {
            throw new DatastoreFileException(String.format(
                    "File permissions don't allow writing datastore file path '%s'", _datastoreFile));
        }
    }

    public void showDialog() {
        System.out.println(_datastoreFile);
    }
}
