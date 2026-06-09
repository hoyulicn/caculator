package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.OfflineCurrencyRate
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@Composable
fun MainCalculatorApp(viewModel: CalculatorViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val history by viewModel.historyState.collectAsState()
    val currencyRates by viewModel.currencyRatesState.collectAsState()
    val currentView by viewModel.currentView.collectAsState()
    val isEditingShortcuts by viewModel.isEditingShortcuts.collectAsState()
    val syncInProgress by viewModel.syncInProgress.collectAsState()
    val syncStatusMsg by viewModel.syncStatusMessage.collectAsState()

    var showToolMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Handle toast messages for WebDAV operations gracefully
    LaunchedEffect(syncStatusMsg) {
        syncStatusMsg?.let { msg ->
            val toastText = when (msg) {
                "SUCCESS_CONN" -> Translations.get("conn_success", settings.language)
                "FAIL_CONN" -> Translations.get("conn_fail", settings.language)
                "SUCCESS_BACKUP" -> Translations.get("backup_success", settings.language)
                "SUCCESS_RESTORE" -> Translations.get("restore_success", settings.language)
                "WIPED_CLEAN" -> Translations.get("data_cleared", settings.language)
                else -> {
                    if (msg.startsWith("FAIL_CONN_ERR:") || msg.startsWith("FAIL_BACKUP:") || msg.startsWith("FAIL_RESTORE:")) {
                        val base = when {
                            msg.startsWith("FAIL_CONN_ERR:") -> Translations.get("conn_fail", settings.language)
                            msg.startsWith("FAIL_BACKUP:") -> Translations.get("backup_fail", settings.language)
                            else -> Translations.get("restore_fail", settings.language)
                        }
                        "$base ${msg.substringAfter(":")}"
                    } else {
                        msg
                    }
                }
            }
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            viewModel.syncStatusMessage.value = null // clear
        }
    }

    MyApplicationTheme(darkTheme = settings.isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Header Area with modern capsule selector button
                    TopUtilityHeader(
                        currentView = currentView,
                        language = settings.language,
                        isDarkMode = settings.isDarkMode,
                        onToggleTheme = { viewModel.toggleTheme(it) },
                        onToggleLang = { viewModel.toggleLanguage() },
                        onOpenSelector = { showToolMenu = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Content Block
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentView,
                            transitionSpec = {
                                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                            },
                            label = "screen_transition"
                        ) { viewKey ->
                            when (viewKey) {
                                "calc_sci" -> ScientificCalculatorScreen(viewModel, settings.language, history)
                                "currency" -> CurrencyConverterScreen(viewModel, settings.language, currencyRates)
                                "unit" -> UnitConverterScreen(viewModel, settings.language)
                                "date" -> DateCalculatorScreen(viewModel, settings.language)
                                "finance" -> FinancialCalculatorScreen(viewModel, settings.language)
                                "settings" -> SettingsAndSyncScreen(viewModel, settings)
                                else -> ScientificCalculatorScreen(viewModel, settings.language, history)
                            }
                        }
                    }
                }
            }

            // Beautiful Modal Tool Switcher Dialog triggered from the single button
            if (showToolMenu) {
                ToolSwitcherDialog(
                    currentView = currentView,
                    language = settings.language,
                    onDismiss = { showToolMenu = false },
                    onSelectView = { viewKey ->
                        viewModel.currentView.value = viewKey
                    }
                )
            }

            // Customizable shortcut bar setup dialog (kept for full compatibility)
            if (isEditingShortcuts) {
                CustomizeShortcutsDialog(
                    selectedShortcuts = viewModel.getPinnedShortcuts(),
                    language = settings.language,
                    onDismiss = { viewModel.isEditingShortcuts.value = false },
                    onSave = { keys ->
                        viewModel.saveShortcuts(keys)
                        viewModel.isEditingShortcuts.value = false
                    }
                )
            }
        }
    }
}

@Composable
fun TopUtilityHeader(
    currentView: String,
    language: String,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onToggleLang: () -> Unit,
    onOpenSelector: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_utility_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Switcher pill button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onOpenSelector() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("app_switcher_capsule"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = getIconForView(currentView),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = Translations.get(currentView, language),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Switch Utility",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        // Action controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Theme toggle
            IconButton(
                onClick = { onToggleTheme(!isDarkMode) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Language switch
            IconButton(
                onClick = onToggleLang,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Toggle Language",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ToolSwitcherDialog(
    currentView: String,
    language: String,
    onDismiss: () -> Unit,
    onSelectView: (String) -> Unit
) {
    val items = listOf("calc_sci", "currency", "unit", "date", "finance", "settings")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("tool_switcher_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Header block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == "zh") "选择智能工具" else "Select Utility Tool",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (language == "zh") "点击快速切换应用功能" else "Tap a tool to quickly switch views",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // List of items
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items) { key ->
                        val active = currentView == key
                        val title = Translations.get(key, language)
                        val icon = getIconForView(key)
                        val desc = when (key) {
                            "calc_sci" -> if (language == "zh") "科学函数与高阶精确代数计算" else "Advanced mathematical function engine"
                            "currency" -> if (language == "zh") "全球实时汇率转换与本地汇率编辑" else "Live monetary exchange rate converter"
                            "unit" -> if (language == "zh") "长度、重量、面积与体积复合单位换算" else "Comprehensive metric & imperial unit scales"
                            "date" -> if (language == "zh") "相隔天数推算与日期前推后加天数" else "Calendar interval calculation & offset math"
                            "finance" -> if (language == "zh") "网贷还款计算与理财投资增值复合计算" else "Mortgage interest schedules & compound growth"
                            "settings" -> if (language == "zh") "WebDAV云备份多设备加密同步及偏好设置" else "Secure cloud configuration & user options"
                            else -> ""
                        }

                        // Background and border highlighting
                        val containerColor = if (active) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                        val borderColor = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tool_select_card_$key"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, borderColor),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            onClick = {
                                onSelectView(key)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Rounded background capsule for icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Navigate",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- Layout Components -----------------

@Composable
fun HeaderRow(
    title: String,
    language: String,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onToggleLang: () -> Unit,
    onOpenShortcuts: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Translations.get("shortcut_tint", language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Lang Button
            IconButton(onClick = onToggleLang) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Language toggle",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            // Theme Switcher icon
            IconButton(onClick = { onToggleTheme(!isDarkMode) }) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme switcher",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            // Shortcuts Pinner gear icon
            IconButton(
                onClick = onOpenShortcuts,
                modifier = Modifier.testTag("shortcut_customizer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pin Shortcuts",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun ShortcutToolbar(
    viewModel: CalculatorViewModel,
    currentView: String,
    language: String,
    onSelectView: (String) -> Unit
) {
    val shortcuts = viewModel.getPinnedShortcuts()
    if (shortcuts.isEmpty()) return

    Column {
        Text(
            text = Translations.get("cust_shortcut", language),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shortcuts) { key ->
                val active = currentView == key
                val title = Translations.get(key, language)
                val icon = getIconForView(key)

                val backgroundColor = if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val textColor = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onSelectView(key) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("shortcut_pill_$key"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(16.dp),
                        tint = textColor
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    currentView: String,
    onViewSelect: (String) -> Unit,
    language: String
) {
    val items = listOf("calc_sci", "currency", "unit", "date", "finance", "settings")
    NavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        items.forEach { key ->
            val active = currentView == key
            NavigationBarItem(
                selected = active,
                onClick = { onViewSelect(key) },
                icon = {
                    Icon(
                        imageVector = if (active) getIconForView(key) else getIconForViewOutline(key),
                        contentDescription = Translations.get(key, language)
                    )
                },
                label = {
                    Text(
                        text = Translations.get(key, language),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("navbar_item_$key")
            )
        }
    }
}

fun getIconForView(key: String): ImageVector {
    return when (key) {
        "calc_sci" -> Icons.Default.Calculate
        "currency" -> Icons.Default.CurrencyExchange
        "unit" -> Icons.Default.SquareFoot
        "date" -> Icons.Default.CalendarMonth
        "finance" -> Icons.Default.TrendingUp
        "settings" -> Icons.Default.SettingsApplications
        else -> Icons.Default.Calculate
    }
}

fun getIconForViewOutline(key: String): ImageVector {
    return when (key) {
        "calc_sci" -> Icons.Outlined.Calculate
        "currency" -> Icons.Outlined.CurrencyExchange
        "unit" -> Icons.Outlined.SquareFoot
        "date" -> Icons.Outlined.CalendarMonth
        "finance" -> Icons.Outlined.TrendingUp
        "settings" -> Icons.Outlined.SettingsApplications
        else -> Icons.Outlined.Calculate
    }
}

// ----------------- CUSTOMIZE SHORTCUTS DIALOG -----------------

@Composable
fun CustomizeShortcutsDialog(
    selectedShortcuts: List<String>,
    language: String,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val allOptions = listOf("calc_sci", "currency", "unit", "date", "finance")
    val selectedState = remember { mutableStateMapOf<String, Boolean>().apply {
        allOptions.forEach { key ->
            put(key, selectedShortcuts.contains(key))
        }
    }}

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Translations.get("cust_shortcut", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = Translations.get("drag_drop_tips", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                allOptions.forEach { key ->
                    val checked = selectedState[key] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedState[key] = !checked }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = getIconForView(key), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(text = Translations.get(key, language), style = MaterialTheme.typography.bodyMedium)
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { selectedState[key] = it },
                            modifier = Modifier.testTag("checkbox_$key")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = Translations.get("cancel", language))
                    }
                    Button(
                        onClick = {
                            val nextList = allOptions.filter { selectedState[it] == true }
                            onSave(nextList)
                        },
                        modifier = Modifier.testTag("shortcuts_save_btn")
                    ) {
                        Text(text = Translations.get("save", language))
                    }
                }
            }
        }
    }
}


// ----------------- SCIENTIFIC CALCULATOR SCREEN -----------------

@Composable
fun ScientificCalculatorScreen(
    viewModel: CalculatorViewModel,
    language: String,
    history: List<com.example.data.CalcHistory>
) {
    val expression by viewModel.calcExpression.collectAsState()
    val result by viewModel.calcResult.collectAsState()
    var showHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scientific_calc_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Upper Display Card with a modern gradient
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAD",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifEmpty { Translations.get("click_btn_to_calc", language) },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (expression.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.ifEmpty { "0" },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("calc_result_text")
                    )
                }
            }
        }

        // Action Keyboard
        Box(modifier = Modifier.weight(2.7f)) {
            val buttons = listOf(
                listOf("sin", "cos", "tan", "e", "^", "π"),
                listOf("log", "ln", "√", "(", ")", "C"),
                listOf("7", "8", "9", "÷", "⌫"),
                listOf("4", "5", "6", "×", "="),
                listOf("1", "2", "3", "-", ""),
                listOf("0", ".", "+", "", "")
            )

            // Dynamic grid layout for keypad
            val context = LocalContext.current
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Flatten the lists but keeping positions optimized
                val flatButtons = listOf(
                    "sin", "cos", "tan", "^", "π",
                    "log", "ln", "√", "(", ")",
                    "7", "8", "9", "÷", "C",
                    "4", "5", "6", "×", "⌫",
                    "1", "2", "3", "-", "=",
                    "0", ".", "+", "e", ""
                )

                items(flatButtons) { text ->
                    if (text.isEmpty()) {
                        Spacer(modifier = Modifier.size(1.dp))
                    } else {
                        val isOperator = text in listOf("÷", "×", "-", "+", "=", "^")
                        val isScientific = text in listOf("sin", "cos", "tan", "log", "ln", "√", "π", "e", "(", ")")
                        val isClear = text in listOf("C", "⌫")

                        val containerColor = when {
                            text == "=" -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isClear -> MaterialTheme.colorScheme.errorContainer
                            isScientific -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val contentColor = when {
                            text == "=" -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                            isClear -> MaterialTheme.colorScheme.onErrorContainer
                            isScientific -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1.15f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(containerColor)
                                .clickable { viewModel.appendToExpression(text) }
                                .padding(2.dp)
                                .testTag("btn_$text"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (text.length > 2) 13.sp else 16.sp,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        Dialog(onDismissRequest = { showHistoryDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translations.get("history", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_history_btn")
                        ) {
                            Text(text = Translations.get("clear_history", language), color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Divider()

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Translations.get("no_history", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(history) { h ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.runFormula(h.expression)
                                            showHistoryDialog = false
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = h.expression,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "= ${h.result}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                    TextButton(onClick = { showHistoryDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }
}


// ----------------- CURRENCY CONVERTER SCREEN -----------------

@Composable
fun CurrencyConverterScreen(
    viewModel: CalculatorViewModel,
    language: String,
    rates: List<OfflineCurrencyRate>
) {
    val amount by viewModel.currencyAmount.collectAsState()
    val fromCur by viewModel.currencyFromSelected.collectAsState()
    val toCur by viewModel.currencyToSelected.collectAsState()
    val result by viewModel.currencyConvertedResult.collectAsState()

    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }
    var editingRate by remember { mutableStateOf<OfflineCurrencyRate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("currency_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Warning about Offline Mode
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = Translations.get("offline_warn", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Amount Input Field
        OutlinedTextField(
            value = amount,
            onValueChange = { viewModel.updateCurrencyAmount(it) },
            label = { Text(text = Translations.get("amount", language)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("currency_amount_input"),
            singleLine = true
        )

        // Selectors Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // From Currency Select Box
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showFromDropdown = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_from_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "$fromCur ▾",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(expanded = showFromDropdown, onDismissRequest = { showFromDropdown = false }) {
                    rates.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(text = "${r.currencyCode} (${if (language == "zh") r.displayNameZh else r.displayNameEn})") },
                            onClick = {
                                viewModel.updateCurrencySelection(r.currencyCode, toCur)
                                showFromDropdown = false
                            }
                        )
                    }
                }
            }

            // Swap icon button
            IconButton(
                onClick = { viewModel.updateCurrencySelection(toCur, fromCur) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap")
            }

            // To Currency Select Box
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showToDropdown = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_to_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "$toCur ▾",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(expanded = showToDropdown, onDismissRequest = { showToDropdown = false }) {
                    rates.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(text = "${r.currencyCode} (${if (language == "zh") r.displayNameZh else r.displayNameEn})") },
                            onClick = {
                                viewModel.updateCurrencySelection(fromCur, r.currencyCode)
                                showToDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${Translations.get("to", language)}: $toCur",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("currency_result_text")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Customizable exchange list items
        Text(
            text = Translations.get("edit_rate", language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        rates.forEach { rate ->
            val label = if (language == "zh") rate.displayNameZh else rate.displayNameEn
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingRate = rate }
                    .padding(vertical = 2.dp)
                    .testTag("currency_rate_item_${rate.currencyCode}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rate.symbol,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(text = rate.currencyCode, fontWeight = FontWeight.Bold)
                            Text(text = label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1 USD = ${String.format(Locale.US, "%.4f", 1.0 / rate.rateToUSD)} ${rate.currencyCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Edit manual",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    // Rate Custom edit modal
    editingRate?.let { rate ->
        var tempValue by remember { mutableStateOf(rate.rateToUSD.toString()) }
        Dialog(onDismissRequest = { editingRate = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${Translations.get("edit_rate", language)} - ${rate.currencyCode}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Translations.get("rate_to_usd", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_rate_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingRate = null }) {
                            Text(text = Translations.get("cancel", language))
                        }
                        Button(
                            onClick = {
                                val d = tempValue.toDoubleOrNull() ?: rate.rateToUSD
                                viewModel.modifyCurrencyRate(rate.currencyCode, d)
                                editingRate = null
                            },
                            modifier = Modifier.testTag("custom_rate_save")
                        ) {
                            Text(text = Translations.get("save", language))
                        }
                    }
                }
            }
        }
    }
}

// ----------------- UNIT CONVERTER SCREEN -----------------

@Composable
fun UnitConverterScreen(
    viewModel: CalculatorViewModel,
    language: String
) {
    val category by viewModel.unitCategory.collectAsState()
    val inputValue by viewModel.unitInputValue.collectAsState()
    val fromSel by viewModel.unitFromSelected.collectAsState()
    val toSel by viewModel.unitToSelected.collectAsState()
    val resultValue by viewModel.unitResultValue.collectAsState()

    val categories = listOf("length", "weight", "area", "volume")
    val unitsByCategory = mapOf(
        "length" to listOf("unit_m", "unit_cm", "unit_mm", "unit_km", "unit_inch", "unit_ft"),
        "weight" to listOf("unit_g", "unit_kg", "unit_oz", "unit_lb"),
        "area" to listOf("unit_m2", "unit_km2", "unit_hectare", "unit_acre"),
        "volume" to listOf("unit_l", "unit_ml", "unit_m3", "unit_gal")
    )

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("unit_converter_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Horizontal category selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = category == cat
                val label = Translations.get("unit_$cat", language)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { viewModel.updateUnitCategory(cat) }
                        .padding(vertical = 8.dp)
                        .testTag("unit_cat_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Value Input Row
        OutlinedTextField(
            value = inputValue,
            onValueChange = { viewModel.updateUnitValues(it, fromSel, toSel) },
            label = { Text(text = Translations.get("input_value", language)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("unit_amount_input"),
            singleLine = true
        )

        // Dropdown conversion selections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // From select
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { fromExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit_from_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = "${Translations.get(fromSel, language)} ▾",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                    unitsByCategory[category]?.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(text = Translations.get(u, language)) },
                            onClick = {
                                viewModel.updateUnitValues(inputValue, u, toSel)
                                fromExpanded = false
                            }
                        )
                    }
                }
            }

            // Swap icon
            IconButton(onClick = { viewModel.updateUnitValues(inputValue, toSel, fromSel) }) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap")
            }

            // To select
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { toExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit_to_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = "${Translations.get(toSel, language)} ▾",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                    unitsByCategory[category]?.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(text = Translations.get(u, language)) },
                            onClick = {
                                viewModel.updateUnitValues(inputValue, fromSel, u)
                                toExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Translations.get(toSel, language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = resultValue,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("unit_result_text")
                )
            }
        }
    }
}


// ----------------- DATE CALCULATOR SCREEN -----------------

@Composable
fun DateCalculatorScreen(
    viewModel: CalculatorViewModel,
    language: String
) {
    // Days Between
    val startDiff by viewModel.dateDiffStart.collectAsState()
    val endDiff by viewModel.dateDiffEnd.collectAsState()
    val diffResult by viewModel.dateDiffResult.collectAsState()

    // Date Offset
    val sourceOffset by viewModel.dateOffsetSource.collectAsState()
    val countOffset by viewModel.dateOffsetCount.collectAsState()
    val isAddOffset by viewModel.dateOffsetIsAdd.collectAsState()
    val offsetResult by viewModel.dateOffsetResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("date_calculator_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Days Difference
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Translations.get("date_diff", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = startDiff,
                    onValueChange = {
                        viewModel.dateDiffStart.value = it
                        viewModel.calculateDateDifference()
                    },
                    label = { Text(text = Translations.get("start_date", language)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_diff_start"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endDiff,
                    onValueChange = {
                        viewModel.dateDiffEnd.value = it
                        viewModel.calculateDateDifference()
                    },
                    label = { Text(text = Translations.get("end_date", language)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_diff_end"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translations.get("diff_result", language),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$diffResult ${Translations.get("days", language)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("date_diff_result")
                    )
                }
            }
        }

        // Card 2: Date Offset
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Translations.get("date_offset", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = sourceOffset,
                    onValueChange = {
                        viewModel.dateOffsetSource.value = it
                        viewModel.calculateDateOffset()
                    },
                    label = { Text(text = Translations.get("start_date", language)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_offset_source"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = countOffset,
                        onValueChange = {
                            viewModel.dateOffsetCount.value = it
                            viewModel.calculateDateOffset()
                        },
                        label = { Text(text = Translations.get("days", language)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("date_offset_count"),
                        singleLine = true
                    )

                    // Toggle Subtraction / Addition
                    Row(
                        modifier = Modifier
                            .weight(1.8f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                viewModel.dateOffsetIsAdd.value = !isAddOffset
                                viewModel.calculateDateOffset()
                            }
                            .padding(12.dp)
                            .testTag("date_offset_toggle_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isAddOffset) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                            contentDescription = "Toggle add sub",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAddOffset) Translations.get("add_days", language) else Translations.get("sub_days", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translations.get("result_date", language),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = offsetResult,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("date_offset_result")
                    )
                }
            }
        }
    }
}


// ----------------- FINANCIAL CALCULATOR SCREEN -----------------

@Composable
fun FinancialCalculatorScreen(
    viewModel: CalculatorViewModel,
    language: String
) {
    // Shared states
    var modeSec by remember { mutableStateOf("mortgage") } // mortgage or compound

    // Mortgage states
    val mPrincipal by viewModel.loanPrincipal.collectAsState()
    val mRate by viewModel.loanRate.collectAsState()
    val mYears by viewModel.loanYears.collectAsState()
    val mIsEqualPI by viewModel.loanIsEqualPI.collectAsState()
    val mPayDesc by viewModel.loanMonthlyPaymentDesc.collectAsState()
    val mTotalInt by viewModel.loanTotalInterest.collectAsState()
    val mTotalPay by viewModel.loanTotalPayment.collectAsState()

    // Compound states
    val cPrincipal by viewModel.compoundPrincipal.collectAsState()
    val cRate by viewModel.compoundRate.collectAsState()
    val cYears by viewModel.compoundYears.collectAsState()
    val cFreq by viewModel.compoundFreq.collectAsState()
    val cResultBal by viewModel.compoundResultBalance.collectAsState()
    val cResultInt by viewModel.compoundResultInterest.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("financial_calculator_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toggle tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (modeSec == "mortgage") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { modeSec = "mortgage" }
                    .padding(vertical = 10.dp)
                    .testTag("finance_tab_mortgage"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Translations.get("finance_mortgage", language).substringBefore("(").trim(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (modeSec == "mortgage") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (modeSec == "compound") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { modeSec = "compound" }
                    .padding(vertical = 10.dp)
                    .testTag("finance_tab_compound"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Translations.get("finance_compound", language).substringBefore("(").trim(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (modeSec == "compound") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (modeSec == "mortgage") {
            // Mortgage Forms
            OutlinedTextField(
                value = mPrincipal,
                onValueChange = {
                    viewModel.loanPrincipal.value = it
                    viewModel.calculateLoanPayment()
                },
                label = { Text(text = Translations.get("loan_amount", language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_amount_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = mRate,
                onValueChange = {
                    viewModel.loanRate.value = it
                    viewModel.calculateLoanPayment()
                },
                label = { Text(text = Translations.get("interest_rate", language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_rate_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = mYears,
                onValueChange = {
                    viewModel.loanYears.value = it
                    viewModel.calculateLoanPayment()
                },
                label = { Text(text = Translations.get("loan_years", language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_years_input"),
                singleLine = true
            )

            // Equal Principal vs Interest Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable {
                        viewModel.loanIsEqualPI.value = !mIsEqualPI
                        viewModel.calculateLoanPayment()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Translations.get("loan_type", language),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (mIsEqualPI) Translations.get("type_equal_pi", language) else Translations.get("type_equal_p", language),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("loan_type_text")
                )
            }

            // Results summary cards
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = Translations.get("monthly_pay", language), fontWeight = FontWeight.Bold)
                        Text(text = "$mPayDesc 元", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("monthly_payment_result"))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = Translations.get("total_interest", language))
                        Text(text = "$mTotalInt 元", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("total_interest_result"))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = Translations.get("total_payment", language), fontWeight = FontWeight.Bold)
                        Text(text = "$mTotalPay 元", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("total_payment_result"))
                    }
                }
            }
        } else {
            // Compound Interest Forms
            OutlinedTextField(
                value = cPrincipal,
                onValueChange = {
                    viewModel.compoundPrincipal.value = it
                    viewModel.calculateCompoundInterest()
                },
                label = { Text(text = Translations.get("init_principal", language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compound_principal_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = cRate,
                onValueChange = {
                    viewModel.compoundRate.value = it
                    viewModel.calculateCompoundInterest()
                },
                label = { Text(text = Translations.get("interest_rate", language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compound_rate_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = cYears,
                onValueChange = {
                    viewModel.compoundYears.value = it
                    viewModel.calculateCompoundInterest()
                },
                label = { Text(text = Translations.get("compound_years", language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compound_years_input"),
                singleLine = true
            )

            // Compounding Frequency selector row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = Translations.get("compound_freq", language),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                val options = listOf("annual", "quarterly", "monthly")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { opt ->
                        val optActive = cFreq == opt
                        val label = Translations.get("freq_$opt", language)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (optActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable {
                                    viewModel.compoundFreq.value = opt
                                    viewModel.calculateCompoundInterest()
                                }
                                .padding(vertical = 10.dp)
                                .testTag("compound_freq_$opt"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (optActive) FontWeight.Bold else FontWeight.Normal)
                            )
                        }
                    }
                }
            }

            // Results and yield cards
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = Translations.get("final_balance", language), fontWeight = FontWeight.Bold)
                        Text(text = "$cResultBal 元", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("compound_balance_result"))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = Translations.get("earned_interest", language))
                        Text(text = "$cResultInt 元", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("compound_interest_result"))
                    }
                }
            }
        }
    }
}


// ----------------- SETTINGS & WEBDAV SYNC SCREEN -----------------

@Composable
fun SettingsAndSyncScreen(
    viewModel: CalculatorViewModel,
    settings: com.example.data.AppSettings
) {
    val syncInProgress by viewModel.syncInProgress.collectAsState()

    var url by remember { mutableStateOf(settings.webDavUrl) }
    var user by remember { mutableStateOf(settings.webDavUser) }
    var pass by remember { mutableStateOf(settings.webDavPass) }
    var folder by remember { mutableStateOf(settings.webDavFolder) }
    var encryptKey by remember { mutableStateOf(settings.webDavEncryptKey) }

    var localJsonText by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Visual Preference
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Translations.get("theme_setting", settings.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = Translations.get("dark_theme", settings.language), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = { viewModel.toggleTheme(it) },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // Section: WebDAV syncing
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Translations.get("webdav_config", settings.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        viewModel.updateWebDavSettings(url, user, pass, folder, encryptKey)
                    },
                    label = { Text(text = Translations.get("webdav_url_label", settings.language)) },
                    placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webdav_url_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = user,
                    onValueChange = {
                        user = it
                        viewModel.updateWebDavSettings(url, user, pass, folder, encryptKey)
                    },
                    label = { Text(text = Translations.get("webdav_user_label", settings.language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webdav_user_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = {
                        pass = it
                        viewModel.updateWebDavSettings(url, user, pass, folder, encryptKey)
                    },
                    label = { Text(text = Translations.get("webdav_pass_label", settings.language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webdav_pass_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = folder,
                    onValueChange = {
                        folder = it
                        viewModel.updateWebDavSettings(url, user, pass, folder, encryptKey)
                    },
                    label = { Text(text = Translations.get("webdav_folder_label", settings.language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webdav_folder_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = encryptKey,
                    onValueChange = {
                        encryptKey = it
                        viewModel.updateWebDavSettings(url, user, pass, folder, encryptKey)
                    },
                    label = { Text(text = Translations.get("webdav_encrypt_label", settings.language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webdav_key_input"),
                    singleLine = true
                )

                // Sync status timestamp
                val timeStr = if (settings.lastBackupTime > 0L) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                    sdf.format(Date(settings.lastBackupTime))
                } else {
                    Translations.get("never_backed_up", settings.language)
                }
                Text(
                    text = "${Translations.get("last_backup_prefix", settings.language)}$timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Buttons container
                if (syncInProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.testWebDavConnection() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("webdav_test_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(text = Translations.get("test_conn", settings.language))
                        }

                        Button(
                            onClick = { viewModel.backupToWebDav() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("webdav_backup_btn")
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = Translations.get("backup_to_cloud", settings.language))
                        }

                        Button(
                            onClick = { viewModel.restoreFromWebDav() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("webdav_restore_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = Translations.get("restore_from_cloud", settings.language))
                        }
                    }
                }
            }
        }

        // Section: Local Data Management
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Translations.get("local_data_title", settings.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Export/Import Box
                OutlinedTextField(
                    value = localJsonText,
                    onValueChange = { localJsonText = it },
                    label = { Text(text = "Plain JSON Backup Data (Read/Write)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("local_json_input"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val json = viewModel.exportLocalDataAsJson()
                            localJsonText = json
                            clipboard.setText(AnnotatedString(json))
                            Toast.makeText(context, "JSON exported and copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("local_export_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text(text = Translations.get("export_local_json", settings.language), style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = {
                            if (localJsonText.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter some JSON to import", Toast.LENGTH_SHORT).show()
                            } else {
                                val success = viewModel.importLocalDataFromJson(localJsonText)
                                if (success) {
                                    Toast.makeText(context, Translations.get("local_import_success", settings.language), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, Translations.get("local_import_fail", settings.language), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("local_import_btn")
                    ) {
                        Text(text = Translations.get("import_local_json", settings.language), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Wipe/Clear Button
                Button(
                    onClick = { viewModel.wipeLocalDatabase() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wipe_db_btn")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Translations.get("clear_local_data", settings.language), color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}
