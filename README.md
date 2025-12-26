# SleepAudio 🌙🎧

**SleepAudio** is a standalone, high-fidelity audio enhancement module designed for Android custom ROMs. It leverages the native Android `DynamicsProcessing` API to deliver a premium audio experience without the bloat or instability of legacy mods.

> **Designed for the Xiaomi POCO F6 (Peridot) and modern Android 14/15 ROMs.**

## ✨ Features

*   **🛡️ Conflict-Safe**: Engineered to coexist with Viper4Android, Dolby Atmos, and JamesDSP. Uses delayed initialization and safe-loading protocols.
*   **🔊 Advanced Dynamics Engine**:
    *   **Bass Enhancer**: Multiband Compressor (MBC) based bass boost for punchy, distortion-free low-end.
    *   **Volume Leveler**: Smart Limiter logic to maintain consistent volume across different media.
    *   **Spatial Surround**: Native Android `Virtualizer` for immersive 3D soundstage.
    *   **Dialogue Clarity**: Automatic vocal frequency lift (1kHz - 3kHz).
*   **🎹 Smart Profiles**:
    *   🎵 **Music**: V-Shape EQ for energetic listening.
    *   🎬 **Movie**: Cinematic bass and clear dialogue.
    *   🎮 **Game**: Footstep amplification (2kHz - 4kHz increase).
    *   💤 **Sleep**: Warm filter (high-frequency roll-off) for relaxing night listening.
    *   🧬 **Hi-Fi**: Bit-perfect passthrough mode with disabled compression and slight "Air" boost (>14kHz).
*   **🎨 Premium UI**: Material Design 3 dashboard with Dark/Light mode support.

## 📦 Installation

This module is designed to be included in the device tree source during build time.

1.  Clone this repository into `packages/apps/SleepAudio`:
    ```bash
    git clone https://github.com/sleep-bugy/SleepAudio packages/apps/SleepAudio
    ```
2.  Add to your `device.mk`:
    ```makefile
    PRODUCT_PACKAGES += SleepAudio
    ```
3.  Build your ROM!

## 🤝 Credits & Acknowledgements

This project wouldn't be possible without the inspiration and open-source work of the community:

*   **LineageOS Team**: For the base `SettingsLib` and SDK structure.
*   **Android Open Source Project (AOSP)**: For the powerful `DynamicsProcessing` and `AudioEffect` APIs.
*   **Viper4Android & Dolby Atmos Modules**: For inspiring the "Profile" based architecture and UI layout.
*   **Xiaomi**: For the device-specific audio HAL integrations on Peridot.

## 📜 License

This project is licensed under the **Apache 2.0 License** - see the [LICENSE](LICENSE) file for details.

---
Made with ❤️ by [Mohammad Adi](https://github.com/sleep-bugy)
