package pwsafe.gui;

import java.awt.BorderLayout;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import pwsafe.store.Attachment;

/**
 * Dialog to view and edit a single Attachment on a PasswordEntry
 *
 * @author Nick Clarke
 */
public class AttachmentDialog extends JDialog implements ActionListener {

    private static final String OK_BUTTON_TEXT = "OK";
    private static final String CANCEL_BUTTON_TEXT = "Cancel";
    private static final String LOAD_FILE_BUTTON_TEXT = "Load file into store...";
    private static final String SAVE_FILE_BUTTON_TEXT = "Save stored file to disk...";

    private static enum ButtonAction {
        OK,
        CANCEL,
        LOAD_FILE,
        SAVE_FILE
    }

// Attachment being edited
    private Attachment _attachment;
    private byte[] _attachmentFileContent = null;
    private boolean _fileContentChanged;

    private boolean _ok = false;

// Attachment editing
    private JTextField _attachmentFilenameField;
    private JLabel _attachmentCreatedField;
    private JTextArea _attachmentDescriptionField;
    private JButton _okButton;
    private JButton _cancelButton;
    private JButton _loadFileButton;
    private JButton _saveFileButton;
    private JFileChooser _fileChooser;


    /**
     * Construct a AttachmentDialog
     *
     * @param attachment the attachment to view/edit, must not be null
     */
    public AttachmentDialog(final Frame parent, final Attachment attachment, final String title) {
        super(parent, title, true);
        if (attachment == null) {
            throw new IllegalArgumentException("attachment must not be null");
        }
        _attachment = attachment;
        _fileContentChanged = false;
        setup();
    }

    private void setup() {
        /* TODO: change to only hide when closed (but clear private data), have a single instance in MainWindow,
                 set properties as needed for each showing */
        setDefaultCloseOperation(DISPOSE_ON_CLOSE /*DO_NOTHING_ON_CLOSE*/);
        // This is invoked before disposing if closed
        /*
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    onCancel();
                }
            });
        */

        _fileChooser = new JFileChooser();

        // Top-level panel inside this dialog
        JPanel mainContentPane = new JPanel(new BorderLayout());
        // mainContentPane.setBorder(someBorder);

        mainContentPane.add(createAttachmentEditPanel(), BorderLayout.CENTER);

        mainContentPane.add(createOKCancelButtonsPanel(), BorderLayout.SOUTH);

        setContentPane(mainContentPane);

        getRootPane().setDefaultButton(_okButton);

        populateAttachmentFields();

        // Auto-size based on components
        pack();
    }

    // TODO: move this somewhere it can be shared with MainWindow
    // Mon 1 Jan 2001 02:03
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE d MMM yyyy HH:mm");

    private String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    private void populateAttachmentFields() {
        _attachmentFilenameField.setText(_attachment.getFilename());
        _attachmentDescriptionField.setText(_attachment.getDescription());
        _attachmentCreatedField.setText(formatDate(_attachment.getAttachmentCreated()));
        _attachmentFileContent = _attachment.getFileContent();
        // TODO: show info about content
        // TODO: _attachmentContentLastChangedField.setText(formatDate(_attachment.getFileContentLastChanged()));
    }

    private void clearAttachmentFields() {
        _attachmentFilenameField.setText(null);
        _attachmentDescriptionField.setText(null);
        _attachmentCreatedField.setText(null);
        // TODO: _attachmentContentLastChangedField.setText(null);
        // TODO: clear info about content
    }

    private Component createAttachmentEditPanel() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel panel = new JPanel(gridbag);
        GridBagConstraints c = new GridBagConstraints();

        // new Insets(top, left, bottom, right)
        final Insets textFieldLabelInsets = new Insets(0, 2, 0, 0);
        final Insets timestampLabelLabelInsets = new Insets(0, 2, 2, 0);
        final Insets timestampLabelInsets = new Insets(0, 0, 2, 0);
        final Insets textFieldInsets = new Insets(2, 2, 0, 2);
        final Insets zeroInsets = new Insets(0, 0, 0, 0);
        final int labelRightPad = 5;

        // Column 1
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.ipadx = labelRightPad;

        // Create fields to be labelled so we can set mnemonics on the labels
        _attachmentFilenameField = new JTextField();
        _attachmentDescriptionField = new JTextArea();

        c.gridheight = 2;
        c.insets = textFieldLabelInsets;
        JLabel label = new JLabel("Filename:");
        label.setLabelFor(_attachmentFilenameField);
        // label.setDisplayedMnemonic(KeyEvent.VK_?);
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy += 2;

        c.gridheight = 2;
        c.insets = textFieldLabelInsets;
        label = new JLabel("Description:");
        label.setLabelFor(_attachmentDescriptionField);
        // label.setDisplayedMnemonic(KeyEvent.VK_?);
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy += 2;

        c.gridheight = 1;
        c.gridwidth = 3;
        c.insets = zeroInsets;
        c.fill = GridBagConstraints.HORIZONTAL;
        _loadFileButton = makeButton(panel, LOAD_FILE_BUTTON_TEXT, KeyEvent.VK_L, ButtonAction.LOAD_FILE);
        gridbag.setConstraints(_loadFileButton, c);
        c.gridy++;

        c.gridheight = 1;
        c.gridwidth = 3;
        c.insets = zeroInsets;
        c.fill = GridBagConstraints.HORIZONTAL;
        _saveFileButton = makeButton(panel, SAVE_FILE_BUTTON_TEXT, KeyEvent.VK_S, ButtonAction.SAVE_FILE);
        gridbag.setConstraints(_saveFileButton, c);
        c.gridy++;

        // Column 2
        c.gridy = 0;
        c.gridx++;
        c.gridheight = 1;

        c.gridwidth = 2;
        c.weightx = 1.0;
        c.ipadx = 0;
        c.insets = textFieldInsets;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        // Field created above, just add to layout
        gridbag.setConstraints(_attachmentFilenameField, c);
        panel.add(_attachmentFilenameField);
        c.gridy++;

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.ipadx = labelRightPad;
        c.insets = timestampLabelLabelInsets;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        label = new JLabel("Created:");
        gridbag.setConstraints(label, c);
        panel.add(label);
        c.gridy++;

        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.ipadx = 0;
        c.insets = textFieldInsets;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.fill = GridBagConstraints.BOTH;
        // Field created above, just add to layout
        _attachmentDescriptionField.setLineWrap(true);
        _attachmentDescriptionField.setRows(4);
        _attachmentDescriptionField.setWrapStyleWord(true); // true - break on whitespace only
        JScrollPane scrollPane = new JScrollPane(_attachmentDescriptionField);
        gridbag.setConstraints(scrollPane, c);
        panel.add(scrollPane);
        c.gridy++;

        // TODO - file content length indicator and last changed time

        // Column 3
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.ipadx = 0;
        c.insets = timestampLabelInsets;
        c.gridx++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;

        c.gridy++;

        c.gridwidth = 1;
        _attachmentCreatedField = new JLabel(" ");
        gridbag.setConstraints(_attachmentCreatedField, c);
        panel.add(_attachmentCreatedField);
        c.gridy += 2;

        return panel;
    }

    private Component createOKCancelButtonsPanel() {
        Box box = Box.createHorizontalBox();
        // box.setBorder(BorderFactory.createLineBorder(Color.black));

        _okButton     = makeButton(box, OK_BUTTON_TEXT,     KeyEvent.VK_O, ButtonAction.OK);
        _cancelButton = makeButton(box, CANCEL_BUTTON_TEXT, KeyEvent.VK_C, ButtonAction.CANCEL);

        return box;
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

    private void saveFile() {
        if (_fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = _fileChooser.getSelectedFile();
            // Save the attachment content to the chosen disk file
            writeFile(file, _attachmentFileContent);
        }
    }

    private void writeFile(File file, byte[] content) {
        if (file.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Refusing to overwrite existing file '" + file.getName() + "' - please choose another filename");
            return;
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(content);
            out.close();
            out = null;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to save attachment to file '" + file.getName() + "':\n" + e.toString());
            return;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
        JOptionPane.showMessageDialog(this, "Saved ok: file '" + file.getName() + "', " + content.length + " bytes");
    }

    private void loadFile() {
        if (_fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = _fileChooser.getSelectedFile();
            // Read this disk file into the attachment content
            byte[] content = readFile(file);
            if (content == null) {
                return;
            }
            _attachmentFilenameField.setText(file.getName());
            clearFileContent();
            _attachmentFileContent = content;
            _fileContentChanged = true;
        }
    }

    private static long MAX_FILE_BYTES = 1024 * 1024 * 10;  // 10 Mb limit for now - intended only for key files etc

    private byte[] readFile(File file) {
        long length = file.length();
        if (length > MAX_FILE_BYTES) {
            JOptionPane.showMessageDialog(this,
                    "File '" + file.getName() + "' is too large to store (" + length + " bytes)");
            return null;
        }
        InputStream in = null;
        byte[] content;
        try {
            in = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) length);
            int count;
            byte[] buf = new byte[1024 * 4];
            while ((count = in.read(buf)) != -1) {
                out.write(buf, 0, count);
            }
            out.close();
            content = out.toByteArray();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to read contents of file '" + file.getName() + "':\n" + e.toString());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
        JOptionPane.showMessageDialog(this, "Read ok: file '" + file.getName() + "', " + content.length + " bytes");
        return content;
    }

    /**
     * Implementation of ActionListener
     */
    public void actionPerformed(ActionEvent e) {
        // Must be one of the buttons
        ButtonAction action = ButtonAction.valueOf(e.getActionCommand());
        switch (action) {
        case OK:
            _ok = true;
            setVisible(false);
            dispose();
            break;
        case CANCEL:
            setVisible(false);
            dispose();
            break;
        case LOAD_FILE:
            loadFile();
            break;
        case SAVE_FILE:
            saveFile();
            break;
        default:
            break;
        }
    }

    private void clearFileContent() {
        if (_fileContentChanged && _attachmentFileContent != null) {
            Arrays.fill(_attachmentFileContent, (byte) 0);
            _attachmentFileContent = null;
        }
    }

    public boolean showDialog() {
        setVisible(true);
        // It's modal, so when setVisible returns, user interaction has finished
        // First, store edited data into the attachment (which the caller has), then clear private data
        if (_ok) {
            // Only the filename name is required, other fields can be set to empty
            String filename = _attachmentFilenameField.getText().trim();
            if ("".equals(filename)) {
                filename = _attachment.getFilename();
            }
            // Save into the attachment, then it's no longer this dialog's responsibility
            _attachment.setAllFields(filename,
                                     _attachmentFileContent,
                                     _attachmentDescriptionField.getText());
            _attachmentFileContent = null;
        } else {
            clearFileContent();
        }
        clearAttachmentFields();
        _attachment = null;
        return _ok;
    }

    /**
     * Called when the window is closed
     */
    @Override
    public void dispose() {
    }
}
