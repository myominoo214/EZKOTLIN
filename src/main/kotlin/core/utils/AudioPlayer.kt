package core.utils

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException
import javax.sound.sampled.FloatControl
import java.io.IOException

class AudioPlayer {
    companion object {
        private var isSongEnabled: Boolean = true
        private var volume: Float = 1f // Default volume (0 to 1)
        
        // Audio file paths - using WAV format for better Java compatibility
        private const val SUCCESS_SONG = "audio/success.wav"
        private const val DUPLICATE_SONG = "audio/duplicate.wav"
        private const val FAIL_SONG = "audio/fail.wav"
        
        /**
         * Enable or disable audio playback
         */
        fun setSongEnabled(enabled: Boolean) {
            isSongEnabled = enabled
        }
        
        /**
         * Check if audio is enabled
         */
        fun isSongEnabled(): Boolean {
            return isSongEnabled
        }
        
        /**
         * Set the volume level (0.0 to 1.0)
         */
        fun setVolume(volumeLevel: Float) {
            volume = volumeLevel.coerceIn(0f, 1f)
        }
        
        /**
         * Get the current volume level
         */
        fun getVolume(): Float {
            return volume
        }
        
        /**
         * Play success sound
         */
        fun playSuccessSong() {
            if (isSongEnabled) {
                playAudio(SUCCESS_SONG)
            }
        }
        
        /**
         * Play duplicate sound
         */
        fun playDuplicateSong() {
            if (isSongEnabled) {
                playAudio(DUPLICATE_SONG)
            }
        }
        
        /**
         * Play fail sound
         */
        fun playFailSong() {
            if (isSongEnabled) {
                playAudio(FAIL_SONG)
            }
        }
        
        /**
         * Generic audio playback function
         * Uses a fallback approach for better compatibility
         */
        private fun playAudio(audioPath: String) {
            try {
                // First try to get the resource as a stream
                val resourceStream = AudioPlayer::class.java.classLoader.getResourceAsStream(audioPath)
                if (resourceStream == null) {
                    println("Audio file not found: $audioPath")
                    return
                }
                
                // Create a buffered input stream to support mark/reset
                val bufferedStream = resourceStream.buffered()
                
                // Get audio input stream
                val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(bufferedStream)
                
                // Get a sound clip resource
                val clip: Clip = AudioSystem.getClip()
                
                // Open audio clip and load samples from the audio input stream
                clip.open(audioInputStream)
                
                // Set volume if supported
                try {
                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                        val range = gainControl.maximum - gainControl.minimum
                        val gain = (range * volume) + gainControl.minimum
                        gainControl.value = gain
                    }
                } catch (e: Exception) {
                    println("Volume control not supported: ${e.message}")
                }
                
                // Start playing the audio
                clip.start()
                
                // Close resources in a separate thread to avoid blocking
                Thread {
                    try {
                        // Wait for the clip to finish playing
                        while (clip.isRunning) {
                            Thread.sleep(100)
                        }
                        clip.close()
                        audioInputStream.close()
                        bufferedStream.close()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }.start()
                
            } catch (e: UnsupportedAudioFileException) {
                println("Audio file format not supported: ${e.message}. Please use WAV format for better compatibility.")
            } catch (e: IOException) {
                println("Error reading audio file: ${e.message}")
            } catch (e: LineUnavailableException) {
                println("Audio line unavailable: ${e.message}")
            } catch (e: Exception) {
                println("Unexpected error playing audio: ${e.message}")
            }
        }
    }
}