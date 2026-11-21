```kotlin
package com.kangrio.byd.assistant.service

import android.content.Context
import android.util.Log
import com.kangrio.byd.assistant.util.WakeWordModelManager
import com.suxsem.androidwakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class OpenWakeWordDetector(
    private val context: Context,
    private val audioSource: Int = android.media.MediaRecorder.AudioSource.DEFAULT,
    private val modelName: String = "hey_billy",
    private val sensitivity: Float = 0.5f,
    private val onDetected: () -> Unit
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var wakeWordEngine: WakeWordDetector? = null

    private val modelsPath = "openwakeword/models"

    private val modelFile: String
        get() = File(
            context.filesDir,
            "$modelsPath/$modelName.onnx"
        ).absolutePath

    init {
        loadModels()
    }

    fun loadModels() {
        WakeWordModelManager.ensureDefaultModels(context)
    }

    /**
     * Starts the selected wake-word model.
     * Returns false if the model cannot be initialized.
     */
    fun start(): Boolean {

        val model = File(modelFile)

        if (!model.exists()) {
            log("ERROR: Selected model not found: ${model.absolutePath}")
            return false
        }

        if (!model.isFile || model.length() <= 0L) {
            log("ERROR: Selected model is empty or invalid")
            return false
        }

        if (sensitivity !in 0.0f..1.0f) {
            log("ERROR: Invalid sensitivity: $sensitivity")
            return false
        }

        return try {

            log("Selected model: $modelName")
            log("Model path: ${model.absolutePath}")
            log("Model size: ${model.length()} bytes")
            log("Audio source: $audioSource")
            log("Sensitivity: $sensitivity")
            log("Initializing wake-word engine")

            val engine = WakeWordDetector(
                context = context,
                audioSource = audioSource,
                modelFile = modelFile,
                verifierFile = null,
                minScore = sensitivity,
                minVerifierScore = 0.1f,
                onDetected = { score ->

                    Log.i(
                        "MHK_WakeWord",
                        "WAKE WORD DETECTED: $modelName, score=$score"
                    )

                    try {
                        onDetected()
                    } catch (e: Exception) {
                        Log.e(
                            "MHK_WakeWord",
                            "Wake-word callback failed",
                            e
                        )
                    }
                }
            )

            wakeWordEngine = engine

            scope.launch {
                try {
                    log("Detection coroutine started: $modelName")

                    engine.startDetection()

                    log("Detection coroutine ended: $modelName")

                } catch (e: Exception) {

                    Log.e(
                        "MHK_WakeWord",
                        "Detection engine failed: $modelName",
                        e
                    )

                    if (wakeWordEngine === engine) {
                        wakeWordEngine = null
                    }

                    try {
                        engine.releaseResources()
                    } catch (releaseError: Exception) {
                        Log.e(
                            "MHK_WakeWord",
                            "Failed to release engine resources",
                            releaseError
                        )
                    }
                }
            }

            log("Wake-word engine launch requested")
            true

        } catch (e: Exception) {

            wakeWordEngine = null

            Log.e(
                "MHK_WakeWord",
                "Failed to initialize wake-word engine",
                e
            )

            false
        }
    }

    fun stop() {

        val engine = wakeWordEngine
        wakeWordEngine = null

        try {
            engine?.pauseDetection()
        } catch (e: Exception) {
            Log.e(
                "MHK_WakeWord",
                "Error pausing detector",
                e
            )
        }

        try {
            engine?.releaseResources()
        } catch (e: Exception) {
            Log.e(
                "MHK_WakeWord",
                "Error releasing detector resources",
                e
            )
        }

        scope.coroutineContext.cancel()

        log("Wake-word engine stopped")
    }

    fun pause() {
        try {
            wakeWordEngine?.pauseDetection()
            log("Wake-word engine paused")
        } catch (e: Exception) {
            Log.e(
                "MHK_WakeWord",
                "Error pausing detector",
                e
            )
        }
    }

    fun resume() {
        val engine = wakeWordEngine ?: run {
            log("Cannot resume: wake-word engine is not initialized")
            return
        }

        scope.launch {
            try {
                engine.startDetection()
                log("Wake-word engine resumed")
            } catch (e: Exception) {
                Log.e(
                    "MHK_WakeWord",
                    "Error resuming detector",
                    e
                )
            }
        }
    }

    private fun log(message: String) {
        Log.d("OpenWakeWordDetector", message)
    }
}
```
