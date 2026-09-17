package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/** App-scoped generation job so work survives leaving the generator screen. */
object GenerationSession {
    enum class Kind { Image, Video, Music, Cover }

    enum class Phase { Idle, Running, Success, Error }

    /** Soft ceiling while waiting for the API — never freeze on a round % like 92. */
    private const val RUNNING_SOFT_CAP = 0.98f

    var kind by mutableStateOf(Kind.Image)
        private set
    var phase by mutableStateOf(Phase.Idle)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    fun start(kind: Kind, block: suspend () -> Unit) {
        this.kind = kind
        phase = Phase.Running
        progress = 0.02f
        errorMessage = null
        job?.cancel()
        job =
            scope.launch {
                val ticker =
                    launch {
                        while (isActive && phase == Phase.Running) {
                            delay(200)
                            // Ease toward the soft cap so the bar keeps moving until real completion.
                            val remaining = RUNNING_SOFT_CAP - progress
                            if (remaining > 0.0015f) {
                                val step = max(0.003f, remaining * 0.07f)
                                progress = (progress + step).coerceAtMost(RUNNING_SOFT_CAP)
                            }
                        }
                    }
                try {
                    block()
                    // 100% only after a real successful result from [block].
                    progress = 1f
                    phase = Phase.Success
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Generation failed"
                    phase = Phase.Error
                } finally {
                    ticker.cancel()
                }
            }
    }

    fun reset() {
        job?.cancel()
        job = null
        phase = Phase.Idle
        progress = 0f
        errorMessage = null
    }
}
