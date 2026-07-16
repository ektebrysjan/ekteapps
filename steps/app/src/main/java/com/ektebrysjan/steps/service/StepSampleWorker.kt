package com.ektebrysjan.steps.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ektebrysjan.steps.data.StepRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Periodic background sample of the hardware step counter.
 *
 * The TYPE_STEP_COUNTER sensor keeps accumulating in hardware even while the app isn't running, so
 * we don't need a foreground service (and its mandatory persistent notification) to keep counting.
 * WorkManager wakes us roughly every 15 minutes to read the running total and let the repository
 * attribute the delta to the correct day. WorkManager persists its jobs across reboots, so counting
 * resumes on its own without a boot receiver.
 */
class StepSampleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sensorManager =
            applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: return Result.success() // No sensor on this device — nothing to sample.

        val rawValue = readCounterOnce(sensorManager, sensor)
        if (rawValue != null) {
            StepRepository.get(applicationContext).recordCounterValue(rawValue)
        }
        // Always succeed: a missed sample is corrected by the next run, whose delta covers the gap.
        return Result.success()
    }

    /** Register briefly, wait for the first reading, then unregister. Null on timeout/no reading. */
    private suspend fun readCounterOnce(sm: SensorManager, sensor: Sensor): Long? =
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        sm.unregisterListener(this)
                        val value = event?.values?.firstOrNull()?.toLong()
                        if (cont.isActive) cont.resume(value)
                    }

                    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) { /* no-op */ }
                }
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            }
        }

    companion object {
        private const val READ_TIMEOUT_MS = 10_000L
        private const val UNIQUE_NAME = "step_sampling"
        private const val INTERVAL_MINUTES = 15L

        /** Schedule the recurring background sample (safe to call repeatedly). */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSampleWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
