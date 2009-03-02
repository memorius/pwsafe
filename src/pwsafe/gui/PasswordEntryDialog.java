package pwsafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;

/**
 * Modal dialog prompting for a password, with optional double-confirm
 *
 * @author Nick Clarke
 */
public class PasswordEntryDialog extends JDialog implements ActionListener {

    private static final String OK_BUTTON_TEXT = "OK";
    private static final String CANCEL_BUTTON_TEXT = "Cancel";

    private static final String BUTTON_OK = "ok";
    private static final String BUTTON_CANCEL = "cancel";

    private static final int PASSWORD_FIELD_COLUMNS = 30;

    private boolean _ok = false;
    private boolean _multipleEntry;
    private char[] _password;
    private JPasswordField _passwordField1;
    private JPasswordField _passwordField2;
    private JPasswordField _passwordField3;
    private JButton _okButton;
    private JButton _cancelButton;

    /**
     * Construct a PasswordEntryDialog
     */
    public PasswordEntryDialog(final Frame parent, final String title, final boolean multipleEntry) {
        super(parent, title, true);
        _multipleEntry = multipleEntry;
        setup();
    }

    private void setup() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE /*DO_NOTHING_ON_CLOSE*/);
        // This is invoked before disposing if closed
        /*
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    onCancel();
                }
            });
        */

        // Top-level panel inside this dialog
        JPanel mainContentPane = new JPanel(new BorderLayout());
        // mainContentPane.setBorder(someBorder);

        mainContentPane.add(createPasswordFieldsPanel(), BorderLayout.CENTER);

        mainContentPane.add(createButtonsPanel(), BorderLayout.SOUTH);

        setContentPane(mainContentPane);

        getRootPane().setDefaultButton(_okButton);

        // Auto-size based on components
        pack();
    }

    private Component createPasswordFieldsPanel() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel panel = new JPanel(gridbag);
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.weighty = 1.0;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel("Password:");
        gridbag.setConstraints(label, c);
        panel.add(label);

        if (_multipleEntry) {
            c.gridy++;
            label = new JLabel("Re-enter password:");
            gridbag.setConstraints(label, c);
            panel.add(label);

            c.gridy++;
            label = new JLabel("Re-enter again:");
            gridbag.setConstraints(label, c);
            panel.add(label);

            c.gridy = 0;
        }

        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 2, 0, 2);
        _passwordField1 = new JPasswordField(PASSWORD_FIELD_COLUMNS);
        gridbag.setConstraints(_passwordField1, c);
        panel.add(_passwordField1);

        if (_multipleEntry) {
            c.gridy++;
            _passwordField2 = new JPasswordField(PASSWORD_FIELD_COLUMNS);
            gridbag.setConstraints(_passwordField2, c);
            panel.add(_passwordField2);

            c.gridy++;
            _passwordField3 = new JPasswordField(PASSWORD_FIELD_COLUMNS);
            gridbag.setConstraints(_passwordField3, c);
            panel.add(_passwordField3);
        }

        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        return panel;
    }

    private Component createButtonsPanel() {
        _okButton = new JButton(OK_BUTTON_TEXT);
        _okButton.setMnemonic(KeyEvent.VK_O);
        _okButton.setActionCommand(BUTTON_OK);

        _cancelButton = new JButton(CANCEL_BUTTON_TEXT);
        _cancelButton.setMnemonic(KeyEvent.VK_O);
        _cancelButton.setActionCommand(BUTTON_CANCEL);

        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        for (JButton b : new JButton[] {_okButton, _cancelButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            b.addActionListener(this);
            box.add(b);
        }

        return box;
    }

    private boolean confirmMatch() {
        if (!_multipleEntry) {
            return true;
        }
        char[] p1 = _passwordField1.getPassword();
        char[] p2 = _passwordField2.getPassword();
        char[] p3 = _passwordField3.getPassword();
        try {
            return Arrays.equals(p1, p2) && Arrays.equals(p2, p3);
        } finally {
            Arrays.fill(p1, (char) 0);
            Arrays.fill(p2, (char) 0);
            Arrays.fill(p3, (char) 0);
        }
    }

    public char[] showDialog() {
        setVisible(true);
        // It's modal, so when setVisible returns, user interaction has finished
        char[] result = _ok ? _password : null;
        _password = null; // caller now takes responsibility for clearing the secret data when done
        return result;
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        if (BUTTON_OK.equals(e.getActionCommand())) {
            if (confirmMatch()) {
                _password = _passwordField1.getPassword();
                _ok = true;
                setVisible(false);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "The entered passwords do not match");
            }
        } else if (BUTTON_CANCEL.equals(e.getActionCommand())) {
            setVisible(false);
            dispose();
        }
    }

    /**
     * Called when the window is closed
     */
    @Override
    public void dispose() {
        // This appears to be the best we can to to clear any password still stored internally
        _passwordField1.setText(null);
        if (_multipleEntry) {
            _passwordField2.setText(null);
            _passwordField3.setText(null);
        }
    }
}
