package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.ChatMessageEntity
import com.example.data.ChatSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory)) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initPrefs(context)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val showSettings by viewModel.showSettings.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val currentModelName by viewModel.currentModelName.collectAsState()
    
    var expandedModelMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        modifier = Modifier.systemBarsPadding(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat")
                }
                Divider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(allSessions) { session ->
                        NavigationDrawerItem(
                            label = { 
                                Column {
                                    Text(session.title, maxLines = 1)
                                    Text(
                                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = session.id == currentSessionId,
                            onClick = {
                                viewModel.switchSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(onClick = { viewModel.deleteSession(session) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "N", 
                                        color = MaterialTheme.colorScheme.onPrimary, 
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Nexus Agent",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = "ROOT ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            ),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    TextButton(onClick = { expandedModelMenu = true }) {
                                        Text(currentModelName.replace("gemini-2.5-", ""), color = MaterialTheme.colorScheme.primary)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Model")
                                    }
                                    DropdownMenu(
                                        expanded = expandedModelMenu,
                                        onDismissRequest = { expandedModelMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Flash") },
                                            onClick = { viewModel.setModel("gemini-2.5-flash"); expandedModelMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Pro") },
                                            onClick = { viewModel.setModel("gemini-2.5-pro"); expandedModelMenu = false }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.setShowSettings(true) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp) }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("CHAT", style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color(0xFFCAC4D0),
                        modifier = if (selectedTabIndex == 0) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("LOGS", style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color(0xFFCAC4D0),
                        modifier = if (selectedTabIndex == 1) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("SYSTEM", style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color(0xFFCAC4D0),
                        modifier = if (selectedTabIndex == 2) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> ChatView(viewModel)
                        1 -> TerminalView(viewModel)
                        2 -> SystemMonitorView(viewModel)
                    }
                }
            }
        }
    }

    if (showSettings) {
        AccountProjectsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowSettings(false) }
        )
    }
}

@Composable
fun ShimmeringGeneratingIndicator() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
    }
}

@Composable
fun ChatView(viewModel: AgentViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isToolRunning by viewModel.isToolRunning.collectAsState()
    val currentToolName by viewModel.currentToolName.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        if (isToolRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CALLING: ${currentToolName ?: "tool"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else if (isGenerating) {
            ShimmeringGeneratingIndicator()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask agent or run command...", color = Color(0xFF938F99)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                if (isGenerating) {
                    FloatingActionButton(
                        onClick = { viewModel.cancelGeneration() },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(20.dp))
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val isTool = message.role == "tool"
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    if (isTool) {
        var expanded by remember { mutableStateOf(false) }
        val lines = message.content.lines()
        val title = lines.firstOrNull() ?: "Tool Call"
        val body = lines.drop(1).joinToString("\n")
        
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.9f).clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = "Tool", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                        }
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF000000), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(
                                    text = body, 
                                    color = MaterialTheme.colorScheme.secondary, 
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            shadowElevation = if (isUser) 8.dp else 0.dp,
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    val parts = message.content.split("```")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 1) {
                            // Code block
                            val lines = part.lines()
                            val lang = lines.firstOrNull() ?: ""
                            val code = lines.drop(1).joinToString("\n").trimEnd()
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E1E1E))) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(lang, color = Color.LightGray, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                        IconButton(
                                            onClick = {
                                                val clip = ClipData.newPlainText("Code", code)
                                                clipboardManager.setPrimaryClip(clip)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = Color.LightGray)
                                        }
                                    }
                                    Text(
                                        text = code,
                                        color = Color(0xFFD4D4D4),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        } else {
                            if (part.isNotBlank()) {
                                Text(
                                    text = part.trim(),
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalView(viewModel: AgentViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .background(Color(0xFF000000), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TERMINAL OUTPUT",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "CONNECTED",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(bottom = 4.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = logs,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountProjectsDialog(viewModel: AgentViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current as android.app.Activity
    val authMode by viewModel.authMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val email by viewModel.googleAccountEmail.collectAsState()
    val projects by viewModel.availableProjects.collectAsState()
    val currentProject by viewModel.googleProjectId.collectAsState()
    
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()

    LaunchedEffect(authErrorMessage) {
        authErrorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearAuthErrorMessage()
        }
    }

    var keyInput by remember { mutableStateOf(apiKey ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Account & Projects", style = MaterialTheme.typography.titleLarge)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = authMode == "API_KEY",
                        onClick = { viewModel.setAuthMode("API_KEY") },
                        label = { Text("API Key") }
                    )
                    FilterChip(
                        selected = authMode == "GOOGLE",
                        onClick = { viewModel.setAuthMode("GOOGLE") },
                        label = { Text("Google Sign-In") }
                    )
                }

                if (authMode == "API_KEY") {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (email == null) {
                        Button(
                            onClick = {
                                val clientId = "YOUR_WEB_CLIENT_ID"
                                if (clientId == "YOUR_WEB_CLIENT_ID" || clientId.isBlank()) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Web Client ID is missing. Please configure it in Settings.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    viewModel.signInWithGoogle(context, clientId)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign in with Google")
                        }
                    } else {
                        Text("Signed in as: $email", style = MaterialTheme.typography.bodyMedium)
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(currentProject ?: "Select GCP Project")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                projects.forEach { proj ->
                                    DropdownMenuItem(
                                        text = { Text(proj) },
                                        onClick = { 
                                            viewModel.setGoogleProjectId(proj)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        if (authMode == "API_KEY" && keyInput.isNotBlank()) {
                            viewModel.saveApiKey(context, keyInput)
                        } else {
                            onDismiss()
                        }
                    }) {
                        Text(if (authMode == "API_KEY") "Save" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun SystemMonitorView(viewModel: AgentViewModel) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        viewModel.startSystemMonitor(context)
        onDispose { viewModel.stopSystemMonitor() }
    }
    val stats by viewModel.systemStats.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .background(Color(0xFF000000), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SYSTEM METRICS",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)

            MetricCard(
                title = "CPU USAGE",
                usagePercent = stats.cpuUsagePercent / 100f,
                valueText = String.format(java.util.Locale.US, "%.1f%%", stats.cpuUsagePercent),
                detailText = "Core average"
            )
            
            MetricCard(
                title = "MEMORY (RAM)",
                usagePercent = if (stats.ramTotalMb > 0) stats.ramUsedMb.toFloat() / stats.ramTotalMb else 0f,
                valueText = "${stats.ramUsedMb} MB",
                detailText = "of ${stats.ramTotalMb} MB"
            )
            
            MetricCard(
                title = "INTERNAL STORAGE",
                usagePercent = if (stats.storageTotalGb > 0) stats.storageUsedGb / stats.storageTotalGb else 0f,
                valueText = String.format(java.util.Locale.US, "%.2f GB", stats.storageUsedGb),
                detailText = String.format(java.util.Locale.US, "of %.2f GB", stats.storageTotalGb)
            )
        }
    }
}

@Composable
fun MetricCard(title: String, usagePercent: Float, valueText: String, detailText: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = usagePercent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "progress"
    )
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace))
                Text(valueText, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(detailText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            Spacer(modifier = Modifier.height(12.dp))
            
            val progressColor = when {
                animatedProgress > 0.9f -> MaterialTheme.colorScheme.error
                animatedProgress > 0.7f -> Color(0xFFE6C84C)
                else -> MaterialTheme.colorScheme.secondary
            }
            
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}
