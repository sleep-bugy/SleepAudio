#!/system/bin/sh

echo "=== SleepAudio Permission Diagnostics ==="
echo ""

echo "1. Checking if module is installed..."
if [ -d "/data/adb/modules/sleepaudio" ]; then
    echo "   ✓ Module directory found"
else
    echo "   ✗ Module directory NOT found"
    exit 1
fi

echo ""
echo "2. Checking permission XML file..."
if [ -f "/system/etc/permissions/privapp-permissions-sleepaudio.xml" ]; then
    echo "   ✓ Permission file exists"
    echo "   Content:"
    cat /system/etc/permissions/privapp-permissions-sleepaudio.xml | grep -v "^$"
else
    echo "   ✗ Permission file NOT found in /system/etc/permissions/"
fi

echo ""
echo "3. Checking APK installation..."
if [ -f "/system/priv-app/SleepAudio/SleepAudio.apk" ]; then
    echo "   ✓ APK found in system"
    pm list packages | grep sleepaudio
else
    echo "   ✗ APK NOT found"
fi

echo ""
echo "4. Checking package permissions..."
dumpsys package org.lineageos.sleepaudio | grep -A 5 "grantedPermissions\|requested permissions"

echo ""
echo "5. Checking SELinux context..."
ls -Z /system/etc/permissions/privapp-permissions-sleepaudio.xml 2>/dev/null

echo ""
echo "=== End Diagnostics ==="
