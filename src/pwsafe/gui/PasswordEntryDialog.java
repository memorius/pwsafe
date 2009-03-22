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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pwsafe.util.RandomPasswordGenerator;

/**
 * Modal dialog prompting for a password, with optional multiple-confirm
 *
 * @author Nick Clarke
 */
public class PasswordEntryDialog extends JDialog
        implements ActionListener, ItemListener, ChangeListener, DocumentListener {

    private static final String OK_BUTTON_TEXT = "OK";
    private static final String CANCEL_BUTTON_TEXT = "Cancel";
    private static final String SHOW_PASSWORDS_BUTTON_TEXT = "Show";
    private static final String HIDE_PASSWORDS_BUTTON_TEXT = "Hide";
    private static final String GENERATE_PASSWORD_BUTTON_TEXT = "Generate Password";

    private static final int PASSWORD_FIELD_COLUMNS = 20;

    // Set by constructor
    private final boolean _multipleEntry;
    private final boolean _allowEmptyPassword;
    private final RandomPasswordGenerator _randomPasswordGenerator;

    private boolean _ok = false;
    private boolean _passwordPlaintextVisible = false;
    private char[] _password;
    private JPasswordField _passwordField1;
    private JPasswordField _passwordField2; // only if multipleEntry
    private JPasswordField _passwordField3; // only if multipleEntry
    private JButton _showOrHidePasswordsButton;
    private JButton _okButton;
    private JButton _cancelButton;

    // These fields are only set if _randomPasswordGenerator != null
    private JSpinner _generatorLengthSpinner;
    private JCheckBox _generatorUseLowercaseAlphaCheckbox;
    private JCheckBox _generatorUseUppercaseAlphaCheckbox;
    private JCheckBox _generatorUseDigitsCheckbox;
    private JCheckBox _generatorUsePunctuationCheckbox;
    private JLabel _generatorAlphabetSizeField;
    private JLabel _generatorBitComplexityField;
    private JButton _generatePasswordButton;

// Action commands
    private static enum ButtonAction {
        OK,
        CANCEL,
        SHOW_HIDE_PASSWORDS,
        GENERATE_PASSWORD
    }

    /**
     * Construct a PasswordEntryDialog
     */
    public PasswordEntryDialog(final Frame parent, final String title, final boolean multipleEntry,
            final boolean showRandomPasswordGenerator, final boolean allowEmptyPassword) {
        super(parent, title, true);
        _multipleEntry = multipleEntry;
        _allowEmptyPassword = allowEmptyPassword;
        _randomPasswordGenerator = (showRandomPasswordGenerator ? new RandomPasswordGenerator() : null);
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

        if (_randomPasswordGenerator != null) {
            mainContentPane.add(createGeneratorPanel(), BorderLayout.EAST);
        }

        mainContentPane.add(createButtonsPanel(), BorderLayout.SOUTH);

        setContentPane(mainContentPane);

        getRootPane().setDefaultButton(_okButton);

        updateOKButtonState();

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
        c.anchor = GridBagConstraints.WEST;
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
        _passwordField1.getDocument().addDocumentListener(this);
        gridbag.setConstraints(_passwordField1, c);
        panel.add(_passwordField1);

        if (_multipleEntry) {
            c.gridy++;
            _passwordField2 = new JPasswordField(PASSWORD_FIELD_COLUMNS);
            _passwordField2.getDocument().addDocumentListener(this);
            gridbag.setConstraints(_passwordField2, c);
            panel.add(_passwordField2);

            c.gridy++;
            _passwordField3 = new JPasswordField(PASSWORD_FIELD_COLUMNS);
            _passwordField3.getDocument().addDocumentListener(this);
            gridbag.setConstraints(_passwordField3, c);
            panel.add(_passwordField3);
        }

        c.gridx++;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridheight = 3;
        c.insets = new Insets(0, 0, 0, 0);
        _showOrHidePasswordsButton = makeButton(panel, SHOW_PASSWORDS_BUTTON_TEXT, KeyEvent.VK_H,
                ButtonAction.SHOW_HIDE_PASSWORDS);
        // _showOrHidePasswordsButton.setMinimumSize(new Dimension(200, 0));
        gridbag.setConstraints(_showOrHidePasswordsButton, c);

        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        return panel;
    }

    private Component createGeneratorPanel() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel panel = new JPanel(gridbag);
        GridBagConstraints c = new GridBagConstraints();

        // Column 1
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;

        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        JLabel label = new JLabel("Random password generator");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.fill = GridBagConstraints.NONE;

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Length (characters):");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Use lowercase alpha:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Use uppercase alpha:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Use numbers:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Use punctuation:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Alphabet size:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        label = new JLabel("Complexity (bits):");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        _generatePasswordButton = makeButton(panel, GENERATE_PASSWORD_BUTTON_TEXT, KeyEvent.VK_G,
                ButtonAction.GENERATE_PASSWORD);
        gridbag.setConstraints(_generatePasswordButton, c);
        c.gridy++;

        // Column 2
        c.gridy = 0;
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1.0;

        c.gridy++;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(_randomPasswordGenerator.getLength(), 1, 10000, 1);
        _generatorLengthSpinner = new JSpinner(spinnerModel);
        _generatorLengthSpinner.addChangeListener(this);
        gridbag.setConstraints(_generatorLengthSpinner, c);
        panel.add(_generatorLengthSpinner);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        _generatorUseLowercaseAlphaCheckbox = new JCheckBox();
        // _generatorUseLowercaseAlphaCheckbox.setMnemonic(KeyEvent.VK_C); 
        _generatorUseLowercaseAlphaCheckbox.setSelected(_randomPasswordGenerator.getUseLowercaseAlpha());
        _generatorUseLowercaseAlphaCheckbox.addItemListener(this);
        gridbag.setConstraints(_generatorUseLowercaseAlphaCheckbox, c);
        panel.add(_generatorUseLowercaseAlphaCheckbox);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        _generatorUseUppercaseAlphaCheckbox = new JCheckBox();
        // _generatorUseUppercaseAlphaCheckbox.setMnemonic(KeyEvent.VK_C); 
        _generatorUseUppercaseAlphaCheckbox.setSelected(_randomPasswordGenerator.getUseUppercaseAlpha());
        _generatorUseUppercaseAlphaCheckbox.addItemListener(this);
        gridbag.setConstraints(_generatorUseUppercaseAlphaCheckbox, c);
        panel.add(_generatorUseUppercaseAlphaCheckbox);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        _generatorUseDigitsCheckbox = new JCheckBox();
        // _generatorUseDigitsCheckbox.setMnemonic(KeyEvent.VK_C); 
        _generatorUseDigitsCheckbox.setSelected(_randomPasswordGenerator.getUseDigits());
        _generatorUseDigitsCheckbox.addItemListener(this);
        gridbag.setConstraints(_generatorUseDigitsCheckbox, c);
        panel.add(_generatorUseDigitsCheckbox);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        _generatorUsePunctuationCheckbox = new JCheckBox();
        // _generatorUsePunctuationCheckbox.setMnemonic(KeyEvent.VK_C); 
        _generatorUsePunctuationCheckbox.setSelected(_randomPasswordGenerator.getUsePunctuation());
        _generatorUsePunctuationCheckbox.addItemListener(this);
        gridbag.setConstraints(_generatorUsePunctuationCheckbox, c);
        panel.add(_generatorUsePunctuationCheckbox);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        _generatorAlphabetSizeField = new JLabel();
        gridbag.setConstraints(_generatorAlphabetSizeField, c);
        panel.add(_generatorAlphabetSizeField);
        c.gridy++;

        c.anchor = GridBagConstraints.WEST;
        _generatorBitComplexityField = new JLabel();
        gridbag.setConstraints(_generatorBitComplexityField, c);
        panel.add(_generatorBitComplexityField);
        c.gridy++;

        updateGeneratorComplexityFields();

        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        return panel;
    }

    /**
     * ItemListener implementation
     */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == _generatorUseLowercaseAlphaCheckbox) {
            _randomPasswordGenerator.setUseLowercaseAlpha(e.getStateChange() == ItemEvent.SELECTED);
            updateGeneratorComplexityFields();
        } else if (source == _generatorUseUppercaseAlphaCheckbox) {
            _randomPasswordGenerator.setUseUppercaseAlpha(e.getStateChange() == ItemEvent.SELECTED);
            updateGeneratorComplexityFields();
        } else if (source == _generatorUseDigitsCheckbox) {
            _randomPasswordGenerator.setUseDigits(e.getStateChange() == ItemEvent.SELECTED);
            updateGeneratorComplexityFields();
        } else if (source == _generatorUsePunctuationCheckbox) {
            _randomPasswordGenerator.setUsePunctuation(e.getStateChange() == ItemEvent.SELECTED);
            updateGeneratorComplexityFields();
        }
    }

    /**
     * DocumentListener implementation for password fields
     */
    public void changedUpdate(DocumentEvent e) {}

    /**
     * DocumentListener implementation for password fields
     */
    public void insertUpdate(DocumentEvent e) {
        updateOKButtonState();
    }

    /**
     * DocumentListener implementation for password fields
     */
    public void removeUpdate(DocumentEvent e) {
        updateOKButtonState();
    }

    /**
     * ChangeListener implementation
     */
    public void stateChanged(ChangeEvent e) {
        Object source = e.getSource();
        if (source == _generatorLengthSpinner) {
            _randomPasswordGenerator.setLength(getSpinnerIntValue(_generatorLengthSpinner));
            updateGeneratorComplexityFields();
        }
    }

    private void updateGeneratorComplexityFields() {
        _generatorBitComplexityField.setText(Integer.toString(_randomPasswordGenerator.getBitComplexity()));
        int alphabetSize = _randomPasswordGenerator.getAlphabetSize();
        _generatorAlphabetSizeField.setText(Integer.toString(alphabetSize));
        _generatePasswordButton.setEnabled(alphabetSize > 0);
    }

    private int getSpinnerIntValue(JSpinner spinner) {
        try {
            spinner.commitEdit();
        } catch (ParseException e) {
            // Edited value is invalid: revert to last valid value
            ((DefaultEditor) spinner.getEditor()).getTextField().setValue(spinner.getValue());
        }
        return ((Integer) spinner.getValue()).intValue();
    }

    private void generatePassword() {
        char[] password = _randomPasswordGenerator.generatePassword();
        try {
            String passwordString = new String(password);
            _passwordField1.setText(passwordString);
            if (_multipleEntry) {
                _passwordField2.setText(passwordString);
                _passwordField3.setText(passwordString);
            }
        } finally {
            Arrays.fill(password, (char) 0);
        }
    }

    private Component createButtonsPanel() {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createLineBorder(Color.black));

        _okButton     = makeButton(box, OK_BUTTON_TEXT,     KeyEvent.VK_O,      ButtonAction.OK);
        _cancelButton = makeButton(box, CANCEL_BUTTON_TEXT, KeyEvent.VK_ESCAPE, ButtonAction.CANCEL);

        return box;
    }

    private void updateOKButtonState() {
        if (_allowEmptyPassword) {
            return; // always enabled
        }
        // Just check that at least one field is non-empty (confirmMatch will interrupt OK if they they don't match)
        char[] p1 = _passwordField1.getPassword();
        char[] p2 = (_multipleEntry ? _passwordField2.getPassword() : new char[0]);
        char[] p3 = (_multipleEntry ? _passwordField3.getPassword() : new char[0]);
        try {
            _okButton.setEnabled(p1.length != 0 || p2.length != 0 || p3.length != 0);
        } finally {
            Arrays.fill(p1, (char) 0);
            Arrays.fill(p2, (char) 0);
            Arrays.fill(p3, (char) 0);
        }
    }

    private void setPasswordPlaintextVisible(boolean visible) {
        _passwordPlaintextVisible = visible;

        _showOrHidePasswordsButton.setText(visible ? HIDE_PASSWORDS_BUTTON_TEXT
                                                   : SHOW_PASSWORDS_BUTTON_TEXT);
        _showOrHidePasswordsButton.setMnemonic(visible ? KeyEvent.VK_H : KeyEvent.VK_H);

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
        char[] result;
        if (_ok) {
            result = _password;
            assert (result != null);
        } else {
            result = null;
        }
        _password = null; // caller now takes responsibility for clearing the secret data when done
        return result;
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        // Must be one of the buttons
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
        case GENERATE_PASSWORD:
            generatePassword();
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
