# Changelog

## v1.0.4 (2025-12-27)

**Hotfix:**
- Fixed SELinux context not being set for permission file
- Added install verification to customize.sh
- Improved module installation reliability for KernelSU

## v1.0.3 (2025-12-27)

**Hotfix:**
- Fixed critical permission issue that prevented root detection from working
- Added missing MODIFY_AUDIO_SETTINGS_PRIVILEGED to Magisk module
- Main toggle now properly enables when installed as system app
- Audio effects can now modify global audio session

## v1.0.2 (2025-12-27)

- Updated QS tile icon to match app launcher icon (cleaner minimal design)

## v1.0.1 (2025-12-27)

**Hotfix:**
- Fixed crash when installed as system app (removed android:persistent flag)
- App now launches correctly after Magisk/KSU module installation

## v1.0 (2025-12-27)

Initial release.

**What works:**
- Bass boost & virtualizer
- 10-band equalizer with dB values shown
- Dialog enhancer, volume leveler
- Reverb effects
- Dynamic range compressor
- Sleep mode (warm filter for night listening)
- Quick settings tile
- Auto-starts when you toggle it on

**UI:**
- Blue theme (no more purple)
- Fixed alignment issues
- About page shows if you have root access

**Installation:**
- Systemless Magisk/KSU module
- Auto-update from GitHub when new version drops

**Known issues:**
- Needs system app installation (root required)
- No reboot needed after toggle, just reopen the app

