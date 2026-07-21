package com.cortex.dl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cortex.dl.ui.theme.CortexBlue
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexSurfaceBorder
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(), onNavigateToHistory: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val sharedUrl by viewModel.sharedUrl.collectAsState()
    var urlText by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var isBatteryOptIgnored by remember { mutableStateOf(com.cortex.dl.util.PowerUtil.isIgnoringBatteryOptimizations(context)) }

    // Auto-fill shared URL if passed from Intent
    LaunchedEffect(sharedUrl) {
        sharedUrl?.let { url ->
            urlText = url
            viewModel.clearSharedUrl()
        }
    }

    // Auto-detect URL from clipboard on screen load
    LaunchedEffect(Unit) {
        clipboardManager.getText()?.text?.let { text ->
            if ((text.startsWith("http://") || text.startsWith("https://")) && text != urlText) {
                detectedClipboardUrl = text
            }
        }
    }

    // Show error snackbar whenever state is Error
    LaunchedEffect(uiState) {
        if (uiState is MainUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as MainUiState.Error).message,
                duration = SnackbarDuration.Long,
            )
            viewModel.resetState()
        }
    }

    Scaffold(
        containerColor = CortexDarkBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = CortexTextPrimary,
                    actionColor = CortexCyan,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CortexDarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            ) {

                // ── Gradient logo title ────────────────────────────────────────
                Text(
                    text = "Cortex DL",
                    style = TextStyle(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.linearGradient(listOf(CortexCyan, CortexBlue)),
                    ),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Advanced Video and Audio Downloader",
                    color = CortexTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                // ── Clipboard Detected Banner ─────────────────────────────────
                AnimatedVisibility(
                    visible = detectedClipboardUrl != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    detectedClipboardUrl?.let { link ->
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = CortexSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CortexCyan.copy(alpha = 0.6f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Link detected in clipboard",
                                        color = CortexCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = link,
                                        color = CortexTextPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                                Row {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            urlText = link
                                            detectedClipboardUrl = null
                                            viewModel.fetchVideoInfo(link)
                                        }
                                    ) {
                                        Text("Paste & Fetch", color = CortexCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    androidx.compose.material3.IconButton(
                                        onClick = { detectedClipboardUrl = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = CortexTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Battery Optimization Prompt Card ──────────────────────────
                AnimatedVisibility(
                    visible = !isBatteryOptIgnored,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Background Downloads",
                                    color = CortexTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Exempt app from battery saver for uninterrupted downloads.",
                                    color = CortexTextSecondary,
                                    fontSize = 11.sp,
                                )
                            }
                            androidx.compose.material3.Button(
                                onClick = {
                                    com.cortex.dl.util.PowerUtil.requestIgnoreBatteryOptimizations(context)
                                    isBatteryOptIgnored = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CortexBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Enable", color = CortexTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── URL input field ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CortexSurface)
                        .border(
                            width = 1.dp,
                            brush = if (uiState is MainUiState.Loading)
                                Brush.linearGradient(listOf(CortexCyan, CortexBlue))
                            else
                                SolidColor(CortexSurfaceBorder),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = CortexTextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 0.dp),
                        )
                        BasicTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            textStyle = TextStyle(
                                color = CortexTextPrimary,
                                fontSize = 15.sp,
                            ),
                            cursorBrush = SolidColor(CortexCyan),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboardController?.hide()
                                    viewModel.fetchVideoInfo(urlText.trim())
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            decorationBox = { innerTextField ->
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (urlText.isEmpty()) {
                                            Text(
                                                stringResource(com.cortex.dl.R.string.enter_url_to_download),
                                                color = CortexTextSecondary,
                                                fontSize = 15.sp,
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (urlText.isEmpty()) {
                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                clipboardManager.getText()?.text?.let { pastedText ->
                                                    if (pastedText.startsWith("http")) {
                                                        urlText = pastedText
                                                        viewModel.fetchVideoInfo(pastedText.trim())
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.ContentPaste,
                                                contentDescription = stringResource(com.cortex.dl.R.string.paste),
                                                tint = CortexCyan
                                            )
                                        }
                                    } else {
                                        androidx.compose.material3.IconButton(
                                            onClick = { urlText = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                                contentDescription = stringResource(com.cortex.dl.R.string.clear),
                                                tint = CortexTextSecondary
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Download / Loading button ──────────────────────────────────
                val isLoading = uiState is MainUiState.Loading

                Button(
                    onClick = {
                        if (!isLoading) {
                            keyboardController?.hide()
                            viewModel.fetchVideoInfo(urlText.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isLoading)
                                Brush.linearGradient(
                                    listOf(CortexCyan.copy(alpha = 0.5f), CortexBlue.copy(alpha = 0.5f))
                                )
                            else
                                Brush.linearGradient(listOf(CortexCyan, CortexBlue))
                        ),
                ) {
                    if (isLoading) {
                        // Spinner + label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CortexDarkBackground,
                                strokeWidth = 2.5.dp,
                            )
                            Text(
                                text = stringResource(com.cortex.dl.R.string.fetching_info),
                                color = CortexDarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(com.cortex.dl.R.string.start_download),
                            color = CortexDarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Subtle status hint ─────────────────────────────────────────
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(com.cortex.dl.R.string.fetching_info),
                        color = CortexTextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // ── Bottom sheet (shown when info is ready) ────────────────────────────
        if (uiState is MainUiState.ReadyWithInfo) {
            val info = (uiState as MainUiState.ReadyWithInfo).videoInfo
            val taskAddedMessage = stringResource(com.cortex.dl.R.string.task_added)
            VideoInfoBottomSheet(
                videoInfo = info,
                sheetState = sheetState,
                onDismiss = { viewModel.resetState() },
                onStartDownload = { prefs ->
                    viewModel.startDownload(videoInfo = info, preferences = prefs)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = taskAddedMessage,
                            duration = SnackbarDuration.Short,
                        )
                    }
                    onNavigateToHistory()
                },
            )
        }
    }
}
