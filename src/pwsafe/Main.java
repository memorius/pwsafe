package pwsafe;

import java.io.File;

/**
 * Main entry point class
 *
 * @author Nick Clarke
 */
public class Main {

    private static final String DEFAULT_USER_HOME_DATA_FILE = ".pwsafe-datastore";

    /**
     * Static class to provide entry point, no instances
     */
    private Main() {}


    /**
     * Main entry point for launching this program
     */
    public static void main(String[] args) throws InitializationException {
        PWSafe safe = new PWSafe(getDatastoreFile(args));
        // TODO: maybe allow a commandline mode here
        safe.showDialog();
    }


    private static File getUserHomeDir() throws InitializationException {
        String userHomeDir = System.getProperty("user.home");
        if (userHomeDir == null) {
            throw new InitializationException("user.home system property not set");
        }
        File f = new File(userHomeDir);
        if (!f.isDirectory()) {
            throw new InitializationException(String.format(
                    "user.home property was '%s' but this path doesn't exist or is not a directory", f));
        }
        return f;
    }

    private static File getDefaultDatastoreFile() throws InitializationException {
        return new File(getUserHomeDir(), DEFAULT_USER_HOME_DATA_FILE);
    }

    private static File getDatastoreFile(String[] args) throws InitializationException {
        if (args.length == 1 && args[0].length() > 0) {
            return new File(args[0]);
        } else {
            return getDefaultDatastoreFile();
        }
    }
}
