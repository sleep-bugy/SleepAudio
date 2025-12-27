# SleepAudio Integration Config
# Usage: $(call inherit-product, packages/apps/SleepAudio/sleepaudio.mk)

# 1. Main Package
PRODUCT_PACKAGES += \
    SleepAudio

# 2. Configurations
PRODUCT_PACKAGES += \
    privapp_whitelist_org.lineageos.sleepaudio \
    SleepAudioConfigOverlay

# 3. SEPolicy (If BOARD_VENDOR_SEPOLICY_DIRS is handled by device, 
# typically we don't add strictly here unless it's a board config, 
# but we document it. For 'complex' look, we define variables)

BOARD_SEPOLICY_DIRS += $(LOCAL_PATH)/sepolicy
