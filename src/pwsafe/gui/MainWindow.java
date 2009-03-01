package pwsafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import pwsafe.DecryptionException;
import pwsafe.EncryptionException;
import pwsafe.EncryptionKey;
import pwsafe.PWSafe;
import pwsafe.store.PasswordStore;
import pwsafe.store.PasswordStoreList;

/**
 * Initial dialog
 *
 * @author Nick Clarke
 */
public class MainWindow extends JFrame implements ActionListener {

    // User-visible text
    private static final String TITLE = "Password Safe";
    private static final String UNLOCK_STORE_BUTTON_TEXT = "Unlock";
    private static final String CHANGE_STORE_PASSWORD_BUTTON_TEXT = "Change password";
    private static final String SET_STORE_PASSWORD_BUTTON_TEXT = "Set password";
    private static final String LOCK_STORE_BUTTON_TEXT = "Lock";
    private static final String ADD_STORE_BUTTON_TEXT = "Add new store";
    private static final String REMOVE_STORE_BUTTON_TEXT = "Delete store";

    // Action commands
    private static final String UNLOCK_STORE = "unlock-store";
    private static final String LOCK_STORE = "lock-store";
    private static final String CHANGE_STORE_PASSWORD = "change-store-password";
    private static final String ADD_STORE = "add-store";
    private static final String REMOVE_STORE = "remove-store";


    private final PasswordStoreList _passwordStoreList;
    private final PWSafe _pwsafe;
    private JList _storeList;
    private JButton _unlockStoreButton;
    private JButton _changeStorePasswordButton;
    private JButton _lockStoreButton;
    private JButton _addStoreButton;
    private JButton _removeStoreButton;

    /**
     * Construct a MainWindow
     */
    public MainWindow(final PWSafe pwsafe, final PasswordStoreList passwordStoreList) {
        super(TITLE);
        if (pwsafe == null) {
            throw new IllegalArgumentException("pwsafe must not be null");
        }
        if (passwordStoreList == null) {
            throw new IllegalArgumentException("passwordStoreList must not be null");
        }
        _pwsafe = pwsafe;
        _passwordStoreList = passwordStoreList;
        setup();
    }

    private void setup() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Top-level panel inside this JFrame
        JPanel mainContentPane = new JPanel(new BorderLayout());
        // mainContentPane.setBorder(someBorder);

        mainContentPane.add(createPasswordStoreListPanel(), BorderLayout.WEST);
        // mainContentPane.add(anotherComponent, BorderLayout.PAGE_END);

        setContentPane(mainContentPane);

        // getRootPane().setDefaultButton(_unlockStoreButton)

        reloadStoreList(null);

        // Auto-size based on components
        pack();
    }

    private Component createPasswordStoreListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(createPasswordStoreListButtons(), BorderLayout.NORTH);
        panel.add(createPasswordStoreList(), BorderLayout.CENTER);
        return panel;
    }

    private Component createPasswordStoreList() {
        _storeList = new JList();
        _storeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //_storeList.setFixedCellWidth(200);
        JScrollPane pane = new JScrollPane(_storeList);
        return pane;
    }

    private Component createPasswordStoreListButtons() {
        _unlockStoreButton = new JButton(UNLOCK_STORE_BUTTON_TEXT);
        _unlockStoreButton.setMnemonic(KeyEvent.VK_U);
        _unlockStoreButton.setActionCommand(UNLOCK_STORE);

        _changeStorePasswordButton = new JButton(CHANGE_STORE_PASSWORD_BUTTON_TEXT);
        // _changeStorePasswordButton.setMnemonic(KeyEvent.VK_C);
        _changeStorePasswordButton.setActionCommand(CHANGE_STORE_PASSWORD);

        _lockStoreButton = new JButton(LOCK_STORE_BUTTON_TEXT);
        _lockStoreButton.setMnemonic(KeyEvent.VK_L);
        _lockStoreButton.setActionCommand(LOCK_STORE);

        _addStoreButton = new JButton(ADD_STORE_BUTTON_TEXT);
        // _addStoreButton.setMnemonic(KeyEvent.VK_A);
        _addStoreButton.setActionCommand(ADD_STORE);

        _removeStoreButton = new JButton(REMOVE_STORE_BUTTON_TEXT);
        // _removeStoreButton.setMnemonic(KeyEvent.VK_R);
        _removeStoreButton.setActionCommand(REMOVE_STORE);

        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        for (JButton b : new JButton[] {
                _unlockStoreButton, _changeStorePasswordButton, _lockStoreButton, _addStoreButton, _removeStoreButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            b.addActionListener(this);
            box.add(b);
        }

        return box;
    }

    /**
     * @return true if unlocking succeeds
     */
    private boolean unlockSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        assert (store.isLocked());
        // Prompt for store password
        PasswordEntryDialog dialog = new PasswordEntryDialog(this, "Enter store unlock password", false);
        char[] password = dialog.showDialog();
        if (password == null || password.length == 0) { // cancelled / empty
            return false;
        }
        // Try to decrypt
        EncryptionKey key = new EncryptionKey(password);
        try {
            store.unlock(key);
            // Store is now responsible for the key - keeps it for locking again later
        } catch (DecryptionException e) {
            key.destroySecrets();
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Decryption failed - wrong password or corrupted file:\n" + e.toString());
            return false;
        }
        // Successfully unlocked
        assert (!store.isLocked());
        reloadStoreList(store);
        return true;
    }

    /**
     * @return true if unlocking succeeds
     */
    private boolean lockSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        assert (!store.isLocked());
        try {
            store.lock();
            // Store is now responsible for the key - keeps it for locking again later
        } catch (EncryptionException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Encryption failed:\n" + e.toString());
            return false;
        }
        // Successfully locked
        assert (store.isLocked());
        reloadStoreList(store);
        return true;
    }

    /**
     * @return true if actually changed, false if user cancelled
     */
    private boolean changeStorePassword() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        assert (!store.isLocked());
        // Prompt for store password
        PasswordEntryDialog dialog = new PasswordEntryDialog(this, "Enter new store password", true);
        char[] password = dialog.showDialog();
        if (password == null || password.length == 0) { // cancelled / empty
            return false;
        }
        // Update the store object - it will store the password for use when locking
        store.setKey(new EncryptionKey(password));
        // Reload list because it changes the status for new stores
        reloadStoreList(store);
        return true;
    }

    /**
     * @param selected store to select after reloading, or null to select first item.
     *         To maintain current selection, use {@code reloadStoreList((PasswordStore) _storeList.getSelectedValue())}.
     */
    protected void reloadStoreList(PasswordStore selected) {
        DefaultListModel listModel = new DefaultListModel();
        for (PasswordStore store : _passwordStoreList.getStores()) {
            listModel.addElement(store);
        }
        _storeList.setModel(listModel);

        if (selected != null) {
            _storeList.setSelectedValue(selected, true);
        } else {
            _storeList.setSelectedIndex(0);
        }
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (UNLOCK_STORE.equals(command)) {
            if (unlockSelectedStore()) {
                _unlockStoreButton.setEnabled(false);
                _lockStoreButton.setEnabled(true);
                _changeStorePasswordButton.setEnabled(true);
            }
        } else if (LOCK_STORE.equals(command)) {
            // TODO: disable lock button for new stores until password has been set
            if (lockSelectedStore()) {
                _unlockStoreButton.setEnabled(true);
                _lockStoreButton.setEnabled(false);
                _changeStorePasswordButton.setEnabled(false);
            }
        } else if (CHANGE_STORE_PASSWORD.equals(command)) {
            changeStorePassword();
        } /* else if () {
        ADD_STORE
        REMOVE_STORE
            b2.setEnabled(true);
            b1.setEnabled(true);
            b3.setEnabled(false);
        }
        */
    }

    /**
     * Called when the window is closed
     */
    @Override
    public void dispose() {
        // Could auto-save here, for now we just explicitly zero keys/passwords in the stores
        try {
            _passwordStoreList.destroySecrets();
        } finally {
            super.dispose();
        }
    }
}
