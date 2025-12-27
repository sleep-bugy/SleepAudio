# SleepAudio

System-wide audio enhancement module for Android, built on the native DynamicsProcessing API. 
Designed as a clean, conflict-free alternative to heavier mods like Viper4Android or Dolby, specifically for AOSP-based ROMs.

## Overview

SleepAudio provides advanced DSP capabilities without modifying system partitions or requiring root/Magisk. It functions as a privileged system app, utilizing standard Android audio effect frameworks to ensure stability and compatibility.

### Key Features
*   **Native DSP Engine**: Runs entirely within the Android Audio Framework (no custom driver injection).
*   **10-Band Graphic Equalizer**: Fully customizable manual EQ with granular gain control.
*   **Environmental Reverb**: Preset-based reverberation (Hall, Room, Plate, etc.) for spatial depth.
*   **Dynamic Range Compressor (DRC)**: Professional-grade compressor to manage audio peaks and balance volume.
*   **Profile System**: Auto-switching profiles for Headset vs. Speaker.
*   **Conflict Safe**: Uses standard effect priorities (0) and specific session IDs to coexist with other audio enhancements.

## Integration

### Requirements
*   Android 13+ (API 33+)
*   AOSP source tree (LineageOS, PixelExperience, etc.)

### Setup
1.  Clone into `packages/apps/SleepAudio`:
    ```bash
    git clone https://github.com/sleep-bugy/SleepAudio packages/apps/SleepAudio
    ```

2.  Add to build configuration (`device.mk`):
    ```makefile
    $(call inherit-product, packages/apps/SleepAudio/sleepaudio.mk)
    ```

3.  Build.

## License
Apache 2.0
