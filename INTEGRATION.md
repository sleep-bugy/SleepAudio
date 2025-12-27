# Device Integration Guide

This guide explains how to properly integrate the **SleepAudio** standalone module into your Android device tree structure.

## 1. Directory Placement
Move the entire `SleepAudio` folder to the `packages/apps/` directory in your ROM source tree.

**Path:** `packages/apps/SleepAudio/`

## 2. Build Integration (device.mk)

You have two options to add SleepAudio to your build:

### Option A: The "Pro" Way (Recommended)
Add this single line to your `device.mk` to inherit the configuration:

```makefile
$(call inherit-product, packages/apps/SleepAudio/sleepaudio.mk)
```

### Option B: The Manual Way
Add `SleepAudio` directly to your `PRODUCT_PACKAGES` list:

```makefile
PRODUCT_PACKAGES += \
    SleepAudio
```

The `Android.bp` file included in this repository already handles the building of the app and the installation of the privileged permission whitelist XML.

## 3. SEPolicy (Optional but Recommended)
Since SleepAudio interacts with audio services, in strict enforcing builds, you might rarely encounter denials if the `system_app` domain is restricted. 

If you see denials related to `audioserver` or `servicemanager`, add the following to your `device/xiaomi/peridot/sepolicy/vendor/system_app.te`:

```sepolicy
# Allow SleepAudio to interact with audio services
allow system_app audioserver_service:service_manager find;
allow system_app audioserver:binder call;
```
*Note: In most AOSP/LineageOS ROMs, standard system apps already have these permissions.*

## 4. Usage
After building and flashing, **SleepAudio** will appear in your App Drawer and as a Quick Settings Tile.
1.  Open the App.
2.  Grant Notification Permission (for foreground service).
3.  Enable the Master Switch.
4.  (Optional) Add the "SleepAudio" tile to your QS panel for quick toggling.

## Troubleshooting
*   **App Crashes on Start**: Ensure `privapp-permissions-sleepaudio.xml` is correctly installed to `/product/etc/permissions/`.
*   **No Effect**: Ensure no other conflicting audio mods (Dirac, Dolby, Viper) are aggressively seizing the audio session `0`. SleepAudio uses `priority=0` to be polite, but some mods might block it.
