package org.lineageos.sleepaudio.utils

object Constants {
    const val TAG = "SleepAudio"
    const val PREF_NAME = "SleepAudioSettings"
    const val CHANNEL_ID = "sleepaudio_service"

    // Master Switch
    const val KEY_ENABLE = "sleep_audio_enable"

    // Profiles
    const val KEY_PROFILE = "sleep_audio_profile"
    const val PROFILE_DYNAMIC = 0
    const val PROFILE_MUSIC = 1
    const val PROFILE_MOVIE = 2
    const val PROFILE_GAME = 3
    const val PROFILE_SLEEP = 4
    const val PROFILE_HIFI = 5
    const val PROFILE_CUSTOM = 6

    // Features
    const val KEY_VIRTUALIZER_ENABLE = "sleep_audio_virtualizer"
    const val KEY_VIRTUALIZER_STRENGTH = "sleep_audio_virtualizer_strength" // 0-100

    const val KEY_BASS_ENABLE = "sleep_audio_bass"
    const val KEY_BASS_STRENGTH = "sleep_audio_bass_strength" // 0-100

    const val KEY_DIALOGUE_ENABLE = "sleep_audio_dialogue"
    const val KEY_DIALOGUE_AMOUNT = "sleep_audio_dialogue_amount" // 0-100

    const val KEY_VOLUME_LEVELER = "sleep_audio_volume"
    
    const val KEY_STEREO_WIDENING = "sleep_audio_stereo_widening" // 0-100
    
    // Graphic Equalizer (GEQ) - 10 Bands
    const val KEY_GEQ_PREFIX = "sleep_audio_geq_band_"
}
