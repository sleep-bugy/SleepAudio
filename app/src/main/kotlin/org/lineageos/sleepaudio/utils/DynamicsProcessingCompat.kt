package org.lineageos.sleepaudio.utils

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.Limiter
import android.media.audiofx.DynamicsProcessing.Mbc
import android.media.audiofx.DynamicsProcessing.Eq
import android.util.Log

object DynamicsProcessingCompat {
    private const val TAG = "DynamicsProcessingCompat"

    // --- Limiter ---
    fun setLimiter(dp: DynamicsProcessing, channel: Int, limiter: Limiter) {
        try {
            val method = DynamicsProcessing::class.java.getMethod("setLimiter", Int::class.javaPrimitiveType, Limiter::class.java)
            method.invoke(dp, channel, limiter)
        } catch (e: Exception) {
            Log.w(TAG, "setLimiter reflection failed", e)
        }
    }

    fun getLimiter(dp: DynamicsProcessing, channel: Int): Limiter? {
        return try {
            val method = DynamicsProcessing::class.java.getMethod("getLimiter", Int::class.javaPrimitiveType)
            method.invoke(dp, channel) as? Limiter
        } catch (e: Exception) {
            Log.w(TAG, "getLimiter reflection failed", e)
            null
        }
    }

    // --- MBC ---
    fun setMbc(dp: DynamicsProcessing, channel: Int, mbc: Mbc) {
        try {
            val method = DynamicsProcessing::class.java.getMethod("setMbc", Int::class.javaPrimitiveType, Mbc::class.java)
            method.invoke(dp, channel, mbc)
        } catch (e: Exception) {
            Log.w(TAG, "setMbc reflection failed", e)
        }
    }

    fun getMbc(dp: DynamicsProcessing, channel: Int): Mbc? {
        return try {
            val method = DynamicsProcessing::class.java.getMethod("getMbc", Int::class.javaPrimitiveType)
            method.invoke(dp, channel) as? Mbc
        } catch (e: Exception) {
            Log.w(TAG, "getMbc reflection failed", e)
            null
        }
    }

    // --- PostEq ---
    fun setPostEq(dp: DynamicsProcessing, channel: Int, eq: Eq) {
        try {
            val method = DynamicsProcessing::class.java.getMethod("setPostEq", Int::class.javaPrimitiveType, Eq::class.java)
            method.invoke(dp, channel, eq)
        } catch (e: Exception) {
            Log.w(TAG, "setPostEq reflection failed", e)
        }
    }

    fun getPostEq(dp: DynamicsProcessing, channel: Int): Eq? {
        return try {
            val method = DynamicsProcessing::class.java.getMethod("getPostEq", Int::class.javaPrimitiveType)
            method.invoke(dp, channel) as? Eq
        } catch (e: Exception) {
            Log.w(TAG, "getPostEq reflection failed", e)
            null
        }
    }

    // --- PreEq ---
    fun setPreEq(dp: DynamicsProcessing, channel: Int, eq: Eq) {
        try {
            val method = DynamicsProcessing::class.java.getMethod("setPreEq", Int::class.javaPrimitiveType, Eq::class.java)
            method.invoke(dp, channel, eq)
        } catch (e: Exception) {
            Log.w(TAG, "setPreEq reflection failed", e)
        }
    }

    fun getPreEq(dp: DynamicsProcessing, channel: Int): Eq? {
        return try {
            val method = DynamicsProcessing::class.java.getMethod("getPreEq", Int::class.javaPrimitiveType)
            method.invoke(dp, channel) as? Eq
        } catch (e: Exception) {
            Log.w(TAG, "getPreEq reflection failed", e)
            null
        }
    }

    // --- Config.Builder ---
    // The SDK compilation often fails on the long constructor.
    // We try to find the constructor that matches the 9 arguments (variant, channels, preEqEnable, preEqBands, mbcEnable, mbcBands, postEqEnable, postEqBands, limiterEnable)
    // Args types: Int, Int, Boolean, Int, Boolean, Int, Boolean, Int, Boolean
    fun createConfig(
        variant: Int, channels: Int,
        preEqInUse: Boolean, preEqBandCount: Int,
        mbcInUse: Boolean, mbcBandCount: Int,
        postEqInUse: Boolean, postEqBandCount: Int,
        limiterInUse: Boolean
    ): DynamicsProcessing.Config? {
        return try {
            val clazz = DynamicsProcessing.Config.Builder::class.java
            // In Android 9+, this constructor exists.
            val constructor = clazz.getConstructor(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            val builder = constructor.newInstance(
                variant, channels,
                preEqInUse, preEqBandCount,
                mbcInUse, mbcBandCount,
                postEqInUse, postEqBandCount,
                limiterInUse
            ) as DynamicsProcessing.Config.Builder
            
            // Set preferred frame duration if possible
            try {
                val setDuration = clazz.getMethod("setPreferredFrameDuration", Float::class.javaPrimitiveType)
                setDuration.invoke(builder, 10.0f)
            } catch (e: Exception) { Log.d(TAG, "Optional setPreferredFrameDuration not found") }

            builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Config via reflection", e)
            null
        }
    }
}
