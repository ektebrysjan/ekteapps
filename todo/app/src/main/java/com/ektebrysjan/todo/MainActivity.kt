package com.ektebrysjan.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ektebrysjan.todo.ui.TodoApp
import com.ektebrysjan.todo.ui.TodoViewModel
import com.ektebrysjan.todo.ui.theme.TodoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoTheme {
                TodoApp(viewModel = viewModel)
            }
        }
    }
}
