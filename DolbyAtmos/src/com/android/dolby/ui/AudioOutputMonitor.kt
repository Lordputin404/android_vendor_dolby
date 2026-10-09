/*
 * Copyright (C) 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class AudioOutputMonitor(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)!!

    fun isOnSpeaker(): Boolean =
        audioManager.getDevicesForAttributes(MEDIA_ATTRS).firstOrNull()?.type ==
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

    val onSpeaker: Flow<Boolean> =
        callbackFlow {
            val callback =
                object : AudioDeviceCallback() {
                    override fun onAudioDevicesAdded(devices: Array<AudioDeviceInfo>) {
                        trySend(isOnSpeaker())
                    }

                    override fun onAudioDevicesRemoved(devices: Array<AudioDeviceInfo>) {
                        trySend(isOnSpeaker())
                    }
                }
            audioManager.registerAudioDeviceCallback(callback, null)
            trySend(isOnSpeaker())
            awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
        }
        .distinctUntilChanged()

    private companion object {
        val MEDIA_ATTRS: AudioAttributes =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
    }
}
