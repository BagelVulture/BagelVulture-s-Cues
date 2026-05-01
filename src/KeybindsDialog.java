import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class KeybindsDialog extends JDialog {
    private int advanceKeyCode;
    private int startKeyCode;
    private int key2Code;
    private int key3Code;
    private int key4Code;
    private int key5Code;
    private boolean capturingAdvance;
    private boolean capturingStart;
    private boolean capturing2;
    private boolean capturing3;
    private boolean capturing4;
    private boolean capturing5;

    public KeybindsDialog(JFrame owner, int advanceKey, int startKey, int k2, int k3, int k4, int k5, 
                         Consumer<int[]> onSave) {
        super(owner, "Keybinds", true);
        advanceKeyCode = advanceKey;
        startKeyCode = startKey;
        key2Code = k2;
        key3Code = k3;
        key4Code = k4;
        key5Code = k5;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField advanceField = createCaptureField(KeyEvent.getKeyText(advanceKey));
        JTextField startField = createCaptureField(KeyEvent.getKeyText(startKey));
        JTextField key2Field = createCaptureField(KeyEvent.getKeyText(k2));
        JTextField key3Field = createCaptureField(KeyEvent.getKeyText(k3));
        JTextField key4Field = createCaptureField(KeyEvent.getKeyText(k4));
        JTextField key5Field = createCaptureField(KeyEvent.getKeyText(k5));

        KeyAdapter keyCapture = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (capturingAdvance) {
                    advanceKeyCode = e.getKeyCode();
                    advanceField.setText(KeyEvent.getKeyText(advanceKeyCode));
                    capturingAdvance = false;
                } else if (capturingStart) {
                    startKeyCode = e.getKeyCode();
                    startField.setText(KeyEvent.getKeyText(startKeyCode));
                    capturingStart = false;
                } else if (capturing2) {
                    key2Code = e.getKeyCode();
                    key2Field.setText(KeyEvent.getKeyText(key2Code));
                    capturing2 = false;
                } else if (capturing3) {
                    key3Code = e.getKeyCode();
                    key3Field.setText(KeyEvent.getKeyText(key3Code));
                    capturing3 = false;
                } else if (capturing4) {
                    key4Code = e.getKeyCode();
                    key4Field.setText(KeyEvent.getKeyText(key4Code));
                    capturing4 = false;
                } else if (capturing5) {
                    key5Code = e.getKeyCode();
                    key5Field.setText(KeyEvent.getKeyText(key5Code));
                    capturing5 = false;
                }
            }
        };

        setupFieldCapture(advanceField, keyCapture, () -> { capturingAdvance = true; capturingStart = false; capturing2 = false; capturing3 = false; capturing4 = false; capturing5 = false; });
        setupFieldCapture(startField, keyCapture, () -> { capturingStart = true; capturingAdvance = false; capturing2 = false; capturing3 = false; capturing4 = false; capturing5 = false; });
        setupFieldCapture(key2Field, keyCapture, () -> { capturing2 = true; capturingAdvance = false; capturingStart = false; capturing3 = false; capturing4 = false; capturing5 = false; });
        setupFieldCapture(key3Field, keyCapture, () -> { capturing3 = true; capturingAdvance = false; capturingStart = false; capturing2 = false; capturing4 = false; capturing5 = false; });
        setupFieldCapture(key4Field, keyCapture, () -> { capturing4 = true; capturingAdvance = false; capturingStart = false; capturing2 = false; capturing3 = false; capturing5 = false; });
        setupFieldCapture(key5Field, keyCapture, () -> { capturing5 = true; capturingAdvance = false; capturingStart = false; capturing2 = false; capturing3 = false; capturing4 = false; });

        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel("Advance cue key (1):"), c);
        c.gridx = 1;
        add(advanceField, c);

        c.gridx = 0;
        c.gridy = 1;
        add(new JLabel("Start/stop key (R):"), c);
        c.gridx = 1;
        add(startField, c);

        c.gridx = 0;
        c.gridy = 2;
        add(new JLabel("Advance cue key (2):"), c);
        c.gridx = 1;
        add(key2Field, c);

        c.gridx = 0;
        c.gridy = 3;
        add(new JLabel("Advance cue key (3):"), c);
        c.gridx = 1;
        add(key3Field, c);

        c.gridx = 0;
        c.gridy = 4;
        add(new JLabel("Advance cue key (4):"), c);
        c.gridx = 1;
        add(key4Field, c);

        c.gridx = 0;
        c.gridy = 5;
        add(new JLabel("Advance cue key (5):"), c);
        c.gridx = 1;
        add(key5Field, c);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        c.gridx = 0;
        c.gridy = 6;
        add(saveButton, c);
        c.gridx = 1;
        add(cancelButton, c);

        saveButton.addActionListener(e -> {
            onSave.accept(new int[] { advanceKeyCode, startKeyCode, key2Code, key3Code, key4Code, key5Code });
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(owner);
    }

    private void setupFieldCapture(JTextField field, KeyAdapter keyCapture, Runnable onCapture) {
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onCapture.run();
                field.setText("Press a key...");
                field.requestFocusInWindow();
            }
        });
        field.addKeyListener(keyCapture);
    }

    private JTextField createCaptureField(String text) {
        JTextField field = new JTextField(text, 10);
        field.setEditable(false);
        field.setFocusable(true);
        field.setBackground(Color.WHITE);
        return field;
    }
}
