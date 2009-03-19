package pwsafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import pwsafe.DecryptionException;
import pwsafe.EncryptionException;
import pwsafe.PWSafe;
import pwsafe.store.PasswordStore;
import pwsafe.store.PasswordStoreEntry;
import pwsafe.store.PasswordStoreEntryList;
import pwsafe.store.PasswordStoreList;
import pwsafe.util.EncryptionKey;

/**
 * The main top-level application window
 *
 * @author Nick Clarke
 */
public class MainWindow extends JFrame implements ActionListener {

// User-visible text
    private static final String TITLE = "Password Safe";
    private static final String DEFAULT_NEW_ENTRY_NAME = "New entry";
    private static final String DEFAULT_NEW_STORE_NAME = "New store";
    // Store list
    private static final String UNLOCK_STORE_BUTTON_TEXT = "Unlock";
    private static final String CHANGE_STORE_PASSWORD_BUTTON_TEXT = "Change password";
    private static final String SET_STORE_PASSWORD_BUTTON_TEXT = "Set password";
    private static final String RENAME_STORE_BUTTON_TEXT = "Rename";
    private static final String LOCK_STORE_BUTTON_TEXT = "Lock";
    private static final String ADD_STORE_BUTTON_TEXT = "Add store";
    private static final String REMOVE_STORE_BUTTON_TEXT = "Delete store";
    // Entry list
    private static final String VIEW_ENTRY_BUTTON_TEXT = "Open entry";
    private static final String ADD_ENTRY_BUTTON_TEXT = "Add entry";
    private static final String REMOVE_ENTRY_BUTTON_TEXT = "Delete entry";
    private static final String SAVE_ENTRY_BUTTON_TEXT = "Save entry";
    private static final String DISCARD_ENTRY_BUTTON_TEXT = "Discard edited entry";
    // Entry editing
    private static final String SHOW_ENTRY_PASSWORD_BUTTON_TEXT = "Show";
    private static final String HIDE_ENTRY_PASSWORD_BUTTON_TEXT = "Hide";
    private static final String COPY_ENTRY_PASSWORD_BUTTON_TEXT = "Copy";

    private static final int PASSWORD_FIELD_COLUMNS = 10;

// Action commands
    private static enum ButtonAction {
        // Store list
        LOCK_OR_UNLOCK_STORE,
        CHANGE_STORE_PASSWORD,
        RENAME_STORE,
        ADD_STORE,
        REMOVE_STORE,
        // Entry list
        VIEW_ENTRY,
        ADD_ENTRY,
        REMOVE_ENTRY,
        SAVE_ENTRY,
        DISCARD_ENTRY,
        SHOW_OR_HIDE_ENTRY_PASSWORD,
        COPY_ENTRY_PASSWORD
    }

// Underlying data store
    private final PasswordStoreList _passwordStoreList;
    private final PWSafe _pwsafe;

// Store list
    private JList _storeList;
    private JButton _lockOrUnlockStoreButton;
    private JButton _changeStorePasswordButton;
    private JButton _renameStoreButton;
    private JButton _addStoreButton;
    private JButton _removeStoreButton;

// Entry list
    private JList _entryList;
    private JButton _viewEntryButton;
    private JButton _addEntryButton;
    private JButton _removeEntryButton;

// Entry editing
    private JTextField _entryNameField;
    private JTextField _entryUserIDField;
    private JPasswordField _entryPasswordField;
    private JTextArea _entryAdditionalInfoField;
    private JButton _showOrHideEntryPasswordButton;
    private JButton _copyEntryPasswordButton;
    private JButton _saveEntryButton;
    private JButton _discardEntryButton;

    private boolean _entryPasswordPlaintextVisible = false;
    private boolean _isNewEntry = false;

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
        // Create an empty initial store at first startup
        if (_passwordStoreList.isEmpty()) {
            _passwordStoreList.addStore(DEFAULT_NEW_STORE_NAME);
        }
        // Create and populate dialog controls
        setup();
    }

    private void setup() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Top-level panel inside this JFrame
        JPanel mainContentPane = new JPanel(new BorderLayout());
        mainContentPane.setBorder(BorderFactory.createLineBorder(Color.black));

        mainContentPane.add(createPasswordStoreListAndEntryListPanel(), BorderLayout.WEST);
        mainContentPane.add(createPasswordStoreEntryPanel(), BorderLayout.CENTER);
        // TODO: mainContentPane.add(createSaveLoadCancelButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainContentPane);

        // TODO: getRootPane().setDefaultButton(someButton)

        reloadPasswordStoreList(null);
        reloadPasswordStoreEntryList(null);

        // Auto-size based on components
        pack();
    }

    private Component createPasswordStoreListAndEntryListPanel() {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));
        box.add(createPasswordStoreListPanel());
        box.add(createPasswordStoreEntryListPanel());
        return box;
    }

    private Component createPasswordStoreEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(createPasswordStoreEntryEditFields(), BorderLayout.CENTER);
        panel.add(createPasswordStoreEntrySaveButtons(), BorderLayout.SOUTH);
        setPasswordStoreEntryEditFieldsEnabled(false);
        return panel;
    }

    private Component createPasswordStoreEntryEditFields() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel panel = new JPanel(gridbag);
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;

        JLabel label = new JLabel("Name:");
        gridbag.setConstraints(label, c);
        panel.add(label);

        c.gridy++;
        label = new JLabel("User ID:");
        gridbag.setConstraints(label, c);
        panel.add(label);

        c.gridy++;
        c.gridheight = 2;
        label = new JLabel("Password:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridheight = 1;

        c.gridy += 2;
        c.anchor = GridBagConstraints.NORTH;
        label = new JLabel("Additional info:");
        gridbag.setConstraints(label, c);
        panel.add(label);

        c.gridy = 0;
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.CENTER;

        _entryNameField = new JTextField();
        gridbag.setConstraints(_entryNameField, c);
        panel.add(_entryNameField);

        c.gridy++;
        _entryUserIDField = new JTextField();
        gridbag.setConstraints(_entryUserIDField, c);
        panel.add(_entryUserIDField);

        c.gridy++;
        /* TODO: make this always disabled, have 'Set password', 'Copy password' buttons next to it,
                 'Set password' will show PasswordEntryDialog for multiple-entry confirm,
                 also 'Generate random password'.
        */
        _entryPasswordField = new JPasswordField(PASSWORD_FIELD_COLUMNS);
        gridbag.setConstraints(_entryPasswordField, c);
        panel.add(_entryPasswordField);

        c.gridy++;
        Component passwordFieldButtons = createPasswordStoreEntryPasswordFieldButtons();
        gridbag.setConstraints(passwordFieldButtons, c);
        panel.add(passwordFieldButtons);
        setPasswordStoreEntryPasswordPlaintextVisible(false);

        c.gridy++;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        _entryAdditionalInfoField = new JTextArea();
        _entryAdditionalInfoField.setLineWrap(false);
        // _entryAdditionalInfoField.setWrapStyleWord(true); // true - break on whitespace only
        JScrollPane scrollPane = new JScrollPane(_entryAdditionalInfoField);
        gridbag.setConstraints(scrollPane, c);
        panel.add(scrollPane);

        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        return panel;
    }

    private Component createPasswordStoreEntryPasswordFieldButtons() {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        _showOrHideEntryPasswordButton = makeButton(box, SHOW_ENTRY_PASSWORD_BUTTON_TEXT, KeyEvent.VK_S,
                ButtonAction.SHOW_OR_HIDE_ENTRY_PASSWORD);
        _copyEntryPasswordButton = makeButton(box, COPY_ENTRY_PASSWORD_BUTTON_TEXT, KeyEvent.VK_C,
                ButtonAction.COPY_ENTRY_PASSWORD);

        return box;
    }

    private void clearPasswordStoreEntryEditFields() {
        _entryNameField.setText(null);
        _entryUserIDField.setText(null);
        _entryPasswordField.setText(null);
        _entryAdditionalInfoField.setText(null);
        setPasswordStoreEntryPasswordPlaintextVisible(false);
    }

    private void setPasswordStoreEntryEditFieldsEnabled(boolean enabled) {
        _entryNameField.setEditable(enabled);
        _entryUserIDField.setEditable(enabled);
        _entryPasswordField.setEditable(enabled);
        _entryAdditionalInfoField.setEditable(enabled);
        _saveEntryButton.setEnabled(enabled);
        _discardEntryButton.setEnabled(enabled);
        _showOrHideEntryPasswordButton.setEnabled(enabled);
        _copyEntryPasswordButton.setEnabled(enabled);
    }

    private void setPasswordStoreEntryPasswordPlaintextVisible(boolean visible) {
        _entryPasswordPlaintextVisible = visible;
        _showOrHideEntryPasswordButton.setText(visible ? HIDE_ENTRY_PASSWORD_BUTTON_TEXT
                                                       : SHOW_ENTRY_PASSWORD_BUTTON_TEXT);
        _showOrHideEntryPasswordButton.setMnemonic(visible ? KeyEvent.VK_H : KeyEvent.VK_S);
        _entryPasswordField.setEchoChar(visible ? ((char) 0) : '*');
    }

    private Component createPasswordStoreEntrySaveButtons() {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        _saveEntryButton    = makeButton(box, SAVE_ENTRY_BUTTON_TEXT,    KeyEvent.VK_S,      ButtonAction.SAVE_ENTRY);
        _discardEntryButton = makeButton(box, DISCARD_ENTRY_BUTTON_TEXT, KeyEvent.VK_ESCAPE, ButtonAction.DISCARD_ENTRY);

        return box;
    }

    /**
     * List of entries in the currently-selected store plus control buttons
     */
    private Component createPasswordStoreEntryListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(createPasswordStoreEntryListButtons(), BorderLayout.NORTH);
        panel.add(createPasswordStoreEntryList(), BorderLayout.CENTER);
        return panel;
    }

    /**
     * List of entries in the currently-selected store
     */
    private Component createPasswordStoreEntryList() {
        _entryList = new JList();
        _entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //_entryList.setFixedCellWidth(200);
        _entryList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Double-click
                        int index = _entryList.locationToIndex(e.getPoint());
                        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getModel().getElementAt(index);
                        if (entry != null) {
                            viewSelectedEntry();
                        }
                    }
                }
            });
        _entryList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    // TODO: may want to veto the change if an entry is currently shown and has been edited
                    if (e.getValueIsAdjusting()) {
                        return; // Will get another event
                    }
                    // int index = e.getFirstIndex();
                    enableEntryListAndButtons();
                    // TODO: need this, but must prevent it interacting with double-click 'viewSelectedEntry'
                    // closeDisplayedEntry(false);
                }
            });
        JScrollPane pane = new JScrollPane(_entryList);
        return pane;
    }

    /**
     * Control buttons for list of entries in currently-selected store
     */
    private Component createPasswordStoreEntryListButtons() {
        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        _viewEntryButton   = makeButton(box, VIEW_ENTRY_BUTTON_TEXT,   KeyEvent.VK_O, ButtonAction.VIEW_ENTRY);
        _addEntryButton    = makeButton(box, ADD_ENTRY_BUTTON_TEXT,    KeyEvent.VK_A, ButtonAction.ADD_ENTRY);
        _removeEntryButton = makeButton(box, REMOVE_ENTRY_BUTTON_TEXT, KeyEvent.VK_D, ButtonAction.REMOVE_ENTRY);

        return box;
    }

    /**
     * List of stores to lock/unlock plus control buttons
     */
    private Component createPasswordStoreListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(createPasswordStoreListButtons(), BorderLayout.NORTH);
        panel.add(createPasswordStoreList(), BorderLayout.CENTER);
        return panel;
    }

    /**
     * List of stores to lock/unlock
     */
    private Component createPasswordStoreList() {
        _storeList = new JList();
        _storeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //_storeList.setFixedCellWidth(200);
        _storeList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Double-click
                        int index = _storeList.locationToIndex(e.getPoint());
                        PasswordStore store = (PasswordStore) _storeList.getModel().getElementAt(index);
                        if (store != null && store.isLocked()) {
                            unlockStore(store);
                        }
                    }
                }
            });
        _storeList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    // TODO: may want to veto the change if an entry is currently shown and has been edited
                    if (e.getValueIsAdjusting()) {
                        return; // Will get another event
                    }
                    // int index = e.getFirstIndex();
                    enableStoreListAndButtons();
                    reloadPasswordStoreEntryList(null);
                }
            });
        JScrollPane pane = new JScrollPane(_storeList);
        return pane;
    }

    /**
     * Control buttons for list of stores to lock/unlock
     */
    private Component createPasswordStoreListButtons() {
        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        _lockOrUnlockStoreButton = makeButton(box, UNLOCK_STORE_BUTTON_TEXT, KeyEvent.VK_U,    ButtonAction.LOCK_OR_UNLOCK_STORE);
        _changeStorePasswordButton =
                             makeButton(box, CHANGE_STORE_PASSWORD_BUTTON_TEXT, KeyEvent.VK_P, ButtonAction.CHANGE_STORE_PASSWORD);
        _renameStoreButton = makeButton(box, RENAME_STORE_BUTTON_TEXT, KeyEvent.VK_R,          ButtonAction.RENAME_STORE);
        _addStoreButton    = makeButton(box, ADD_STORE_BUTTON_TEXT,    -1,                     ButtonAction.ADD_STORE);
        _removeStoreButton = makeButton(box, REMOVE_STORE_BUTTON_TEXT, -1,                     ButtonAction.REMOVE_STORE);

        return box;
    }

    private void viewSelectedEntry() {
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        assert (entry != null);
        _entryNameField.setText(entry.getDisplayName());
        _entryUserIDField.setText(entry.getUserID());
        char[] password = entry.getPassword();
        _entryPasswordField.setText(password == null ? null : new String(password));
        // No choice but to create a String object with the possibly-secret data here
        char[] additional = entry.getAdditionalInfo();
        _entryAdditionalInfoField.setText(additional == null ? null : new String(additional));
        setPasswordStoreEntryEditFieldsEnabled(true);
        setPasswordStoreEntryListButtonsEnabled(false);
        setPasswordStoreListButtonsEnabled(false);
    }

    private void lockOrUnlockSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        if (store.isLocked()) {
            unlockStore(store);
        } else {
            lockStore(store);
        }
    }

    private void unlockStore(PasswordStore store) {
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
        reloadPasswordStoreList(store);
        reloadPasswordStoreEntryList(null);
    }

    private void lockStore(PasswordStore store) {
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
        reloadPasswordStoreList(store);
        reloadPasswordStoreEntryList(null);
    }

    private void changeSelectedStorePassword() {
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
        reloadPasswordStoreList(store);
    }

    private void renameSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        String newStoreName = JOptionPane.showInputDialog(this,
                "Enter new name for store:", "Rename store", JOptionPane.QUESTION_MESSAGE);
        if (newStoreName != null && !"".equals(newStoreName)) {
            store.setStoreName(newStoreName);
            reloadPasswordStoreList(store);
        }
    }

    private void removeSelectedStore() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null);
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                "Permanently delete store named '" + store.getStoreName() + "'?",
                "Confirm store delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)) {
            _passwordStoreList.removeStore(store);
            // TODO: select the next lowest entry rather than going back to the first
            reloadPasswordStoreList(null);
            reloadPasswordStoreEntryList(null);
        }
    }

    private void confirmAndRemoveSelectedEntry() {
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        assert (entry != null);
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                "Permanently delete entry named '" + entry.getDisplayName() + "'?",
                "Confirm entry delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)) {
            removeSelectedEntry(entry);
            // TODO: select the next lowest entry rather than going back to the first
            reloadPasswordStoreEntryList(null);
        }
    }

    private void copySelectedEntryPassword() {
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        assert (entry != null);
        Clipboard cb = getToolkit().getSystemClipboard();
        try {
            // Copy from the field rather than the entry, in case it has been edited or is new
            char[] password = _entryPasswordField.getPassword();
            StringSelection ss = new StringSelection(password == null ? "" : new String(password));
            // No choice but to allocate as a String, but that's now a fairly minor concern since we're exposing
            // to other apps in the clipboard
            cb.setContents(ss, ss);
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to access clipboard:\n" + e.toString());
            return;
        }
    }

    private void removeSelectedEntry(PasswordStoreEntry entry) {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null && !store.isLocked());
        store.getEntryList().removeEntry(entry);
    }

    private void addNewStore() {
        PasswordStore newStore = _passwordStoreList.addStore(DEFAULT_NEW_STORE_NAME);
        reloadPasswordStoreList(newStore);
        reloadPasswordStoreEntryList(null);
    }

    private void addNewEntry() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null && !store.isLocked());
        PasswordStoreEntry newEntry = store.getEntryList().addEntry(DEFAULT_NEW_ENTRY_NAME);
        reloadPasswordStoreEntryList(newEntry);
        viewSelectedEntry();
        _isNewEntry = true;
    }

    /**
     * @param storeToSelect store to select after reloading, or null to select first item.
     *         To maintain current selection, use {@code reloadPasswordStoreList((PasswordStore) _storeList.getSelectedValue())}.
     */
    private void reloadPasswordStoreList(PasswordStore storeToSelect) {
        DefaultListModel listModel = new DefaultListModel();
        for (PasswordStore store : _passwordStoreList.getStores()) {
            listModel.addElement(store);
        }
        _storeList.setModel(listModel);

        if (storeToSelect != null) {
            assert (!listModel.isEmpty());
            _storeList.setSelectedValue(storeToSelect, true);
        } else if (!listModel.isEmpty()) {
            _storeList.setSelectedIndex(0);
        }
        enableStoreListAndButtons();
    }

    /**
     * @param entryToSelect entry to select after reloading, or null to select first item.
     *         To maintain current selection, use {@code reloadPasswordStoreEntryList((PasswordStoreEntry) _entryList.getSelectedValue())}.
     */
    private void reloadPasswordStoreEntryList(PasswordStoreEntry entryToSelect) {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        // closeDisplayedEntry(false);
        DefaultListModel listModel = new DefaultListModel();
        if (store != null && !store.isLocked()) {
            for (PasswordStoreEntry entry : store.getEntryList().getEntries()) {
                listModel.addElement(entry);
            }
        }
        _entryList.setModel(listModel);

        if (entryToSelect != null) {
            assert (store != null && !store.isLocked() && !listModel.isEmpty());
            _entryList.setSelectedValue(entryToSelect, true);
        } else if (!listModel.isEmpty()) {
            _entryList.setSelectedIndex(0);
        }
        enableEntryListAndButtons();
    }

    private void enableStoreListAndButtons() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        if (store == null) {
            _lockOrUnlockStoreButton.setEnabled(false);
            _changeStorePasswordButton.setEnabled(false);
            _renameStoreButton.setEnabled(false);
            _removeStoreButton.setEnabled(false);
            _addEntryButton.setEnabled(false);
        } else {
            boolean isLocked = store.isLocked();
            boolean hasKey = store.hasKey();
            _lockOrUnlockStoreButton.setEnabled(isLocked || hasKey);
            _lockOrUnlockStoreButton.setText(isLocked ? UNLOCK_STORE_BUTTON_TEXT : LOCK_STORE_BUTTON_TEXT);
            _lockOrUnlockStoreButton.setMnemonic(isLocked ? KeyEvent.VK_U : KeyEvent.VK_L);
            _changeStorePasswordButton.setEnabled(!isLocked);
            _changeStorePasswordButton.setText((isLocked || hasKey) ? CHANGE_STORE_PASSWORD_BUTTON_TEXT
                                                                    : SET_STORE_PASSWORD_BUTTON_TEXT);
            _renameStoreButton.setEnabled(true);
            _removeStoreButton.setEnabled(true);
            _addEntryButton.setEnabled(!isLocked);
        }
        _addStoreButton.setEnabled(true);
        _storeList.setEnabled(true);
    }

    private void enableEntryListAndButtons() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        boolean storeUnlocked = (store != null && !store.isLocked());
        if (entry == null) {
            _viewEntryButton.setEnabled(false);
            _removeEntryButton.setEnabled(false);
        } else {
            _viewEntryButton.setEnabled(true);
            _removeEntryButton.setEnabled(true);
        }
        _addEntryButton.setEnabled(storeUnlocked);
        _entryList.setEnabled(storeUnlocked);
    }

    private void setPasswordStoreEntryListButtonsEnabled(boolean enabled) {
        _entryList.setEnabled(enabled);
        _viewEntryButton.setEnabled(enabled);
        _addEntryButton.setEnabled(enabled);
        _removeEntryButton.setEnabled(enabled);
    }

    private void setPasswordStoreListButtonsEnabled(boolean enabled) {
        _storeList.setEnabled(enabled);
        _lockOrUnlockStoreButton.setEnabled(enabled);
        _changeStorePasswordButton.setEnabled(enabled);
        _renameStoreButton.setEnabled(enabled);
        _addStoreButton.setEnabled(enabled);
        _removeStoreButton.setEnabled(enabled);
    }

    /**
     * @return the entry that was selected
     */
    private PasswordStoreEntry closeDisplayedEntry(boolean saveEdits) {
        setPasswordStoreEntryEditFieldsEnabled(false);
        enableStoreListAndButtons();
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        if (entry == null) {
            assert (!saveEdits);
        } else if (saveEdits) {
            // Only the entry name is required, other fields can be set to empty / null
            String entryName = _entryNameField.getText();
            if (entryName != null) {
                entryName = entryName.trim();
            }
            if (entryName == null || "".equals(entryName)) {
                entryName = entry.getDisplayName();
            }
            entry.setAllFields(entryName,
                               _entryUserIDField.getText(),
                               _entryPasswordField.getPassword(),
                               _entryAdditionalInfoField.getText().toCharArray());
        } else if (_isNewEntry) {
            // Discarding newly-added entry, remove completely
            removeSelectedEntry(entry);
            entry = null;
        }
        clearPasswordStoreEntryEditFields();
        _isNewEntry = false;
        return entry;
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        ButtonAction action = ButtonAction.valueOf(e.getActionCommand());
        switch (action) {
        case LOCK_OR_UNLOCK_STORE:
            lockOrUnlockSelectedStore();
            break;
        case CHANGE_STORE_PASSWORD:
            changeSelectedStorePassword();
            break;
        case RENAME_STORE:
            renameSelectedStore();
            break;
        case REMOVE_STORE:
            removeSelectedStore();
            break;
        case ADD_STORE:
            addNewStore();
            break;
        case VIEW_ENTRY:
            viewSelectedEntry();
            break;
        case ADD_ENTRY:
            addNewEntry();
            break;
        case REMOVE_ENTRY:
            confirmAndRemoveSelectedEntry();
            break;
        case SAVE_ENTRY:
            reloadPasswordStoreEntryList(closeDisplayedEntry(true));
            break;
        case DISCARD_ENTRY:
            reloadPasswordStoreEntryList(closeDisplayedEntry(false));
            break;
        case SHOW_OR_HIDE_ENTRY_PASSWORD:
            setPasswordStoreEntryPasswordPlaintextVisible(!_entryPasswordPlaintextVisible);
            break;
        case COPY_ENTRY_PASSWORD:
            copySelectedEntryPassword();
            break;
        default:
            break;
        }
    }

    private JButton makeButton(Container container, String text, int mnemonic, ButtonAction action) {
        JButton button = new JButton(text);
        if (mnemonic != -1) {
            button.setMnemonic(mnemonic);
        }
        button.setActionCommand(action.name());
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        button.addActionListener(this);
        container.add(button);
        return button;
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

    /* TODO: add Save / Load buttons to write to file; these should first check if there are any newly-created stores
             which have not had their password set: (!store.isLocked() && !store.hasKey()), since this will make
             serialization fail. */
}
