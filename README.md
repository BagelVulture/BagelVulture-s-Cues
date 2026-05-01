# BagelVulture's-Cues

A Java Swing application for a sequential sound player with a timeline interface.

## Features

- Timeline scaled by "cues" instead of time
- Multiple vertical tracks
- Add sounds via File > Insert Sound or drag-and-drop
- Drag sound widgets to different cues and tracks (snaps to positions)
- Play sounds sequentially in cue order
- Supports WAV, MP3, and MP4 files (audio extracted from MP4)

## Dependencies

- Java 8 or higher
- FFmpeg (for MP3 and MP4 playback)

Install FFmpeg if not already available:

```bash
# On Ubuntu/Debian
sudo apt update && sudo apt install ffmpeg

# On macOS
brew install ffmpeg

# On Windows, download from https://ffmpeg.org/download.html
```

## Running the Application

Compile and run the Java application:

```bash
javac src/SoundPlayer.java
java -cp src SoundPlayer
```

Note: Requires a graphical environment to display the Swing GUI.

## Usage

- Use File > Insert Sound to add audio files (supports WAV, MP3, and MP4)
- Drag files from file system into the timeline to add them
- Drag the blue rectangles (sound widgets) to reposition them on the timeline
- Each new widget is initially shorter than one cue span (about 3/5 of a cue)
- Drag the right edge of a widget to extend it to the next cue or beyond
- Longer widgets that span multiple cues loop until their end cue is reached
- Press Playback > Play or the spacebar to start interactive playback mode
- In playback mode:
  - The current cue is highlighted in blue
  - Press "1" to play all sounds on the current cue and advance to the next cue
  - Sounds/widgets before the current cue are greyed out

Note: MP3 and MP4 files are converted to WAV on-the-fly using FFmpeg for playback.

## Implementation Notes

- Cues are spaced 100 pixels apart horizontally
- Tracks are 50 pixels high
- Sound widgets snap to cue and track positions when dragged
- Playback sorts sounds by cue index, then by track index