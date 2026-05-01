import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TrackControlPanel extends JPanel {
    private final SoundPlayer.Track track;
    private final JTextField nameField;
    private final JToggleButton[] filterButtons;
    private final int[] filterKeys; // Maps button index to filter key number (2-5)

    public TrackControlPanel(SoundPlayer.Track track, int key2, int key3, int key4, int key5) {
        this.track = track;
        this.filterKeys = new int[] { 2, 3, 4, 5 };
        setPreferredSize(new Dimension(100, 50));
        setMaximumSize(new Dimension(100, 50));
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        nameField = new JTextField(track.getName() != null ? track.getName() : "Track", 8);
        nameField.setMaximumSize(new Dimension(90, 20));
        nameField.setFont(new Font("Arial", Font.PLAIN, 9));
        nameField.setFocusable(false);
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                track.setName(nameField.getText());
                nameField.setFocusable(false);
            }
        });
        nameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                nameField.setFocusable(true);
                nameField.requestFocusInWindow();
            }
        });

        add(nameField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 1));
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.setMaximumSize(new Dimension(90, 25));

        filterButtons = new JToggleButton[4];
        int[] keyArr = { key2, key3, key4, key5 };
        for (int i = 0; i < 4; i++) {
            String buttonLabel = String.valueOf(filterKeys[i]);
            filterButtons[i] = new JToggleButton(buttonLabel);
            filterButtons[i].setPreferredSize(new Dimension(20, 18));
            filterButtons[i].setFont(new Font("Arial", Font.PLAIN, 8));
            filterButtons[i].setSelected(track.hasFilter(filterKeys[i]));
            final int filterIndex = i;
            filterButtons[i].addActionListener(e -> {
                track.setFilter(filterKeys[filterIndex], filterButtons[filterIndex].isSelected());
            });
            buttonPanel.add(filterButtons[i]);
        }

        add(buttonPanel);
    }

    public void updateFilterButtons() {
        for (int i = 0; i < 4; i++) {
            filterButtons[i].setSelected(track.hasFilter(filterKeys[i]));
        }
    }

    public void updateName() {
        nameField.setText(track.getName());
    }
}
