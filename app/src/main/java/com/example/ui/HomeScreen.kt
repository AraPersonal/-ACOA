package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.io.File
import com.example.data.ChatMessageEntity

object Routes {
    const val DASHBOARD = "dashboard"
    const val GALLERY = "gallery"
    const val CHAT = "chat"
    const val CONSOLE = "console"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory)) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD

    Scaffold(
        containerColor = Color(0xFF1C1B1F),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF151419)) {
                NavigationBarItem(
                    selected = currentRoute == Routes.DASHBOARD,
                    onClick = { navController.navigate(Routes.DASHBOARD) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.Gray, indicatorColor = Color(0xFF2D2C31))
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.GALLERY,
                    onClick = { navController.navigate(Routes.GALLERY) { launchSingleTop = true } },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Gallery") },
                    label = { Text("Gallery", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.Gray, indicatorColor = Color(0xFF2D2C31))
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.CHAT,
                    onClick = { navController.navigate(Routes.CHAT) { launchSingleTop = true } },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                    label = { Text("Chat", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.Gray, indicatorColor = Color(0xFF2D2C31))
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.CONSOLE,
                    onClick = { navController.navigate(Routes.CONSOLE) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Console") },
                    label = { Text("Console", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.Gray, indicatorColor = Color(0xFF2D2C31))
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.SETTINGS,
                    onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.Gray, indicatorColor = Color(0xFF2D2C31))
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Nexus Agent AI", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF151419))
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            composable(Routes.DASHBOARD) { DashboardView(viewModel, navController) }
            composable(Routes.GALLERY) { GalleryView(viewModel) }
            composable(Routes.CHAT) { ChatView(viewModel) }
            composable(Routes.CONSOLE) { ConsoleView(viewModel) }
            composable(Routes.SETTINGS) { SettingsView(viewModel) }
        }
    }
}

@Composable
fun DashboardView(viewModel: AgentViewModel, navController: NavHostController) {
    val workspaces = listOf(
        Pair("Chat", "NORMAL"),
        Pair("Terminal", "TERMINAL"),
        Pair("Tweaks", "SYSTEM_TWEAK"),
        Pair("Apps", "APK_BUILDER"),
        Pair("Modules", "MODULE_BUILDER")
    )
    
    var selectedWorkspace by remember { mutableStateOf<String?>(null) }
    val allSessions by viewModel.allSessions.collectAsState()
    
    if (selectedWorkspace == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Workspaces", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(workspaces) { workspace ->
                    ElevatedCard(
                        onClick = { selectedWorkspace = workspace.second },
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF2D2C31)),
                        modifier = Modifier.height(120.dp).fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(workspace.first, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedWorkspace = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Session History", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    viewModel.createNewSession(selectedWorkspace!!)
                    navController.navigate(Routes.CHAT) { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("New Session")
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val filteredSessions = allSessions.filter { it.sessionType == selectedWorkspace }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSessions) { session ->
                    Card(
                        onClick = { 
                            viewModel.loadSession(session.id, session.sessionType)
                            navController.navigate(Routes.CHAT) { launchSingleTop = true }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2C31)),
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                            Text(session.title, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryView(viewModel: AgentViewModel) {
    val sessionType by viewModel.currentSessionType.collectAsState()
    
    val targetAssetType = when (sessionType) {
        "SYSTEM_TWEAK" -> "BACKUP"
        "APK_BUILDER" -> "APK"
        "MODULE_BUILDER" -> "MODULE"
        else -> null
    }

    if (targetAssetType == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No gallery available for $sessionType workspace.", color = Color.Gray)
        }
        return
    }

    LaunchedEffect(targetAssetType) { viewModel.loadAssetVersions(targetAssetType) }
    val versions by viewModel.assetVersions.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gallery ($targetAssetType)", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (versions.isEmpty()) {
            Text("No assets found.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(versions) { version ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2C31)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(File(version.filePath).name, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            when (targetAssetType) {
                                "BACKUP" -> {
                                    Button(onClick = { viewModel.restoreBackup(version) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("Restore", fontSize = 12.sp)
                                    }
                                }
                                "APK" -> {
                                    Button(onClick = { viewModel.installApk(version.filePath) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("Install", fontSize = 12.sp)
                                    }
                                }
                                "MODULE" -> {
                                    Button(onClick = { viewModel.sendMessage("Please flash the module at ${version.filePath} using `su -c magisk --install-module ${version.filePath}`") }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("Flash", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsoleView(viewModel: AgentViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Console", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(8.dp)).padding(8.dp)) {
            SelectionContainer {
                Text(logs, color = Color.Green, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(viewModel: AgentViewModel) {
    val context = LocalContext.current as android.app.Activity
    val currentModel by viewModel.currentModelName.collectAsState()
    val currentTemp by viewModel.temperature.collectAsState()
    val currentPat by viewModel.githubPat.collectAsState()
    val currentWorkspace by viewModel.workspaceDir.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    
    var keyInput by remember { mutableStateOf(apiKey ?: "") }
    var tempInput by remember { mutableStateOf(currentTemp) }
    var patInput by remember { mutableStateOf(currentPat) }
    var workspaceInput by remember { mutableStateOf(currentWorkspace) }
    var modelInput by remember { mutableStateOf(currentModel) }
    var expanded by remember { mutableStateOf(false) }
    val availableModels = listOf("gemini-3.6-flash", "models/gemini-3.1-pro-preview", "gemini-1.5-pro", "gemini-1.5-flash")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("AI Model", color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4A4950), unfocusedBorderColor = Color(0xFF4A4950)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2D2C31))
                    ) {
                        availableModels.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName, color = Color.White) },
                                onClick = {
                                    modelInput = modelName
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it }, label = { Text("Gemini API Key", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
            item {
                OutlinedTextField(
                    value = workspaceInput, onValueChange = { workspaceInput = it }, label = { Text("Workspace Directory", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
            item {
                OutlinedTextField(
                    value = patInput, onValueChange = { patInput = it }, label = { Text("GitHub PAT", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
            item {
                Text("Temperature: ${String.format(java.util.Locale.US, "%.1f", tempInput)}", color = Color.White)
                Slider(value = tempInput, onValueChange = { tempInput = it }, valueRange = 0f..2f)
            }
            item {
                Button(
                    onClick = { viewModel.saveSettings(context, keyInput, modelInput, tempInput, patInput, workspaceInput) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Save Settings")
                }
            }
        }
    }
}

@Composable
fun ShimmeringGeneratingIndicator() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1500, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "shimmer_anim"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF333333), Color(0xFF666666), Color(0xFF333333)),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterStart) {
        Box(modifier = Modifier.size(width = 120.dp, height = 24.dp).clip(RoundedCornerShape(12.dp)).background(brush))
    }
}

@Composable
fun ChatView(viewModel: AgentViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isToolRunning by viewModel.isToolRunning.collectAsState()
    val currentToolName by viewModel.currentToolName.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isToolRunning) { if (isToolRunning) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(isGenerating) { if (!isGenerating) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    val transition = rememberInfiniteTransition(label = "bg")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.0f, targetValue = if (isGenerating) 0.05f else 0.0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "bg_anim"
    )

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = alphaAnim), Color.Transparent)))) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg -> MessageBubble(msg) }
        }

        if (isToolRunning) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2D2C31), border = BorderStroke(1.dp, Color(0xFF4A4950))) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CALLING: ${currentToolName ?: "tool"}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = Color.LightGray)
                    }
                }
            }
        } else if (isGenerating) {
            ShimmeringGeneratingIndicator()
        }

        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF252429)).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1B1F), RoundedCornerShape(24.dp)).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Command or prompt...", color = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
                    )
                )
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = Color.White, contentColor = Color.Black, shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val isTool = message.role == "tool"

    if (isTool) {
        var expanded by remember { mutableStateOf(false) }
        val lines = message.content.lines()
        val title = lines.firstOrNull()?.replace("[TOOL_CALL]", "⚙️ Executing") ?: "⚙️ Executing..."
        val body = lines.drop(1).joinToString("\n")
        
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
            Surface(
                shape = RoundedCornerShape(12.dp), color = Color.Transparent, border = BorderStroke(1.dp, Color(0xFF4A4950)),
                modifier = Modifier.fillMaxWidth(0.9f).clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.LightGray)
                            Spacer(Modifier.width(8.dp))
                            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                        }
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", modifier = Modifier.size(16.dp), tint = Color.LightGray)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(Color.Black, RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Text(text = body, color = Color.Green, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isUser) Color(0xFF4A4950) else Color(0xFF2D2C31),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                SelectionContainer {
                    Text(text = message.content, color = Color.White, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
