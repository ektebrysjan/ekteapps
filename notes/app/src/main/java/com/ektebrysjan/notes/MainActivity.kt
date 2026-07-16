package com.ektebrysjan.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ektebrysjan.notes.ui.NotesApp
import com.ektebrysjan.notes.ui.NotesViewModel
import com.ektebrysjan.notes.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesTheme {
                NotesApp(viewModel = viewModel)
            }
        }
    }
}
