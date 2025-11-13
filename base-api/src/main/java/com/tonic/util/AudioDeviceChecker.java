package com.tonic.util;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * A utility class so we can check if the current system has an audio device or not. The OSRS gamepack has
 * a memory leak that occurs when ran on servers with no audio devices due to sounds queueing up but never
 * getting fired or cleared.
 */
public class AudioDeviceChecker {
    /**
     * Checks if the system has an audio device
     * @return bool
     */
    public static boolean hasAudioDevice() {
        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            return mixerInfos != null && mixerInfos.length > 0;
        } catch (Throwable t) {
            return false;
        }
    }
}
