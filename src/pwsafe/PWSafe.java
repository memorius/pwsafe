package pwsafe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pwsafe.gui.MainWindow;
import pwsafe.store.PasswordStoreList;
import pwsafe.util.IOUtils;
import pwsafe.util.SerializationUtils;

/**
 * Main lifecycle class - an instance of this encapsulates the password datastore file and dialogs etc
 *
 * @author Nick Clarke
 */
public class PWSafe {

    /* TODO:
         - timeout thread, discard password data after some interval, prompt if needed again
    */

    private final File _datastoreFile;
    private PasswordStoreList _passwordStores = null;

    /**
     * Construct a PWSafe
     */
    public PWSafe(final File datastoreFile) throws InitializationException {
        if (datastoreFile == null) {
            throw new IllegalArgumentException("datastoreFile must not be null");
        }
        _datastoreFile = datastoreFile;
        System.out.println("Password Safe datastore file: " + _datastoreFile);
        try {
            checkDatastoreFileAccessAndCreate();
        } catch (DatastoreFileException e) {
            throw new InitializationException(String.format("Invalid datastore file path '%s'", _datastoreFile), e);
        }
        try {
            load();
        } catch (DatastoreFileException e) {
            throw new InitializationException(String.format("Failed to load datastore file '%s'", _datastoreFile), e);
        }
    }

    public PasswordStoreList load() throws DatastoreFileException {
        // Destroy any existing secrets first
        if (_passwordStores != null) {
            _passwordStores.destroySecrets();
            _passwordStores = null;
        }
        if (_datastoreFile.length() > 0) {
            byte[] serialized;
            try {
                serialized = IOUtils.readFile(_datastoreFile);
            } catch (IOException e) {
                throw new DatastoreFileException(String.format("Failed to read datastore file '%s'", _datastoreFile), e);
            }
            try {
                _passwordStores = SerializationUtils.deserialize(serialized, PasswordStoreList.class);
            } catch (SerializationUtils.SerializationException e) {
                throw new DatastoreFileException("Failed to deserialize datastores", e);
            }
        } else {
            _passwordStores = new PasswordStoreList();
        }
        return _passwordStores;
    }

    public void save() throws DatastoreFileException {
        byte[] serialized;
        try {
            serialized = SerializationUtils.serialize(_passwordStores);
        } catch (SerializationUtils.SerializationException e) {
            throw new DatastoreFileException("Failed to serialize datastore", e);
        }
        try {
            IOUtils.writeFile(_datastoreFile, serialized);
        } catch (IOException e) {
            throw new DatastoreFileException(String.format("Failed to write datastore file '%s'", _datastoreFile), e);
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
        MainWindow window = new MainWindow(this, _passwordStores);
        window.setVisible(true);
        // Return and leave the dialog (AWT thread) as the only thing running - we exit when it is disposed
    }
}
