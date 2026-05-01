import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Comparator;

public class SoundPlayer extends JFrame {
    private JPanel timelinePanel;
    private List<Track> tracks;
    private int cueSpacing = 100;
    private int trackHeight = 50;
    private int numTracks = 20;
    private int numCues = 50;
    private boolean playingMode = false;
    private int currentCue = 0;
    private int advanceKey = KeyEvent.VK_1;
    private int startKey = KeyEvent.VK_R;
    private int key2 = KeyEvent.VK_2;
    private int key3 = KeyEvent.VK_3;
    private int key4 = KeyEvent.VK_4;
    private int key5 = KeyEvent.VK_5;
    private int lastAdvanceKey = -1;
    private List<ClipPlayback> activeClips = new ArrayList<>();
    private List<LoopPlayback> activeLoops = new ArrayList<>();
    private List<TrackControlPanel> trackPanels = new ArrayList<>();
    private JPanel trackControlsPanel;
    private List<SoundWidget> selectedWidgets = new ArrayList<>();
    private List<SoundWidget> copiedWidgets = null;
    private Point lastMousePoint = new Point(0, 0);
    private File currentSaveFile = null;

    private void stopAllSounds() {
        synchronized (activeLoops) {
            for (LoopPlayback lp : new ArrayList<>(activeLoops)) {
                lp.clip.stop();
                lp.clip.close();
                if (lp.tempFile != null) {
                    lp.tempFile.delete();
                }
                activeLoops.remove(lp);
            }
        }
        synchronized (activeClips) {
            for (ClipPlayback cp : new ArrayList<>(activeClips)) {
                cp.clip.stop();
                cp.clip.close();
                if (cp.tempFile != null) {
                    cp.tempFile.delete();
                }
                activeClips.remove(cp);
            }
        }
    }

    public SoundPlayer() {
        setTitle("BagelVulture's Cues");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        tracks = new ArrayList<>();
        for (int i = 0; i < numTracks; i++) {
            tracks.add(new Track(i));
        }

        timelinePanel = new TimelinePanel();

        // Create track controls panel in the first cue column
        trackControlsPanel = new JPanel();
        trackControlsPanel.setLayout(new BoxLayout(trackControlsPanel, BoxLayout.Y_AXIS));
        trackControlsPanel.setBackground(new Color(250, 250, 250));
        trackControlsPanel.setPreferredSize(new Dimension(cueSpacing, tracks.size() * trackHeight));
        trackControlsPanel.setMinimumSize(new Dimension(cueSpacing, tracks.size() * trackHeight));
        trackControlsPanel.setMaximumSize(new Dimension(cueSpacing, tracks.size() * trackHeight));

        for (Track track : tracks) {
            TrackControlPanel controlPanel = new TrackControlPanel(track, key2, key3, key4, key5);
            trackPanels.add(controlPanel);
            trackControlsPanel.add(controlPanel);
        }

        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension leftSize = trackControlsPanel.getPreferredSize();
                Dimension rightSize = timelinePanel.getPreferredSize();
                int width = leftSize.width + rightSize.width;
                int height = Math.max(leftSize.height, rightSize.height);
                return new Dimension(width, height);
            }
        };
        contentPanel.add(trackControlsPanel, BorderLayout.WEST);
        contentPanel.add(timelinePanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        add(scrollPane, BorderLayout.CENTER);

        createMenuBar();

        setVisible(true);

        timelinePanel.requestFocusInWindow();

        startPlayingMode();

        // Set up key bindings for global key handling
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(startKey, 0), "start");
        actionMap.put("start", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAllSounds();
                startPlayingMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(advanceKey, 0), "advance1");
        actionMap.put("advance1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playingMode) {
                    lastAdvanceKey = advanceKey;
                    playCurrentCue();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(key2, 0), "advance2");
        actionMap.put("advance2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playingMode) {
                    lastAdvanceKey = key2;
                    playCurrentCue();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(key3, 0), "advance3");
        actionMap.put("advance3", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playingMode) {
                    lastAdvanceKey = key3;
                    playCurrentCue();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(key4, 0), "advance4");
        actionMap.put("advance4", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playingMode) {
                    lastAdvanceKey = key4;
                    playCurrentCue();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(key5, 0), "advance5");
        actionMap.put("advance5", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playingMode) {
                    lastAdvanceKey = key5;
                    playCurrentCue();
                }
            }
        });
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelected();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), "paste");
        actionMap.put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteAt(lastMousePoint.x, lastMousePoint.y);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "back");
        actionMap.put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playingMode && currentCue > 0) {
                    currentCue--;
                    timelinePanel.repaint();
                }
            }
        });    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem insertSoundItem = new JMenuItem("Insert Sound");
        insertSoundItem.addActionListener(e -> insertSound());
        fileMenu.add(insertSoundItem);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> save());
        fileMenu.add(saveItem);
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.addActionListener(e -> saveAs());
        fileMenu.add(saveAsItem);
        JMenuItem loadItem = new JMenuItem("Load...");
        loadItem.addActionListener(e -> load());
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);

        JMenu playbackMenu = new JMenu("Playback");
        JMenuItem playItem = new JMenuItem("Play");
        playItem.addActionListener(e -> startPlayingMode());
        playbackMenu.add(playItem);
        menuBar.add(playbackMenu);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem cueCountItem = new JMenuItem("Cue Count...");
        cueCountItem.addActionListener(e -> changeCueCount());
        settingsMenu.add(cueCountItem);
        JMenuItem keybindsItem = new JMenuItem("Keybinds");
        keybindsItem.addActionListener(e -> openKeybindsDialog());
        settingsMenu.add(keybindsItem);
        menuBar.add(settingsMenu);

        setJMenuBar(menuBar);
    }

    private void openKeybindsDialog() {
        KeybindsDialog dialog = new KeybindsDialog(this, advanceKey, startKey, key2, key3, key4, key5, keyCodes -> {
            int advance = keyCodes[0];
            int start = keyCodes[1];
            int k2 = keyCodes[2];
            int k3 = keyCodes[3];
            int k4 = keyCodes[4];
            int k5 = keyCodes[5];
            if (advance == start || advance == k2 || advance == k3 || advance == k4 || advance == k5 ||
                start == k2 || start == k3 || start == k4 || start == k5 ||
                k2 == k3 || k2 == k4 || k2 == k5 || k3 == k4 || k3 == k5 || k4 == k5) {
                JOptionPane.showMessageDialog(this, "All keys must be different.", "Invalid keybinds", JOptionPane.ERROR_MESSAGE);
            } else {
                advanceKey = advance;
                startKey = start;
                key2 = k2;
                key3 = k3;
                key4 = k4;
                key5 = k5;
            }
        });
        dialog.setVisible(true);
    }

    private void changeCueCount() {
        String input = JOptionPane.showInputDialog(this, "Number of cues:", numCues);
        if (input == null) return;
        try {
            int newCount = Integer.parseInt(input.trim());
            if (newCount < 1) {
                JOptionPane.showMessageDialog(this, "Cue count must be at least 1.", "Invalid cue count", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newCount < numCues) {
                for (Track track : tracks) {
                    for (SoundWidget widget : track.getSounds()) {
                        if (widget.getStartCue() + widget.getSpan() > newCount) {
                            JOptionPane.showMessageDialog(this, "Cannot shrink cues because some sounds extend past the new end.", "Cannot shrink cues", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }
            numCues = newCount;
            currentCue = Math.min(currentCue, numCues - 1);
            timelinePanel.revalidate();
            timelinePanel.repaint();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid number.", "Invalid input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void save() {
        if (currentSaveFile == null) {
            saveAs();
        } else {
            saveTo(currentSaveFile);
        }
    }

    private void saveAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(currentSaveFile);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("BagelVulture's Cues Save File", "bvcsf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".bvcsf")) {
                file = new File(file.getParentFile(), file.getName() + ".bvcsf");
            }
            saveTo(file);
            currentSaveFile = file;
        }
    }

    private void load() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("BagelVulture Cue Save Files", "bvcsf"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFrom(fileChooser.getSelectedFile());
            currentSaveFile = fileChooser.getSelectedFile();
        }
    }

    private void saveTo(File file) {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("numCues:" + numCues);
            out.println("numTracks:" + numTracks);
            out.println("tracks:");
            for (Track track : tracks) {
                out.println("track " + track.getIndex() + " name:" + track.getName());
                out.print("track " + track.getIndex() + " filters:");
                for (int i = 1; i <= 5; i++) {
                    out.print(" " + track.hasFilter(i));
                }
                out.println();
            }
            out.println("sounds:");
            for (Track track : tracks) {
                for (SoundWidget widget : track.getSounds()) {
                    out.println("sound track:" + track.getIndex() + " start:" + widget.getStartCue() + " width:" + widget.getWidth() + " volume:" + widget.getVolumePercent() + " name:" + widget.getName() + " data:" + Base64.getEncoder().encodeToString(getWavBytes(widget.getFile())));
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFrom(File file) {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            int newNumCues = 50;
            int newNumTracks = 20;
            List<String> trackNames = new ArrayList<>();
            List<boolean[]> trackFilters = new ArrayList<>();
            List<SoundData> soundDatas = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                if (line.startsWith("numCues:")) {
                    newNumCues = Integer.parseInt(line.substring(8).trim());
                } else if (line.startsWith("numTracks:")) {
                    newNumTracks = Integer.parseInt(line.substring(10).trim());
                } else if (line.startsWith("track ")) {
                    int idx = parseIntField(line, "track ");
                    if (line.contains("name:")) {
                        String name = line.substring(line.indexOf("name:") + 5);
                        while (trackNames.size() <= idx) trackNames.add("Track " + (trackNames.size() + 1));
                        trackNames.set(idx, name);
                    } else if (line.contains("filters:")) {
                        boolean[] filters = new boolean[6];
                        String[] filterStrs = line.substring(line.indexOf("filters:") + 8).trim().split(" ");
                        for (int i = 0; i < filterStrs.length && i < 5; i++) {
                            filters[i+1] = Boolean.parseBoolean(filterStrs[i]);
                        }
                        while (trackFilters.size() <= idx) trackFilters.add(new boolean[6]);
                        trackFilters.set(idx, filters);
                    }
                } else if (line.startsWith("sound ")) {
                    int trackIdx = parseIntField(line, "track:");
                    int start = parseIntField(line, "start:");
                    int width = parseIntField(line, "width:");
                    int volume = 100;
                    if (line.contains("volume:")) {
                        try {
                            volume = parseIntField(line, "volume:");
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    String name = line.substring(line.indexOf("name:") + 5, line.indexOf(" data:"));
                    String dataStr = line.substring(line.indexOf("data:") + 5);
                    byte[] data = Base64.getDecoder().decode(dataStr);
                    soundDatas.add(new SoundData(trackIdx, start, width, volume, name, data));
                }
            }
            // Apply
            numCues = newNumCues;
            // For simplicity, assume numTracks same
            for (int i = 0; i < tracks.size(); i++) {
                if (i < trackNames.size()) {
                    tracks.get(i).setName(trackNames.get(i));
                }
                if (i < trackFilters.size()) {
                    boolean[] fs = trackFilters.get(i);
                    for (int j = 1; j <= 5; j++) {
                        tracks.get(i).setFilter(j, fs[j]);
                    }
                }
            }
            // Clear existing sounds
            for (Track track : tracks) {
                track.getSounds().clear();
            }
            // Add new sounds
            for (SoundData sd : soundDatas) {
                if (sd.trackIdx < tracks.size()) {
                    Path tempPath = Files.createTempFile("load_sound_", ".wav");
                    Files.write(tempPath, sd.data);
                    File tempFile = tempPath.toFile();
                    tempFile.deleteOnExit();
                    SoundWidget widget = new SoundWidget(tempFile, sd.start, sd.trackIdx, sd.width, sd.name, sd.volume);
                    tracks.get(sd.trackIdx).addSound(widget);
                }
            }
            // Update UI
            updateTrackPanels();
            timelinePanel.revalidate();
            timelinePanel.repaint();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int parseIntField(String line, String field) {
        int pos = line.indexOf(field);
        if (pos < 0) throw new IllegalArgumentException("Missing field: " + field);
        int start = pos + field.length();
        int end = start;
        while (end < line.length() && line.charAt(end) != ' ') {
            end++;
        }
        return Integer.parseInt(line.substring(start, end));
    }

    private byte[] getWavBytes(File file) throws IOException {
        File wavFile = file;
        if (!file.getName().toLowerCase().endsWith(".wav")) {
            // Convert to wav
            Path tempWav = Files.createTempFile("save_sound_", ".wav");
            File tempFile = tempWav.toFile();
            try {
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", file.getAbsolutePath(), "-vn", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", tempFile.getAbsolutePath());
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                Process p = pb.start();
                p.waitFor();
                wavFile = tempFile;
            } catch (Exception e) {
                throw new IOException("Failed to convert to wav", e);
            }
        }
        return Files.readAllBytes(wavFile.toPath());
    }

    private void updateTrackPanels() {
        for (int i = 0; i < trackPanels.size(); i++) {
            trackPanels.get(i).updateFilterButtons();
            trackPanels.get(i).updateName();
        }
    }

    private static class SoundData {
        int trackIdx, start, width, volume;
        String name;
        byte[] data;
        SoundData(int t, int s, int w, int v, String n, byte[] d) {
            trackIdx = t; start = s; width = w; volume = v; name = n; data = d;
        }
    }

    private void startPlayingMode() {
        playingMode = true;
        currentCue = 0;
        timelinePanel.repaint();
    }

    private void playCurrentCue() {
        int activeCue = currentCue;
        List<SoundWidget> cueSounds = new ArrayList<>();
        for (Track track : tracks) {
            if (!track.getSounds().isEmpty()) {
                for (SoundWidget widget : track.getSounds()) {
                    if (widget.getStartCue() == activeCue) {
                        boolean shouldPlay = !track.hasFilters();
                        if (!shouldPlay && lastAdvanceKey != -1) {
                            if ((lastAdvanceKey == advanceKey && track.hasFilter(1)) ||
                                (lastAdvanceKey == key2 && track.hasFilter(2)) ||
                                (lastAdvanceKey == key3 && track.hasFilter(3)) ||
                                (lastAdvanceKey == key4 && track.hasFilter(4)) ||
                                (lastAdvanceKey == key5 && track.hasFilter(5))) {
                                shouldPlay = true;
                            }
                        }
                        if (shouldPlay) {
                            cueSounds.add(widget);
                        }
                    }
                }
            }
        }
        // Sort by track
        cueSounds.sort(Comparator.comparingInt(w -> w.getTrackIndex()));
        // Advance cue immediately
        currentCue++;
        stopLoopsAtCue(currentCue);
        timelinePanel.repaint();
        // Play in background thread
        new Thread(() -> {
            for (SoundWidget widget : cueSounds) {
                if (!widget.isLooping()) {
                    playSound(widget.file);
                } else {
                    startLoopingSound(widget);
                }
            }
        }).start();
    }

    private void stopLoopsAtCue(int cue) {
        List<LoopPlayback> toStop = new ArrayList<>();
        for (LoopPlayback lp : activeLoops) {
            if (lp.endCue <= cue) {
                toStop.add(lp);
            }
        }
        for (LoopPlayback lp : toStop) {
            lp.clip.stop();
            lp.clip.close();
            if (lp.tempFile != null) {
                lp.tempFile.delete();
            }
            activeLoops.remove(lp);
        }
    }

    private void startLoopingSound(SoundWidget widget) {
        try {
            OpenClipResult result = openClip(widget.getFile());
            if (result == null) {
                return;
            }
            Clip clip = result.clip;
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            synchronized (activeLoops) {
                activeLoops.add(new LoopPlayback(clip, widget.getEndCue(), result.tempFile));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelected() {
        for (SoundWidget widget : selectedWidgets) {
            for (Track track : tracks) {
                track.getSounds().remove(widget);
            }
        }
        selectedWidgets.clear();
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    private void copySelected() {
        if (selectedWidgets.isEmpty()) return;
        int minCue = Integer.MAX_VALUE;
        int minTrack = Integer.MAX_VALUE;
        for (SoundWidget w : selectedWidgets) {
            minCue = Math.min(minCue, w.startCue);
            minTrack = Math.min(minTrack, w.getTrackIndex());
        }
        copiedWidgets = new ArrayList<>();
        for (SoundWidget w : selectedWidgets) {
            copiedWidgets.add(new SoundWidget(w.getFile(), w.startCue - minCue, w.getTrackIndex() - minTrack, w.getWidth(), w.getName(), w.getVolumePercent()));
        }
    }

    private void pasteAt(int x, int y) {
        if (copiedWidgets == null || copiedWidgets.isEmpty()) return;
        int targetCue = x / cueSpacing;
        int targetTrack = y / trackHeight;
        selectedWidgets.clear();
        for (SoundWidget copied : copiedWidgets) {
            int newCue = targetCue + copied.getStartCue();
            int newTrack = targetTrack + copied.getTrackIndex();
            if (newCue >= 0 && newTrack >= 0) {
                ensureTrackExists(newTrack);
                if (newCue * cueSpacing + copied.getWidth() <= numCues * cueSpacing && !overlapOnTrack(newTrack, newCue, copied.getWidth(), null)) {
                    SoundWidget newWidget = new SoundWidget(copied.getFile(), newCue, newTrack, copied.getWidth(), copied.getName(), copied.getVolumePercent());
                    tracks.get(newTrack).addSound(newWidget);
                    selectedWidgets.add(newWidget);
                }
            }
        }
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    private OpenClipResult openClip(File file) throws Exception {
        File audioFile = file;
        File tempFile = null;
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp3") || name.endsWith(".mp4")) {
            Path tempWav = Files.createTempFile("sound_", ".wav");
            tempFile = tempWav.toFile();
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", file.getAbsolutePath(), "-vn", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", tempFile.getAbsolutePath());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            p.waitFor();
            audioFile = tempFile;
        }
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        return new OpenClipResult(clip, tempFile);
    }

    private class OpenClipResult {
        Clip clip;
        File tempFile;

        OpenClipResult(Clip clip, File tempFile) {
            this.clip = clip;
            this.tempFile = tempFile;
        }
    }

    private class ClipPlayback {
        Clip clip;
        File tempFile;

        ClipPlayback(Clip clip, File tempFile) {
            this.clip = clip;
            this.tempFile = tempFile;
        }
    }

    private class LoopPlayback {
        Clip clip;
        int endCue;
        File tempFile;

        LoopPlayback(Clip clip, int endCue, File tempFile) {
            this.clip = clip;
            this.endCue = endCue;
            this.tempFile = tempFile;
        }
    }

    private void playSound(File file) {
        OpenClipResult result = null;
        try {
            result = openClip(file);
            if (result == null) {
                return;
            }
            Clip clip = result.clip;
            synchronized (activeClips) {
                activeClips.add(new ClipPlayback(clip, result.tempFile));
            }
            clip.start();
            Thread.sleep(clip.getMicrosecondLength() / 1000);
            clip.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (result != null) {
                Clip resultClip = result.clip;
                synchronized (activeClips) {
                    activeClips.removeIf(cp -> cp.clip == resultClip);
                }
                if (result.tempFile != null) {
                    result.tempFile.delete();
                }
            }
        }
    }

    private void insertSound() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Audio Files", "wav", "mp3", "mp4"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            if (selectedFiles.length == 0) {
                selectedFiles = new File[] { fileChooser.getSelectedFile() };
            }
            int trackIndex = 0;
            int cueIndex = 0;
            for (File file : selectedFiles) {
                for (int offset = 0; offset < numTracks; offset++) {
                    int tryTrack = (trackIndex + offset) % numTracks;
                    if (addSound(file, tryTrack, cueIndex)) {
                        trackIndex = tryTrack + 1;
                        break;
                    }
                }
            }
        }
    }

    private void ensureTrackExists(int trackIndex) {
        while (trackIndex >= tracks.size()) {
            tracks.add(new Track(tracks.size()));
        }
    }

    private boolean overlapOnTrack(int trackIndex, int startCue, int width, SoundWidget ignore) {
        if (trackIndex < 0) {
            return true;
        }
        ensureTrackExists(trackIndex);
        int thisStart = startCue * cueSpacing;
        int thisEnd = thisStart + width;
        for (SoundWidget widget : tracks.get(trackIndex).getSounds()) {
            if (widget != ignore) {
                int otherStart = widget.getX();
                int otherEnd = widget.getX() + widget.getWidth();
                if (thisStart < otherEnd && otherStart < thisEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean addSound(File file, int trackIndex, int cueIndex) {
        if (trackIndex < 0) {
            return false;
        }
        ensureTrackExists(trackIndex);
        int startCue = Math.max(0, cueIndex);
        int width = Math.max(40, cueSpacing * 3 / 5);
        if (startCue * cueSpacing + width > numCues * cueSpacing) {
            return false;
        }
        if (!overlapOnTrack(trackIndex, startCue, width, null)) {
            SoundWidget widget = new SoundWidget(file, startCue, trackIndex, width);
            tracks.get(trackIndex).addSound(widget);
            timelinePanel.revalidate();
            timelinePanel.repaint();
            return true;
        }
        return false;
    }

    private class TimelinePanel extends JPanel {
        public TimelinePanel() {
            setPreferredSize(new Dimension(1000, numTracks * trackHeight));
            setBackground(Color.WHITE);

            // Enable drag and drop
            setTransferHandler(new SoundTransferHandler());

            // Add mouse listeners for dragging widgets
            TimelineMouseListener mouseListener = new TimelineMouseListener();
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseListener);
        }

        @Override
        public Dimension getPreferredSize() {
            int lowestWidgetTrack = 0;
            int lowestNonDefaultTrack = 0;
            for (Track track : tracks) {
                if (!track.getSounds().isEmpty()) {
                    lowestWidgetTrack = Math.max(lowestWidgetTrack, track.getIndex());
                }
                if (track.hasNonDefaultSettings()) {
                    lowestNonDefaultTrack = Math.max(lowestNonDefaultTrack, track.getIndex());
                }
            }
            int lowestTrack = Math.max(lowestWidgetTrack, lowestNonDefaultTrack);
            int minHeight = (lowestTrack + 1) * trackHeight;
            int viewportHeight = 0;
            Container parent = getParent();
            if (parent instanceof JViewport) {
                viewportHeight = parent.getHeight();
            }
            int height = Math.max(Math.max(trackHeight, minHeight), viewportHeight);
            height = Math.max(height, numTracks * trackHeight);
            return new Dimension(numCues * cueSpacing, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Rectangle clip = g.getClipBounds();
            if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

            // Draw cues
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            int startCue = Math.max(0, clip.x / cueSpacing - 1);
            int endCue = Math.min(numCues, (clip.x + clip.width) / cueSpacing + 2);
            for (int i = startCue; i < endCue; i++) {
                int x = i * cueSpacing;
                if (playingMode && i == currentCue) {
                    g.setColor(Color.BLUE);
                    g.fillRect(x - 2, 0, 4, getHeight());
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawLine(x, 0, x, getHeight());
                }
                g.setColor(Color.DARK_GRAY);
                g.drawString(String.valueOf(i + 1), x + 2, 12);
            }

            // Draw tracks
            g.setColor(Color.GRAY);
            int trackLines = Math.max(getHeight() / trackHeight, tracks.size());
            for (int i = 0; i <= trackLines; i++) {
                int y = i * trackHeight;
                g.drawLine(0, y, getWidth(), y);
            }

            // Draw sound widgets
            for (Track track : tracks) {
                for (SoundWidget widget : track.getSounds()) {
                    if (widget.getX() + widget.getWidth() >= clip.x && widget.getX() <= clip.x + clip.width) {
                        boolean isPast = playingMode && widget.getX() < currentCue * cueSpacing;
                        widget.draw(g, isPast);
                    }
                }
            }
        }
    }

    public static class Track {
        private int index;
        private List<SoundWidget> sounds;
        private String name;
        private boolean[] filters = new boolean[6];

        public Track(int index) {
            this.index = index;
            this.name = "Track " + (index + 1);
            sounds = new ArrayList<>();
        }

        public void addSound(SoundWidget widget) {
            sounds.add(widget);
        }

        public List<SoundWidget> getSounds() {
            return sounds;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setFilter(int key, boolean enabled) {
            filters[key] = enabled;
        }

        public boolean hasFilter(int key) {
            return filters[key];
        }

        public boolean hasFilters() {
            for (int i = 1; i < 6; i++) {
                if (filters[i]) return true;
            }
            return false;
        }

        public boolean hasNonDefaultSettings() {
            return !name.equals("Track " + (index + 1)) || hasFilters();
        }
    }

    private class SoundWidget {
        private File file;
        private String name;
        private int startCue;
        private int width;
        private int y;
        private int height = 40;
        private boolean dragging = false;
        private boolean resizing = false;
        private int dragOffsetX, dragOffsetY;
        private int resizeOffsetX;
        private int originalStartCue, originalY;

        private int volumePercent = 100;

        public SoundWidget(File file, int startCue, int trackIndex, int width) {
            this.file = file;
            this.startCue = startCue;
            this.width = width;
            this.y = trackIndex * trackHeight;
            this.name = file.getName();
            this.volumePercent = 100;
        }

        public SoundWidget(File file, int startCue, int trackIndex, int width, String name) {
            this.file = file;
            this.startCue = startCue;
            this.width = width;
            this.y = trackIndex * trackHeight;
            this.name = name;
            this.volumePercent = 100;
        }

        public SoundWidget(File file, int startCue, int trackIndex, int width, String name, int volumePercent) {
            this.file = file;
            this.startCue = startCue;
            this.width = width;
            this.y = trackIndex * trackHeight;
            this.name = name;
            this.volumePercent = Math.max(0, Math.min(100, volumePercent));
        }

        public File getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getVolumePercent() {
            return volumePercent;
        }

        public void setVolumePercent(int volumePercent) {
            this.volumePercent = Math.max(0, Math.min(500, volumePercent));
        }

        public int getStartCue() {
            return startCue;
        }

        public int getEndCue() {
            return startCue + ((width + cueSpacing - 1) / cueSpacing);
        }

        public boolean isLooping() {
            return width >= cueSpacing;
        }

        public int getTrackIndex() {
            return y / trackHeight;
        }

        public int getX() {
            return startCue * cueSpacing;
        }

        public int getWidth() {
            return width;
        }

        public int getSpan() {
            return Math.max(1, (width + cueSpacing - 1) / cueSpacing);
        }

        public File getTempFile() {
            return null;
        }

        public void draw(Graphics g, boolean isPast) {
            if (selectedWidgets.contains(this)) {
                g.setColor(Color.RED);
            } else if (isPast) {
                g.setColor(Color.LIGHT_GRAY);
            } else {
                g.setColor(Color.BLUE);
            }
            g.fillRect(getX(), y, getWidth(), height);
            g.setColor(Color.BLACK);
            g.drawRect(getX(), y, getWidth(), height);
            // Resize handle highlight
            int handleLeft = getX() + getWidth() - 12;
            g.setColor(Color.ORANGE);
            g.fillRect(handleLeft, y + 4, 12, height - 8);
            g.setColor(Color.BLACK);
            g.drawRect(handleLeft, y + 4, 12, height - 8);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
            g.drawString(name, getX() + 5, y + 18);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.drawString(volumePercent + "%", getX() + 5, y + 32);
        }

        public boolean contains(int px, int py) {
            return px >= getX() && px <= getX() + getWidth() && py >= y && py <= y + height;
        }

        public boolean isOnResizeHandle(int px, int py) {
            int handleLeft = getX() + getWidth() - 10;
            return px >= handleLeft && px <= getX() + getWidth() && py >= y && py <= y + height;
        }

        public void startDrag(int px, int py) {
            if (selectedWidgets.contains(this)) {
                for (SoundWidget w : selectedWidgets) {
                    w.originalStartCue = w.startCue;
                    w.originalY = w.y;
                }
            }
            if (isOnResizeHandle(px, py)) {
                resizing = true;
                resizeOffsetX = getX() + getWidth() - px;
            } else {
                dragging = true;
                dragOffsetX = px - getX();
                dragOffsetY = py - y;
            }
        }

        public void drag(int px, int py) {
            if (resizing) {
                int newRight = px + resizeOffsetX;
                int newWidth = newRight - getX();
                int minWidth = cueSpacing * 3 / 5;
                newWidth = Math.max(minWidth, newWidth);
                newWidth = Math.min(newWidth, numCues * cueSpacing - getX());
                if (newWidth >= cueSpacing) {
                    int cues = Math.max(1, Math.round((float) newWidth / cueSpacing));
                    newWidth = cues * cueSpacing;
                } else {
                    int mid = (minWidth + cueSpacing) / 2;
                    newWidth = newWidth < mid ? minWidth : cueSpacing;
                }
                if (newWidth != width && !overlapOnTrack(getTrackIndex(), startCue, newWidth, this)) {
                    width = newWidth;
                }
            } else if (dragging) {
                int deltaX = px - dragOffsetX - getX();
                int deltaY = py - dragOffsetY - y;
                for (SoundWidget w : selectedWidgets) {
                    int newX = w.getX() + deltaX;
                    int newY = w.y + deltaY;
                    int centerY = newY + height / 2;
                    int newCue = Math.max(0, Math.round((float) newX / cueSpacing));
                    int newTrack = Math.max(0, centerY / trackHeight);
                        int oldTrack = w.getTrackIndex();
                    ensureTrackExists(newTrack);
                    if (newCue * cueSpacing + w.getWidth() <= numCues * cueSpacing && !overlapOnTrack(newTrack, newCue, w.getWidth(), w)) {
                        w.startCue = newCue;
                        if (newTrack != oldTrack) {
                            tracks.get(oldTrack).getSounds().remove(w);
                            tracks.get(newTrack).addSound(w);
                        }
                        w.y = newTrack * trackHeight;
                    }
                }
            }
        }

        public void stopDrag() {
            dragging = false;
            resizing = false;
        }
    }

    private class SoundTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                Point dropPoint = support.getDropLocation().getDropPoint();
                int cueIndex = dropPoint.x / cueSpacing;
                int startTrack = Math.max(0, dropPoint.y / trackHeight);
                int trackIndex = startTrack;
                int trackLimit = Math.max(tracks.size(), startTrack + files.size());
                for (File file : files) {
                    if (file.getName().endsWith(".wav") || file.getName().endsWith(".mp3") || file.getName().endsWith(".mp4")) {
                        boolean added = false;
                        for (int offset = 0; offset < trackLimit; offset++) {
                            int tryTrack = startTrack + offset;
                            ensureTrackExists(tryTrack);
                            if (addSound(file, tryTrack, cueIndex)) {
                                added = true;
                                trackIndex = tryTrack + 1;
                                break;
                            }
                        }
                        if (added) {
                            startTrack = trackIndex;
                            trackLimit = Math.max(trackLimit, startTrack + files.size());
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    // Mouse listener for dragging
    private class TimelineMouseListener implements MouseListener, MouseMotionListener {
        private SoundWidget draggedWidget = null;

        @Override
        public void mousePressed(MouseEvent e) {
            lastMousePoint = e.getPoint();
            if (SwingUtilities.isRightMouseButton(e)) {
                showContextMenu(e);
            } else {
                // Left click for selection
                boolean found = false;
                for (Track track : tracks) {
                    for (SoundWidget widget : track.getSounds()) {
                        if (widget.contains(e.getX(), e.getY())) {
                            found = true;
                            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                                if (selectedWidgets.contains(widget)) {
                                    selectedWidgets.remove(widget);
                                } else {
                                    selectedWidgets.add(widget);
                                }
                            } else {
                                selectedWidgets.clear();
                                selectedWidgets.add(widget);
                            }
                            timelinePanel.repaint();
                            break;
                        }
                    }
                    if (found) break;
                }
                if (!found) {
                    selectedWidgets.clear();
                    timelinePanel.repaint();
                }
                // Check for drag start
                draggedWidget = null;
                for (Track track : tracks) {
                    for (SoundWidget widget : track.getSounds()) {
                        if (widget.contains(e.getX(), e.getY())) {
                            draggedWidget = widget;
                            widget.startDrag(e.getX(), e.getY());
                            break;
                        }
                    }
                    if (draggedWidget != null) break;
                }
            }
        }

        private void showContextMenu(MouseEvent e) {
            JPopupMenu menu = new JPopupMenu();
            if (!selectedWidgets.isEmpty()) {
                JMenuItem copyItem = new JMenuItem("Copy");
                copyItem.addActionListener(ev -> copySelected());
                menu.add(copyItem);
            }
            if (copiedWidgets != null && !copiedWidgets.isEmpty()) {
                JMenuItem pasteItem = new JMenuItem("Paste");
                pasteItem.addActionListener(ev -> pasteAt(e.getX(), e.getY()));
                menu.add(pasteItem);
            }
            if (!selectedWidgets.isEmpty()) {
                JMenuItem deleteItem = new JMenuItem("Delete");
                deleteItem.addActionListener(ev -> deleteSelected());
                menu.add(deleteItem);
            }
            if (menu.getComponentCount() > 0) {
                menu.show(timelinePanel, e.getX(), e.getY());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (draggedWidget != null) {
                draggedWidget.drag(e.getX(), e.getY());
                timelinePanel.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggedWidget != null) {
                draggedWidget.stopDrag();
                draggedWidget = null;
            }
        }

        // Other methods
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int x = e.getX();
                int y = e.getY();
                for (Track track : tracks) {
                    for (SoundWidget widget : track.getSounds()) {
                        if (widget.contains(x, y)) {
                            JTextField nameField = new JTextField(widget.getName(), 20);
                            JTextField volumeField = new JTextField(String.valueOf(widget.getVolumePercent()), 4);
                            JPanel panel = new JPanel(new GridBagLayout());
                            GridBagConstraints gbc = new GridBagConstraints();
                            gbc.insets = new Insets(2, 2, 2, 2);
                            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
                            panel.add(new JLabel("Name:"), gbc);
                            gbc.gridx = 1; panel.add(nameField, gbc);
                            gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Volume (%):"), gbc);
                            gbc.gridx = 1; panel.add(volumeField, gbc);
                            int result = JOptionPane.showConfirmDialog(SoundPlayer.this, panel, "Edit sound", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                            if (result == JOptionPane.OK_OPTION) {
                                String newName = nameField.getText().trim();
                                String volumeText = volumeField.getText().trim();
                                if (!newName.isEmpty()) {
                                    widget.setName(newName);
                                }
                                try {
                                    int newVolume = Integer.parseInt(volumeText);
                                    widget.setVolumePercent(newVolume);
                                } catch (NumberFormatException ignored) {
                                }
                                timelinePanel.repaint();
                            }
                            return;
                        }
                    }
                }
            }
        }
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {
            lastMousePoint = e.getPoint();
            boolean onHandle = false;
            for (Track track : tracks) {
                for (SoundWidget widget : track.getSounds()) {
                    if (widget.isOnResizeHandle(e.getX(), e.getY())) {
                        onHandle = true;
                        break;
                    }
                }
                if (onHandle) break;
            }
            setCursor(onHandle ? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) : Cursor.getDefaultCursor());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SoundPlayer::new);
    }
}