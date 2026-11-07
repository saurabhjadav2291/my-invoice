@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.PaymentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private const val DAY_MILLIS = 86_400_000L

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(42.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color(0x22B4643C), spotColor = Color(0x33B4643C))
                    .background(Color.White, CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 4.dp, top = 18.dp, bottom = 4.dp),
    )
}

private val FieldShape = RoundedCornerShape(14.dp)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
    errorContainerColor = Color.White,
    focusedBorderColor = Palette.Orange,
    unfocusedBorderColor = Palette.FieldBorder,
    focusedLabelColor = Palette.Orange,
    cursorColor = Palette.Orange,
)

/** White rounded box that looks like a text field but opens a picker. */
@Composable
private fun PickerButton(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = FieldShape,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, Palette.FieldBorder),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = supportingText?.let { { Text(it) } },
        shape = FieldShape,
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A button that opens a menu of choices. */
@Composable
fun <T> DropdownField(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Box {
            PickerButton(onClick = { open = true }) {
                Text(
                    selected?.let(optionLabel) ?: placeholder,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        PickerButton(onClick = { open = true }) {
            Text(InvoiceDates.format(date), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays() * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays(it / DAY_MILLIS)) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun StatusChip(status: PaymentStatus) {
    val (background, content) = when (status) {
        PaymentStatus.PAID -> Color(0xFFDDF5E5) to Color(0xFF1B7A3D)
        PaymentStatus.OVERDUE -> Color(0xFFFFE1DD) to Color(0xFFC62A1F)
        PaymentStatus.PARTLY_PAID -> Color(0xFFFFE8D9) to Color(0xFFB34200)
        PaymentStatus.SENT -> Color(0xFFE6EDFD) to Color(0xFF2753A8)
        PaymentStatus.DRAFT, PaymentStatus.VOID -> Color(0xFFF0EEF0) to Color(0xFF5E606E)
    }
    Surface(color = background, shape = CircleShape) {
        Text(
            status.label,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Quantity as typed ("1.5") to thousandths (1500). Null if not a number. */
fun parseQuantityMilli(text: String): Long? {
    val cleaned = text.trim()
    if (cleaned.isEmpty()) return null
    val parts = cleaned.split('.')
    if (parts.size > 2 || parts.any { p -> p.any { !it.isDigit() } }) return null
    val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
    val fraction = parts.getOrNull(1) ?: ""
    if (fraction.length > 3) return null
    return whole * 1000 + fraction.padEnd(3, '0').toLong()
}

fun formatQuantity(milli: Long): String {
    val whole = milli / 1000
    val fraction = (milli % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
}

/** Minor units back to an editable string: 1200050 -> "12000.50", 1200000 -> "12000". */
fun minorToInput(minor: Long): String {
    val major = minor / 100
    val cents = minor % 100
    return if (cents == 0L) major.toString() else "$major.${cents.toString().padStart(2, '0')}"
}
