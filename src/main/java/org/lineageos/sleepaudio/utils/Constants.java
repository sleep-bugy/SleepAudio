package org.lineageos.sleepaudio.utils;

public class Constants {
    public static final String PREF_NAME = "SleepAudioSettings";
    public static final String TAG = "SleepAudio";
    
    // Master Switch
    public static final String KEY_ENABLE = "sleep_audio_enable";
    
    // Profiles
    public static final String KEY_PROFILE = "sleep_audio_profile";
    public static final int PROFILE_DYNAMIC = 0;
    public static final int PROFILE_MUSIC = 1;
    public static final int PROFILE_MOVIE = 2;
    public static final int PROFILE_GAME = 3;
    public static final int PROFILE_SLEEP = 4;
    public static final int PROFILE_HIFI = 5;

    // Feature Toggles (Mimicking Dolby)
    public static final String KEY_VIRTUALIZER_ENABLE = "sleep_audio_virtualizer";
    public static final String KEY_BASS_ENABLE = "sleep_audio_bass";
    public static final String KEY_DIALOGUE_ENABLE = "sleep_audio_dialogue";
    public static final String KEY_VOLUME_LEVELER = "sleep_audio_volume";
    
    // Feature Amounts (0-100 or specific ranges)
    public static final String KEY_BASS_STRENGTH = "sleep_audio_bass_strength";
    public static final String KEY_VIRTUALIZER_STRENGTH = "sleep_audio_virtualizer_strength";
    public static final String KEY_DIALOGUE_AMOUNT = "sleep_audio_dialogue_amount";
    
    // Notification
    public static final String CHANNEL_ID = "sleepaudio_service";
}
