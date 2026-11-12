package io.github.dimitrysaf.asko.lib.share

import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

/** How many images Coil is loading right now, so a share image can wait for its pictures. */
object ImageLoads : EventListener() {
    private val pending = MutableStateFlow(0)
    val count: StateFlow<Int> = pending

    override fun onStart(request: ImageRequest) = pending.update { it + 1 }
    override fun onSuccess(request: ImageRequest, result: SuccessResult) = done()
    override fun onError(request: ImageRequest, result: ErrorResult) = done()
    override fun onCancel(request: ImageRequest) = done()

    private fun done() = pending.update { (it - 1).coerceAtLeast(0) }

    /** Until no image is loading, or [timeoutMs] at most (offline, a slow server). */
    suspend fun awaitIdle(timeoutMs: Long = 6_000) {
        withTimeoutOrNull(timeoutMs) { count.first { it == 0 } }
    }
}
