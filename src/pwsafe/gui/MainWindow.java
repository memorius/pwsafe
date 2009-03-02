package pwsafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
        MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Double-click
                        int index = _storeList.locationToIndex(e.getPoint());
                        PasswordStore store = (PasswordStore) _storeList.getModel().getElementAt(index);
                        if (store != null && store.isLocked()) {
                            unlockSelectedStore();
                        }
                    }
                }
            };
        _storeList.addMouseListener(mouseListener);
        ListSelectionListener selectionListener = new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        return; // Will get another event
                    }
                    // int index = e.getFirstIndex();
                    updateStoreListButtonState();
                }
            };
        _storeList.addListSelectionListener(selectionListener);
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

    private void unlockSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        assert (store.isLocked());
        // Prompt for store password
        PasswordEntryDialog dialog = new PasswordEntryDialog(this, "Enter store unlock password", false);
        char[] password = dialog.showDialog();
        if (password == null || password.length == 0) { // cancelled / empty
            return;
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
            return;
        }
        // Successfully unlocked
        assert (!store.isLocked());
        reloadStoreList(store);
    }

    private void lockSelectedStore() {
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
            return;
        }
        // Successfully locked
        assert (store.isLocked());
        reloadStoreList(store);
    }

    private void changeStorePassword() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        assert (!store.isLocked());
        // Prompt for store password
        PasswordEntryDialog dialog = new PasswordEntryDialog(this, "Enter new store password", true);
        char[] password = dialog.showDialog();
        if (password == null || password.length == 0) { // cancelled / empty
            return;
        }
        // Update the store object - it will store the password for use when locking
        store.setKey(new EncryptionKey(password));
        // Reload list because it changes the status for new stores
        reloadStoreList(store);
    }

    private void removeSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                "Permanently delete password store named '" + store.getStoreName() + "'?",
                "Confirm password store delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)) {
            _passwordStoreList.removeStore(store);
            reloadStoreList(null);
        }
    }

    private void addNewStore() {
        String storeName = JOptionPane.showInputDialog(this,
                "Enter name for new password store:", "Create new password store", JOptionPane.QUESTION_MESSAGE);
        if (storeName != null && !"".equals(storeName)) {
            PasswordStore newStore = _passwordStoreList.addStore(storeName);
            reloadStoreList(newStore);
        }
    }

    /**
     * @param storeToSelect store to select after reloading, or null to select first item.
     *         To maintain current selection, use {@code reloadStoreList((PasswordStore) _storeList.getSelectedValue())}.
     */
    private void reloadStoreList(PasswordStore storeToSelect) {
        DefaultListModel listModel = new DefaultListModel();
        List<PasswordStore> stores = _passwordStoreList.getStores();
        for (PasswordStore store : stores) {
            listModel.addElement(store);
        }
        _storeList.setModel(listModel);

        if (storeToSelect != null) {
            _storeList.setSelectedValue(storeToSelect, true);
        } else {
            _storeList.setSelectedIndex(0);
        }
        updateStoreListButtonState();
    }

    private void updateStoreListButtonState() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        if (store == null) {
            _unlockStoreButton.setEnabled(false);
            _lockStoreButton.setEnabled(false);
            _changeStorePasswordButton.setEnabled(false);
            _removeStoreButton.setEnabled(false);
        } else {
            boolean isLocked = store.isLocked();
            boolean hasKey = store.hasKey();
            _unlockStoreButton.setEnabled(isLocked);
            _lockStoreButton.setEnabled(hasKey);
            _changeStorePasswordButton.setEnabled(!isLocked);
            _changeStorePasswordButton.setText((isLocked || hasKey) ? CHANGE_STORE_PASSWORD_BUTTON_TEXT
                                                                    : SET_STORE_PASSWORD_BUTTON_TEXT);
            _removeStoreButton.setEnabled(true);
        }
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (UNLOCK_STORE.equals(command)) {
            unlockSelectedStore();
        } else if (LOCK_STORE.equals(command)) {
            lockSelectedStore();
        } else if (CHANGE_STORE_PASSWORD.equals(command)) {
            changeStorePassword();
        } else if (REMOVE_STORE.equals(command)) {
            removeSelectedStore();
        } else if (ADD_STORE.equals(command)) {
            addNewStore();
        }
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
