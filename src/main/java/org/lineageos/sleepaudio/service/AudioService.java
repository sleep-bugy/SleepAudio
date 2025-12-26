package org.lineageos.sleepaudio.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.DynamicsProcessing.Config;
import android.media.audiofx.DynamicsProcessing.Eq;
import android.media.audiofx.DynamicsProcessing.EqBand;
import android.media.audiofx.DynamicsProcessing.Mbc;
import android.media.audiofx.DynamicsProcessing.MbcBand;
import android.media.audiofx.DynamicsProcessing.Limiter;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;
import org.lineageos.sleepaudio.R;
import org.lineageos.sleepaudio.utils.Constants;

import android.media.audiofx.Virtualizer;

public class AudioService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int GLOBAL_SESSION_ID = 0;
    
    private SharedPreferences mPrefs;
    private DynamicsProcessing mDynamicsProcessing; // Renamed for clarity
    private Virtualizer mVirtualizer; // Standard Virtualizer
    private boolean mIsRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsRunning) {
            startForeground(1, createNotification());
            mIsRunning = true;
        }
        // Delayed init to let other heavy audio mods (Viper/Dolby) settle first
        new android.os.Handler().postDelayed(this::initEngine, 2000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        releaseEffects();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                Constants.CHANNEL_ID,
                "SleepAudio Engine",
                NotificationManager.IMPORTANCE_LOW
        );
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification createNotification() {
        return new Notification.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle("SleepAudio Active")
                .setContentText("Enhancing audio stream")
                .setSmallIcon(R.drawable.ic_qs_tile)
                .build();
    }

    private synchronized void initEngine() {
        if (!mPrefs.getBoolean(Constants.KEY_ENABLE, false)) {
            releaseEffects();
            return;
        }

        try {
            // 1. DynamicsProcessing (Bass, EQ, Limiter)
            if (mDynamicsProcessing == null) {
                Config.Builder builder = new Config.Builder(
                        Config.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2, true, true, true, true
                );
                builder.setPreferredFrameDuration(10.0f);
                builder.setMbcBandCount(2); 
                builder.setPostEqBandCount(10);
                
                // Priority 0 to minimize conflict with Viper (usually high priority)
                mDynamicsProcessing = new DynamicsProcessing(0, GLOBAL_SESSION_ID, builder.build());
                mDynamicsProcessing.setEnabled(true);
            }

            // 2. Virtualizer (Surround)
            if (mVirtualizer == null) {
                // Try creating Virtualizer safe-guarded
                try {
                    mVirtualizer = new Virtualizer(0, GLOBAL_SESSION_ID);
                    mVirtualizer.setEnabled(true);
                } catch (Exception e) {
                    Log.w(Constants.TAG, "Virtualizer not supported or conflicted: " + e.getMessage());
                }
            }
            
            Log.i(Constants.TAG, "SleepAudio Engine Started");
            applyAllSettings();
            
        } catch (Exception e) {
            Log.e(Constants.TAG, "Critical: Engine start failed", e);
            releaseEffects(); // Fail safe
        }
    }

    private void applyAllSettings() {
        applyDynamicsProcessing();
        applyVirtualizer();
    }
    
    private void applyVirtualizer() {
        if (mVirtualizer == null) return;
        boolean virtEnabled = mPrefs.getBoolean(Constants.KEY_VIRTUALIZER_ENABLE, false);
        // int strength = mPrefs.getInt(Constants.KEY_VIRTUALIZER_STRENGTH, 50);
        
        if (virtEnabled) {
             // Force strongest setting for now, or scaled
             // Standard Virtualizer usually 0-1000
             try {
                if (mVirtualizer.getStrengthSupported()) {
                    mVirtualizer.setStrength((short) 1000); 
                }
             } catch (IllegalStateException e) {
                 // Ignore if released
             }
        } else {
             try {
                if (mVirtualizer.getStrengthSupported()) {
                    mVirtualizer.setStrength((short) 0);
                }
             } catch (IllegalStateException e) {}
        }
    }

    private void applyDynamicsProcessing() {
        if (mDynamicsProcessing == null) return;
        
        // 1. Bass Enhancement (MBC Band 0)
        boolean bassEnabled = mPrefs.getBoolean(Constants.KEY_BASS_ENABLE, false);
        // int bassStrength = mPrefs.getInt(Constants.KEY_BASS_STRENGTH, 50); // Future usage
        
        Mbc mbc = mDynamicsProcessing.getMbc();
        if (mbc != null) {
            mbc.setEnabled(true);
            
            // Band 0: Bass (< 150Hz)
            MbcBand bassBand = mbc.getBand(0);
            bassBand.setCutoffFrequency(150.0f);
            
            if (bassEnabled) {
                // Compressor logic for punchy bass
                bassBand.setAttackTime(10.0f);
                bassBand.setReleaseTime(100.0f);
                bassBand.setRatio(4.0f); // Compression ratio
                bassBand.setThreshold(-20.0f);
                bassBand.setKneeWidth(6.0f);
                bassBand.setPostGain(6.0f); // 6dB Boost
            } else {
                bassBand.setPostGain(0.0f); // Flat
            }
            mbc.setBand(0, bassBand);
            
            // Band 1: Rest of spectrum (Flat)
            MbcBand highBand = mbc.getBand(1);
            highBand.setCutoffFrequency(20000.0f);
            highBand.setPostGain(0.0f);
            mbc.setBand(1, highBand);
            
            mDynamicsProcessing.setMbc(mbc);
        }

        // 2. Volume Leveler (Limiter)
        boolean volLeveler = mPrefs.getBoolean(Constants.KEY_VOLUME_LEVELER, false);
        Limiter limiter = mDynamicsProcessing.getLimiter();
        if (limiter != null) {
            boolean isHiFi = mPrefs.getInt(Constants.KEY_PROFILE, 0) == Constants.PROFILE_HIFI;
            // Force disable limiter in Hi-Fi mode
            if (isHiFi) volLeveler = false;
            
            limiter.setEnabled(volLeveler);
            if (volLeveler) {
                limiter.setAttackTime(1.0f);
                limiter.setReleaseTime(60.0f);
                limiter.setThreshold(-2.0f); // Limit near 0dB
                limiter.setRatio(10.0f);     // Hard limit
                limiter.setPostGain(2.0f);   // Makeup gain
            }
            mDynamicsProcessing.setLimiter(limiter);
        }
        
        // 3. Dialogue & EQ
        applyProfileTone();
    }
    
    private void applyProfileTone() {
        if (mDynamicsProcessing == null || mDynamicsProcessing.getPostEq() == null) return;
        
        int profile = mPrefs.getInt(Constants.KEY_PROFILE, Constants.PROFILE_DYNAMIC);
        boolean dialogue = mPrefs.getBoolean(Constants.KEY_DIALOGUE_ENABLE, false);
        
        Eq postEq = mDynamicsProcessing.getPostEq();
        postEq.setEnabled(true);
        int bandCount = postEq.getBandCount(); // Should be 10
        
        // Default Flat
        float[] gains = new float[bandCount]; 
        
        // Logic mimicking Dolby's tone profiles
        if (profile == Constants.PROFILE_MUSIC) {
            // V-Shape: Boost Lows and Highs
            gains[0] = 3.0f; gains[1] = 2.0f; gains[8] = 2.0f; gains[9] = 3.0f;
        } else if (profile == Constants.PROFILE_GAME) {
            // Footsteps: Boost Mid-Highs (2k-4k)
            gains[5] = 4.0f; gains[6] = 5.0f; gains[7] = 4.0f;
        } else if (profile == Constants.PROFILE_MOVIE) {
            // Cinematic: Boost Sub-bass and Treble for air
            gains[0] = 5.0f; gains[9] = 2.0f;
        } else if (profile == Constants.PROFILE_HIFI) {
             // Hi-Fi Logic: Pure flat with tiny air
             gains[9] = 2.0f; 
        }
        
        if (dialogue) {
            // Speech Range: 1k - 3k (Bands 4, 5, 6 roughly if 10 bands log scale)
            gains[4] += 3.0f;
            gains[5] += 4.0f;
            gains[6] += 3.0f;
        }

        // Apply to bands
        for (int i = 0; i < bandCount; i++) {
            EqBand band = postEq.getBand(i);
            // Rough Logarithmic freq mapping for 10 bands
            // 32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
            float freq = 32.0f * (float)Math.pow(2, i);
            band.setCutoffFrequency(freq);
            // Safety clamp
            if (gains[i] > 10.0f) gains[i] = 10.0f;
            if (gains[i] < -10.0f) gains[i] = -10.0f;
            
            band.setGain(gains[i]);
            postEq.setBand(i, band);
        }
        mDynamicsProcessing.setPostEq(postEq);
    }
    
    private void applyBassBoost(boolean deep) {
        // Legacy stub
    }
    
    private void applyHiFi() {
        // Handled in applyProfileTone and applyDynamicsProcessing
    }
    
    private void applyWarmFilter() {
        // Stub
    }
    
    private void applyFootstepEnhancer() {
         // Stub
    }
    
    private void applyTrebleClarity() {
        // Stub
    }

    private void resetBands() {
         // Stub
    }

    private void releaseEffects() {
        if (mDynamicsProcessing != null) {
            mDynamicsProcessing.release();
            mDynamicsProcessing = null;
        }
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Constants.KEY_ENABLE.equals(key)) {
            initEngine(); 
        } else {
            applyAllSettings();
        }
    }
}
