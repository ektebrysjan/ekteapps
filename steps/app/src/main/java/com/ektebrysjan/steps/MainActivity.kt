package com.ektebrysjan.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ektebrysjan.steps.data.StepRepository
import com.ektebrysjan.steps.service.StepSampleWorker
import com.ektebrysjan.steps.ui.AppScaffold
import com.ektebrysjan.steps.ui.StepsViewModel
import com.ektebrysjan.steps.ui.theme.StepsTheme
import kotlinx.coroutines.launch

/**
 * Hosts the UI, samples the step counter live while the app is on screen, and schedules the
 * background WorkManager sampler. There is no foreground service and no notification — the hardware
 * counter accumulates on its own while the app is closed, and [StepSampleWorker] reconciles it.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: StepsViewModel by viewModels()
    private val repository by lazy { StepRepository.get(this) }
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // If activity-recognition was granted (or wasn't required), start counting.
        val activityGranted = results[Manifest.permission.ACTIVITY_RECOGNITION] ?: true
        if (activityGranted || !needsActivityRecognition()) {
            startCounting()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        setContent {
            StepsTheme {
                AppScaffold(viewModel = viewModel)
            }
        }

        requestPermissionThenStart()
    }

    override fun onResume() {
        super.onResume()
        // Re-attach the live listener when we come back to the foreground (if allowed).
        if (canCount()) registerForegroundUpdates()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun requestPermissionThenStart() {
        if (needsActivityRecognition() &&
            !isGranted(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
        } else {
            startCounting()
        }
    }

    /** Kick off both the background sampler and the live foreground listener. */
    private fun startCounting() {
        StepSampleWorker.enqueue(this)
        registerForegroundUpdates()
    }

    private fun registerForegroundUpdates() {
        val sensor = stepSensor ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val rawValue = event.values.firstOrNull()?.toLong() ?: return
        lifecycleScope.launch { repository.recordCounterValue(rawValue) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    private fun canCount(): Boolean =
        !needsActivityRecognition() || isGranted(Manifest.permission.ACTIVITY_RECOGNITION)

    private fun needsActivityRecognition(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
