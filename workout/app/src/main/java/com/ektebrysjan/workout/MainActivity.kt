package com.ektebrysjan.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ektebrysjan.workout.ui.WorkoutApp
import com.ektebrysjan.workout.ui.WorkoutViewModel
import com.ektebrysjan.workout.ui.theme.WorkoutTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutTheme {
                WorkoutApp(viewModel = viewModel)
            }
        }
    }
}
