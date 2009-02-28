package pwsafe.gui;

import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import pwsafe.PWSafe;
import pwsafe.store.PasswordStore;
import pwsafe.store.PasswordStoreList;

/**
 * Initial dialog
 *
 * @author Nick Clarke
 */
public class MainWindow extends JFrame {

    private static final String TITLE = "Password Safe";


    private final PasswordStoreList _storeList;
    private final PWSafe _pwsafe;
    private JList _list;

    /**
     * Construct a MainWindow
     */
    public MainWindow(final PWSafe pwsafe, final PasswordStoreList storeList) {
        super(TITLE);
        if (pwsafe == null) {
            throw new IllegalArgumentException("pwsafe must not be null");
        }
        if (storeList == null) {
            throw new IllegalArgumentException("storeList must not be null");
        }
        _pwsafe = pwsafe;
        _storeList = storeList;
        setup();
    }

    private void setup() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        _list = new JList();
        _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //_list.setFixedCellWidth(200);

        JScrollPane scrollPane = new JScrollPane(_list);
        getContentPane().add(scrollPane);

        reloadStoreList();

        // Auto-size based on components
        pack();
    }

    /**
     * Called when the window is closed
     */
    @Override
    public void dispose() {
        // Could auto-save here, for now we just explicitly zero keys/passwords in the stores
        try {
            _storeList.destroySecrets();
        } finally {
            super.dispose();
        }
    }

    private void reloadStoreList() {
        DefaultListModel listModel = new DefaultListModel();
        for (PasswordStore store : _storeList.getStores()) {
            listModel.addElement(store);
        }
        _list.setModel(listModel);
        _list.setSelectedIndex(0);
    }
}
