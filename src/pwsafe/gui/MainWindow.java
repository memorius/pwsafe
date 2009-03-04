package pwsafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import pwsafe.DecryptionException;
import pwsafe.EncryptionException;
import pwsafe.EncryptionKey;
import pwsafe.PWSafe;
import pwsafe.store.PasswordStore;
import pwsafe.store.PasswordStoreEntry;
import pwsafe.store.PasswordStoreEntryList;
import pwsafe.store.PasswordStoreList;

/**
 * The main top-level application window
 *
 * @author Nick Clarke
 */
public class MainWindow extends JFrame implements ActionListener {

// User-visible text
    private static final String TITLE = "Password Safe";
    // Store list
    private static final String UNLOCK_STORE_BUTTON_TEXT = "Unlock";
    private static final String CHANGE_STORE_PASSWORD_BUTTON_TEXT = "Change password";
    private static final String SET_STORE_PASSWORD_BUTTON_TEXT = "Set password";
    private static final String LOCK_STORE_BUTTON_TEXT = "Lock";
    private static final String ADD_STORE_BUTTON_TEXT = "Add new store";
    private static final String REMOVE_STORE_BUTTON_TEXT = "Delete store";
    // Entry list
    private static final String VIEW_ENTRY_BUTTON_TEXT = "View entry";
    private static final String ADD_ENTRY_BUTTON_TEXT = "Add entry";
    private static final String REMOVE_ENTRY_BUTTON_TEXT = "Remove entry";
    private static final String SAVE_ENTRY_BUTTON_TEXT = "Save entry";
    private static final String DISCARD_ENTRY_BUTTON_TEXT = "Discard edited entry";

// Action commands
    private static enum ButtonAction {
        // Store list
        UNLOCK_STORE,
        LOCK_STORE,
        CHANGE_STORE_PASSWORD,
        ADD_STORE,
        REMOVE_STORE,
        // Entry list
        VIEW_ENTRY,
        ADD_ENTRY,
        REMOVE_ENTRY,
        SAVE_ENTRY,
        DISCARD_ENTRY
    }

// Underlying data store
    private final PasswordStoreList _passwordStoreList;
    private final PWSafe _pwsafe;

// Store list
    private JList _storeList;
    private JButton _unlockStoreButton;
    private JButton _changeStorePasswordButton;
    private JButton _lockStoreButton;
    private JButton _addStoreButton;
    private JButton _removeStoreButton;

// Entry list
    private JList _entryList;
    private JButton _viewEntryButton;
    private JButton _addEntryButton;
    private JButton _removeEntryButton;

// Entry editing
    private JButton _saveEntryButton;
    private JButton _discardEntryButton;


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
        mainContentPane.setBorder(BorderFactory.createLineBorder(Color.black));

        mainContentPane.add(createPasswordStoreListPanel(), BorderLayout.WEST);
        mainContentPane.add(createPasswordStoreContentsPanel(), BorderLayout.CENTER);
        // TODO: mainContentPane.add(createSaveLoadCancelButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainContentPane);

        // TODO: getRootPane().setDefaultButton(someButton)

        reloadPasswordStoreList(null);
        reloadPasswordStoreEntryList(null);

        // Auto-size based on components
        pack();
    }

    /**
     * Contents of the currently-selected store
     */
    private Component createPasswordStoreContentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(createPasswordStoreEntryListPanel(), BorderLayout.WEST);
        panel.add(createPasswordStoreEntryPanel(), BorderLayout.CENTER);
        return panel;
    }

    private Component createPasswordStoreEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(createPasswordStoreEntryEditFields(), BorderLayout.CENTER);
        panel.add(createPasswordStoreEntrySaveButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private Component createPasswordStoreEntryEditFields() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel panel = new JPanel(gridbag);
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.weighty = 1.0;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;

        // TODO
        JLabel label = new JLabel("Some text fields here");
        gridbag.setConstraints(label, c);
        panel.add(label);

        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        return panel;
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
                    updateEntryListButtonState();
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

        _viewEntryButton   = makeButton(box, VIEW_ENTRY_BUTTON_TEXT,   KeyEvent.VK_V, ButtonAction.VIEW_ENTRY);
        _addEntryButton    = makeButton(box, ADD_ENTRY_BUTTON_TEXT,    KeyEvent.VK_A, ButtonAction.ADD_ENTRY);
        _removeEntryButton = makeButton(box, REMOVE_ENTRY_BUTTON_TEXT, KeyEvent.VK_R, ButtonAction.REMOVE_ENTRY);

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
                            unlockSelectedStore();
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
                    updateStoreListButtonState();
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

        _unlockStoreButton = makeButton(box, UNLOCK_STORE_BUTTON_TEXT, KeyEvent.VK_U,        ButtonAction.UNLOCK_STORE);
        _changeStorePasswordButton =
                             makeButton(box, CHANGE_STORE_PASSWORD_BUTTON_TEXT, -1 /*KeyEvent.VK_C*/, ButtonAction.CHANGE_STORE_PASSWORD);
        // TODO: button to rename store
        _lockStoreButton   = makeButton(box, LOCK_STORE_BUTTON_TEXT,   KeyEvent.VK_L,        ButtonAction.LOCK_STORE);
        _addStoreButton    = makeButton(box, ADD_STORE_BUTTON_TEXT,    -1 /*KeyEvent.VK_A*/, ButtonAction.ADD_STORE);
        _removeStoreButton = makeButton(box, REMOVE_STORE_BUTTON_TEXT, -1 /*KeyEvent.VK_R*/, ButtonAction.REMOVE_STORE);

        return box;
    }

    private void viewSelectedEntry() {
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        assert (entry != null);
        // TODO: update text fields with data in entry and enable them
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
        reloadPasswordStoreList(store);
        reloadPasswordStoreEntryList(null);
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
        reloadPasswordStoreList(store);
        reloadPasswordStoreEntryList(null);
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
        reloadPasswordStoreList(store);
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
            // TODO: select the next lowest entry rather than going back to the first
            reloadPasswordStoreList(null);
            reloadPasswordStoreEntryList(null);
        }
    }

    private void removeSelectedEntry() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null && !store.isLocked());
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        assert (entry != null);
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                "Permanently delete password account entry named '" + entry.getDisplayName() + "'?",
                "Confirm password account entry delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)) {
            store.getEntryList().removeEntry(entry);
            // TODO: select the next lowest entry rather than going back to the first
            reloadPasswordStoreEntryList(null);
        }
    }

    private void addNewStore() {
        String storeName = JOptionPane.showInputDialog(this,
                "Enter name for new password store:", "Create new password store", JOptionPane.QUESTION_MESSAGE);
        if (storeName != null && !"".equals(storeName)) {
            PasswordStore newStore = _passwordStoreList.addStore(storeName);
            reloadPasswordStoreList(newStore);
            reloadPasswordStoreEntryList(null);
        }
    }

    private void addNewEntry() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        assert (store != null && !store.isLocked());
        String entryName = JOptionPane.showInputDialog(this,
                "Enter name for new password account entry:", "Create new password account entry", JOptionPane.QUESTION_MESSAGE);
        if (entryName != null && !"".equals(entryName)) {
            PasswordStoreEntry newEntry = store.getEntryList().addEntry(entryName);
            reloadPasswordStoreEntryList(newEntry);
            viewSelectedEntry();
        }
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
        updateStoreListButtonState();
    }

    /**
     * @param entryToSelect entry to select after reloading, or null to select first item.
     *         To maintain current selection, use {@code reloadPasswordStoreEntryList((PasswordStoreEntry) _entryList.getSelectedValue())}.
     */
    private void reloadPasswordStoreEntryList(PasswordStoreEntry entryToSelect) {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        closeDisplayedEntry(false);
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
        updateEntryListButtonState();
    }

    private void updateStoreListButtonState() {
        PasswordStore store = (PasswordStore) _storeList.getSelectedValue();
        if (store == null) {
            _unlockStoreButton.setEnabled(false);
            _lockStoreButton.setEnabled(false);
            _changeStorePasswordButton.setEnabled(false);
            _removeStoreButton.setEnabled(false);
            _addEntryButton.setEnabled(false);
        } else {
            boolean isLocked = store.isLocked();
            boolean hasKey = store.hasKey();
            _unlockStoreButton.setEnabled(isLocked);
            _lockStoreButton.setEnabled(hasKey);
            _changeStorePasswordButton.setEnabled(!isLocked);
            _changeStorePasswordButton.setText((isLocked || hasKey) ? CHANGE_STORE_PASSWORD_BUTTON_TEXT
                                                                    : SET_STORE_PASSWORD_BUTTON_TEXT);
            _removeStoreButton.setEnabled(true);
            _addEntryButton.setEnabled(!isLocked);
        }
    }

    private void updateEntryListButtonState() {
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        if (entry == null) {
            _viewEntryButton.setEnabled(false);
            _removeEntryButton.setEnabled(false);
        } else {
            _viewEntryButton.setEnabled(true);
            _removeEntryButton.setEnabled(true);
        }
    }

    private void closeDisplayedEntry(boolean saveEdits) {
        PasswordStoreEntry entry = (PasswordStoreEntry) _entryList.getSelectedValue();
        if (entry == null) {
            return;
        }
        if (saveEdits) {
            // TODO: read text fields and write to entry
        }
        // TODO: set text fields to empty and disable them and Save / Cancel buttons
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        ButtonAction action = ButtonAction.valueOf(e.getActionCommand());
        switch (action) {
        case UNLOCK_STORE:
            unlockSelectedStore();
            break;
        case LOCK_STORE:
            lockSelectedStore();
            break;
        case CHANGE_STORE_PASSWORD:
            changeStorePassword();
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
            removeSelectedEntry();
            break;
        case SAVE_ENTRY:
            closeDisplayedEntry(true);
            break;
        case DISCARD_ENTRY:
            closeDisplayedEntry(false);
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
