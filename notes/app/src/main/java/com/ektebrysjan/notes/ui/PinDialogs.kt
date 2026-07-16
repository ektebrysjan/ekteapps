package com.ektebrysjan.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.ektebrysjan.notes.R
import com.ektebrysjan.notes.util.PinManager

/** Enter + confirm a new PIN. Calls [onConfirm] with the validated PIN. */
@Composable
fun SetPinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_pin)) },
        text = {
            Column {
                PinField(value = pin, onValueChange = { pin = it; error = null },
                    label = stringResource(R.string.enter_pin))
                PinField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = stringResource(R.string.confirm_pin),
                    modifier = Modifier.padding(top = 8.dp)
                )
                error?.let {
                    Text(
                        text = stringResource(it),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < PinManager.MIN_PIN_LENGTH -> error = R.string.pin_too_short
                    pin != confirm -> error = R.string.pin_mismatch
                    else -> onConfirm(pin)
                }
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Prompt for an existing PIN. [onVerify] returns true if correct; the dialog shows an error and
 * stays open on false, and closes on true.
 */
@Composable
fun EnterPinDialog(
    title: String,
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PinField(value = pin, onValueChange = { pin = it; error = false },
                    label = stringResource(R.string.enter_pin))
                if (error) {
                    Text(
                        text = stringResource(R.string.pin_wrong),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (onVerify(pin)) onDismiss() else { error = true; pin = "" }
            }) { Text(stringResource(R.string.unlock)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(8)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier
    )
}
