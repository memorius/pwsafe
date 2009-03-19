package pwsafe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
 * Modal dialog prompting for a password, with optional multiple-confirm
 *
 * @author Nick Clarke
 */
public class PasswordEntryDialog extends JDialog implements ActionListener {

    private static final String OK_BUTTON_TEXT = "OK";
    private static final String CANCEL_BUTTON_TEXT = "Cancel";
    private static final String SHOW_PASSWORDS_BUTTON_TEXT = "Reveal";
    private static final String HIDE_PASSWORDS_BUTTON_TEXT = "Hide";

    private static final int PASSWORD_FIELD_COLUMNS = 20;

    private boolean _ok = false;
    private boolean _multipleEntry;
    private boolean _passwordPlaintextVisible = false;
    private char[] _password;
    private JPasswordField _passwordField1;
    private JPasswordField _passwordField2;
    private JPasswordField _passwordField3;
    private JButton _showOrHidePasswordsButton;
    private JButton _okButton;
    private JButton _cancelButton;

// Action commands
    private static enum ButtonAction {
        OK,
        CANCEL,
        SHOW_HIDE_PASSWORDS
    }

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

        mainContentPane.add(createGeneratorPanel(), BorderLayout.EAST);

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

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;

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
        c.weighty = 1.0;
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

        c.gridx++;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridheight = 3;
        c.insets = new Insets(0, 0, 0, 0);
        _showOrHidePasswordsButton = makeButton(panel, SHOW_PASSWORDS_BUTTON_TEXT, KeyEvent.VK_R, ButtonAction.SHOW_HIDE_PASSWORDS);
        // _showOrHidePasswordsButton.setMinimumSize(new Dimension(200, 0));
        gridbag.setConstraints(_showOrHidePasswordsButton, c);

        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        return panel;
    }

    private Component createGeneratorPanel() {
        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        // TODO: random password generation

        return box;
    }

    private Component createButtonsPanel() {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        _okButton     = makeButton(box, OK_BUTTON_TEXT,     KeyEvent.VK_O,      ButtonAction.OK);
        _cancelButton = makeButton(box, CANCEL_BUTTON_TEXT, KeyEvent.VK_ESCAPE, ButtonAction.CANCEL);

        return box;
    }

    private void setPasswordPlaintextVisible(boolean visible) {
        _passwordPlaintextVisible = visible;

        _showOrHidePasswordsButton.setText(visible ? HIDE_PASSWORDS_BUTTON_TEXT
                                                   : SHOW_PASSWORDS_BUTTON_TEXT);
        _showOrHidePasswordsButton.setMnemonic(visible ? KeyEvent.VK_H : KeyEvent.VK_S);

        _passwordField1.setEchoChar(visible ? ((char) 0) : '*');
        if (_multipleEntry) {
            _passwordField2.setEchoChar(visible ? ((char) 0) : '*');
            _passwordField3.setEchoChar(visible ? ((char) 0) : '*');
        }
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
        ButtonAction action = ButtonAction.valueOf(e.getActionCommand());
        switch (action) {
        case OK:
            if (confirmMatch()) {
                _password = _passwordField1.getPassword();
                _ok = true;
                setVisible(false);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "The entered passwords do not match");
            }
            break;
        case CANCEL:
            setVisible(false);
            dispose();
            break;
        case SHOW_HIDE_PASSWORDS:
            setPasswordPlaintextVisible(!_passwordPlaintextVisible);
            break;
        default:
            break;
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
