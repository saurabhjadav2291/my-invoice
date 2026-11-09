@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.PaymentStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private const val DAY_MILLIS = 86_400_000L

/**
 * Top bar of an inner screen: a round back button, a title, and actions on the right.
 */
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Ui.colors
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(AppIcons.Back, "Back", onBack)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Layout of an inner screen (details and forms). The content scrolls under a pinned
 * [topBar] and an optional [bottomBar]; both sit on a blur of the content
 * behind them (design.md section 5.5), so nothing is cut off by a hard edge.
 * [content] gets the padding it needs to clear both bars when scrolled to an end.
 */
@Composable
fun InnerScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable (HazeState) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val c = Ui.colors
    val density = LocalDensity.current
    val haze = rememberHazeState()
    var topHeight by remember { mutableStateOf(0.dp) }
    var bottomHeight by remember { mutableStateOf(0.dp) }
    val systemBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barTint = rememberBackgroundBrush(alpha = 0.97f)
    val barFallback = rememberBackgroundBrush()
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            content(PaddingValues(top = topHeight, bottom = maxOf(bottomHeight, systemBottom)))
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { topHeight = with(density) { it.height.toDp() } }
                .hazeEffect(haze) {
                    blurRadius = 20.dp
                    noiseFactor = 0f
                    // Near-solid background behind the title and back button so scrolled text never
                    // reads through them. The tint is the same sky gradient as the page behind
                    // (measured from the top of the screen), so the bar blends in; only its last
                    // fifth fades into the content.
                    tints = listOf(HazeTint(barTint))
                    fallbackTint = HazeTint(barFallback)
                    mask = Brush.verticalGradient(0.8f to Color.Black, 1f to Color.Transparent)
                },
        ) { topBar() }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { bottomHeight = with(density) { it.height.toDp() } },
        ) { bottomBar(haze) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = SectionHeader(text, modifier)

/** Label above a field, in grey caption text. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = Ui.colors.greyText,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    // The well itself is drawn by Modifier.fieldWell(); the field only adds the focus ring.
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedBorderColor = Ui.colors.primary,
    unfocusedBorderColor = Color.Transparent,
    focusedTextColor = Ui.colors.ink,
    unfocusedTextColor = Ui.colors.ink,
    cursorColor = Ui.colors.primary,
    errorBorderColor = Ui.colors.error,
)

/**
 * Text fields are soft wells: the segmented-tab key turned inside out, darker at the top
 * with the white rim along the bottom, 8 dp corners (design.md section 6.7).
 */
fun Modifier.fieldWell(): Modifier = softKey(FormShape, Color(0xFFE9ECF0), Color(0xFFF7F8FA), shadeAlpha = 0.08f)

/** Height of single-line fields and pickers (design.md section 6.7). */
val FieldHeight = 48.dp

/** Well with 8 dp corners that looks like a text field but opens a picker. */
@Composable
private fun PickerPill(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val c = Ui.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = FieldHeight)
            .fieldWell()
            .clip(FormShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
    placeholder: String? = null,
) {
    val c = Ui.colors
    val colors = fieldColors()
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        // BasicTextField with the outlined decoration, so the field can be 48 dp tall
        // (OutlinedTextField forces 56 dp) with 12 dp vertical padding.
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
            cursorBrush = SolidColor(c.primary),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().heightIn(min = FieldHeight).fieldWell(),
            decorationBox = { inner ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = inner,
                    enabled = true,
                    singleLine = singleLine,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interaction,
                    isError = isError,
                    placeholder = placeholder?.let { text -> @Composable { Text(text, style = MaterialTheme.typography.bodyLarge, color = c.grey, maxLines = 1) } },
                    colors = colors,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = isError,
                            interactionSource = interaction,
                            colors = colors,
                            shape = FormShape,
                            focusedBorderThickness = 2.dp,
                            unfocusedBorderThickness = 0.dp,
                        )
                    },
                )
            },
        )
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = Ui.colors.greyText,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            )
        }
    }
}

/** A pill that opens a menu of choices. */
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
    val c = Ui.colors
    Column(modifier) {
        FieldLabel(label)
        Box {
            PickerPill(onClick = { open = true }) {
                Text(
                    selected?.let(optionLabel) ?: placeholder,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected == null) c.grey else c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.greyText)
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(12.dp),
                containerColor = c.surface,
                border = BorderStroke(1.dp, c.hairline),
                shadowElevation = 8.dp,
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), color = c.ink) },
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
        FieldLabel(label)
        PickerPill(onClick = { open = true }) {
            Text(
                InvoiceDates.format(date),
                style = MaterialTheme.typography.bodyLarge,
                color = Ui.colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays() * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                AppButton("OK", variant = ButtonVariant.Link, onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays(it / DAY_MILLIS)) }
                    open = false
                })
            },
            dismissButton = { AppButton("Cancel", onClick = { open = false }, variant = ButtonVariant.Tertiary) },
            shape = RoundedCornerShape(20.dp),
            colors = DatePickerDefaults.colors(containerColor = Ui.colors.surface),
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.Transparent))
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Ui.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.ink)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.greyText)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.primary,
                checkedBorderColor = c.primary,
                uncheckedThumbColor = c.grey,
                uncheckedTrackColor = TrackColor,
                uncheckedBorderColor = c.ink.copy(alpha = 0.14f),
            ),
        )
    }
}

@Composable
fun statusColors(status: PaymentStatus): StatusColors {
    val c = Ui.colors
    return when (status) {
        PaymentStatus.PAID -> c.paid
        PaymentStatus.OVERDUE -> c.overdue
        PaymentStatus.PARTLY_PAID -> c.partlyPaid
        PaymentStatus.SENT -> c.sent
        PaymentStatus.DRAFT, PaymentStatus.VOID -> c.draft
    }
}

/** Soft tinted tag with a coloured dot (design.md section 2.3). */
@Composable
fun StatusChip(status: PaymentStatus) {
    val colors = statusColors(status)
    Row(
        Modifier
            .clip(TagShape)
            .background(colors.fill)
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(colors.content))
        Spacer(Modifier.width(4.dp))
        Text(status.label, color = colors.content, style = MaterialTheme.typography.labelMedium)
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
    val c = Ui.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge, color = c.greyText) },
        confirmButton = {
            AppButton(confirmLabel, variant = ButtonVariant.Link, tint = c.error, onClick = {
                onConfirm()
                onDismiss()
            })
        },
        dismissButton = { AppButton("Cancel", onClick = onDismiss, variant = ButtonVariant.Tertiary) },
        shape = RoundedCornerShape(20.dp),
        containerColor = c.surface,
    )
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ui.colors.ink)
        Text(message, color = Ui.colors.greyText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
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
