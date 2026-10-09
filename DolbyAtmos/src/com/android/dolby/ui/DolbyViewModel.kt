/*
 * Copyright (C) 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dolby.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dolby.DolbyController
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DolbyViewModel"

internal class DolbyViewModel(
    private val controller: DolbyController,
    audioOutput: AudioOutputMonitor,
    val stereoSupported: Boolean,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val worker: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val pendingWrites = AtomicInteger(0)

    private val settings = MutableStateFlow(readSettings())

    val ui: StateFlow<DolbyUiState> =
        combine(settings, audioOutput.onSpeaker) { s, onSpeaker -> s.copy(isOnSpeaker = onSpeaker) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = settings.value.copy(isOnSpeaker = audioOutput.isOnSpeaker()),
            )

    fun refresh() {
        viewModelScope.launch(worker) {
            if (pendingWrites.get() == 0) settings.value = readSettings()
        }
    }

    fun setEnabled(v: Boolean) = change({ it.copy(dsOn = v) }) { setDsOnAndPersist(v) }
    
    fun setProfile(v: Int) {
        if (v == settings.value.profile) return
        change({ it.copy(profile = v) }) { profile = v }
    }
    
    fun setIeq(v: Int) = change({ it.copy(ieq = v) }) { setIeqPreset(v) }
    fun setDialogue(v: Int) = change({ it.copy(dialogue = v) }) { setDialogueEnhancerAmount(v) }
    fun setStereo(v: Int) = change({ it.copy(stereo = v) }) { setStereoWideningAmount(v) }
    fun setHpVirt(v: Boolean) = change({ it.copy(hpVirt = v) }) { setHeadphoneVirtEnabled(v) }
    fun setSpkVirt(v: Boolean) = change({ it.copy(spkVirt = v) }) { setSpeakerVirtEnabled(v) }
    fun setBass(v: Boolean) = change({ it.copy(bass = v) }) { setBassEnhancerEnabled(v) }
    fun setVolume(v: Boolean) = change({ it.copy(volume = v) }) { setVolumeLevelerEnabled(v) }
    fun reset() = change { resetProfileSpecificSettings() }

    private fun change(
        optimistic: (DolbyUiState) -> DolbyUiState = { it },
        write: DolbyController.() -> Unit,
    ) {
        settings.update(optimistic)
        pendingWrites.incrementAndGet()
        viewModelScope.launch(worker) {
            try {
                controller.write()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply Dolby setting", e)
            } finally {
                if (pendingWrites.decrementAndGet() == 0) {
                    delay(150)
                    if (pendingWrites.get() == 0) settings.value = readSettings()
                }
            }
        }
    }

    private fun readSettings(): DolbyUiState {
        val p = controller.profile
        return DolbyUiState(
            dsOn = controller.dsOn,
            profile = p,
            ieq = controller.getIeqPreset(p),
            dialogue = controller.getDialogueEnhancerAmount(p),
            stereo = controller.getStereoWideningAmount(p),
            hpVirt = controller.getHeadphoneVirtEnabled(p),
            spkVirt = controller.getSpeakerVirtEnabled(p),
            bass = controller.getBassEnhancerEnabled(p),
            volume = controller.getVolumeLevelerEnabled(p),
            presetName = controller.getPresetName(),
        )
    }
}
