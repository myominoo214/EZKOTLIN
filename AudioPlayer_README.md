# AudioPlayer Utility

A Kotlin utility class for playing audio feedback sounds in your application, similar to the JavaScript implementation.

## Features

- Play success, duplicate, and fail sounds
- Enable/disable audio playback
- Non-blocking audio playback
- Automatic resource cleanup
- Error handling for missing or unsupported audio files

## Usage

### Basic Usage

```kotlin
import utils.AudioPlayer

// Play different sounds
AudioPlayer.playSuccessSong()    // Play success sound
AudioPlayer.playDuplicateSong()  // Play duplicate sound
AudioPlayer.playFailSong()       // Play fail sound

// Control audio settings
AudioPlayer.setSongEnabled(false) // Disable audio
AudioPlayer.setSongEnabled(true)  // Enable audio
val isEnabled = AudioPlayer.isSongEnabled() // Check if enabled
```

### Integration Example

```kotlin
fun handleSaveOperation(result: SaveResult) {
    when (result) {
        SaveResult.SUCCESS -> {
            showSuccessMessage("Data saved successfully!")
            AudioPlayer.playSuccessSong()
        }
        SaveResult.DUPLICATE -> {
            showWarningMessage("Duplicate entry detected!")
            AudioPlayer.playDuplicateSong()
        }
        SaveResult.ERROR -> {
            showErrorMessage("Failed to save data!")
            AudioPlayer.playFailSong()
        }
    }
}
```

## Required Audio Files

Place the following audio files in `src/main/resources/audio/`:

- `success.wav` - Success sound
- `duplicate.wav` - Duplicate/warning sound  
- `fail.wav` - Error/failure sound

### Supported Audio Formats

- WAV (recommended)
- AIFF
- AU

### Audio File Requirements

- Keep file sizes small (< 1MB) for quick loading
- Use short duration sounds (1-3 seconds)
- Consider using 16-bit, 44.1kHz sample rate for compatibility

## Implementation Details

### Thread Safety
- Audio playback runs in separate threads to avoid blocking the UI
- Multiple sounds can play simultaneously

### Error Handling
- Gracefully handles missing audio files
- Logs errors to console without crashing the application
- Continues execution even if audio playback fails

### Resource Management
- Automatically closes audio clips after playback
- Cleans up audio input streams
- No memory leaks from unclosed resources

## Example Audio Files

You can find free audio files from:
- [Freesound.org](https://freesound.org/)
- [Zapsplat](https://www.zapsplat.com/)
- [BBC Sound Effects](https://sound-effects.bbcrewind.co.uk/)

Or create simple beep sounds using audio editing software like Audacity.

## Testing

Run the example class to test audio functionality:

```bash
./gradlew run -PmainClass=examples.AudioPlayerExampleKt
```

## Notes

- Audio playback requires the Java Sound API (included in standard JDK)
- On some systems, audio might not work in headless environments
- Consider adding user preferences to control audio settings
- The utility is designed to be lightweight and non-intrusive