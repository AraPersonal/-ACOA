import re

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

# 1. Imports
haptic_imports = """
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.io.File
"""
content = content.replace("import java.util.Locale", "import java.util.Locale\n" + haptic_imports)

# 2. Add Tab
tabs_old = """                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("SYSTEM", style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color(0xFFCAC4D0),
                        modifier = if (selectedTabIndex == 2) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                    )
                }"""
tabs_new = """                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("SYSTEM", style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color(0xFFCAC4D0),
                        modifier = if (selectedTabIndex == 2) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                    )
                    Tab(
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 },
                        text = { Text("FILES", style = MaterialTheme.typography.labelLarge) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color(0xFFCAC4D0),
                        modifier = if (selectedTabIndex == 3) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                    )
                }"""
content = content.replace(tabs_old, tabs_new)

# 3. Add Tab Content
when_old = """                        0 -> ChatView(viewModel)
                        1 -> TerminalView(viewModel)
                        2 -> SystemMonitorView(viewModel)
                    }"""
when_new = """                        0 -> ChatView(viewModel)
                        1 -> TerminalView(viewModel)
                        2 -> SystemMonitorView(viewModel)
                        3 -> FileManagerView(viewModel)
                    }"""
content = content.replace(when_old, when_new)

# 4. ChatView Haptics and Background
chatview_old = """fun ChatView(viewModel: AgentViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isToolRunning by viewModel.isToolRunning.collectAsState()
    val currentToolName by viewModel.currentToolName.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {"""
chatview_new = """fun ChatView(viewModel: AgentViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isToolRunning by viewModel.isToolRunning.collectAsState()
    val currentToolName by viewModel.currentToolName.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(isToolRunning) {
        if (isToolRunning) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    LaunchedEffect(isGenerating) {
        if (!isGenerating) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    val transition = rememberInfiniteTransition(label = "bg")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = if (isGenerating) 0.15f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bg_anim"
    )

    Column(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = alphaAnim), Color.Transparent)
        )
    )) {"""
content = content.replace(chatview_old, chatview_new)

# 5. AccountProjectsDialog Updates
dialog_old = """fun AccountProjectsDialog(viewModel: AgentViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current as android.app.Activity
    val authMode by viewModel.authMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val email by viewModel.googleAccountEmail.collectAsState()
    val projects by viewModel.availableProjects.collectAsState()
    val currentProject by viewModel.googleProjectId.collectAsState()
    
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()"""
dialog_new = """fun AccountProjectsDialog(viewModel: AgentViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current as android.app.Activity
    val authMode by viewModel.authMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val email by viewModel.googleAccountEmail.collectAsState()
    val projects by viewModel.availableProjects.collectAsState()
    val currentProject by viewModel.googleProjectId.collectAsState()
    
    val currentModel by viewModel.currentModelName.collectAsState()
    val currentTemp by viewModel.temperature.collectAsState()
    val currentPat by viewModel.githubPat.collectAsState()
    val currentWorkspace by viewModel.workspaceDir.collectAsState()
    
    var tempInput by remember { mutableFloatStateOf(currentTemp) }
    var patInput by remember { mutableStateOf(currentPat) }
    var workspaceInput by remember { mutableStateOf(currentWorkspace) }
    var modelInput by remember { mutableStateOf(currentModel) }
    
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()"""
content = content.replace(dialog_old, dialog_new)

save_btn_old = """                    Button(onClick = { 
                        if (authMode == "API_KEY" && keyInput.isNotBlank()) {
                            viewModel.saveApiKey(context, keyInput)
                        } else {
                            onDismiss()
                        }
                    }) {
                        Text(if (authMode == "API_KEY") "Save" else "Close")
                    }"""
save_btn_new = """                    Button(onClick = { 
                        if (authMode == "API_KEY" && keyInput.isNotBlank()) {
                            viewModel.saveSettings(context, keyInput, modelInput, tempInput, patInput, workspaceInput)
                        } else {
                            onDismiss()
                        }
                    }) {
                        Text(if (authMode == "API_KEY") "Save" else "Close")
                    }"""
content = content.replace(save_btn_old, save_btn_new)

dialog_fields_old = """                if (authMode == "API_KEY") {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {"""
dialog_fields_new = """                if (authMode == "API_KEY") {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                label = { Text("Gemini API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = workspaceInput,
                                onValueChange = { workspaceInput = it },
                                label = { Text("Workspace Directory") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = patInput,
                                onValueChange = { patInput = it },
                                label = { Text("GitHub PAT") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Temperature: ${String.format(java.util.Locale.US, "%.1f", tempInput)}")
                            Slider(
                                value = tempInput,
                                onValueChange = { tempInput = it },
                                valueRange = 0f..2f
                            )
                        }
                    }
                } else {"""
content = content.replace(dialog_fields_old, dialog_fields_new)

# 6. Message Bubble
msg_bubble_old = """    if (isTool) {
        var expanded by remember { mutableStateOf(false) }
        val lines = message.content.lines()
        val title = lines.firstOrNull() ?: "Tool Call"
        val body = lines.drop(1).joinToString("\\n")
        
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
        }"""
msg_bubble_new = """    if (isTool) {
        var expanded by remember { mutableStateOf(false) }
        val lines = message.content.lines()
        val title = lines.firstOrNull()?.replace("[TOOL_CALL]", "⚙️ Executing") ?: "⚙️ Executing..."
        val body = lines.drop(1).joinToString("\\n")
        
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(0.9f).clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                        }
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF000000), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(
                                    text = body, 
                                    color = MaterialTheme.colorScheme.secondary, 
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }
        }"""
content = content.replace(msg_bubble_old, msg_bubble_new)

# Append FileManagerView
filemanager_view = """
@Composable
fun FileManagerView(viewModel: AgentViewModel) {
    val workspace by viewModel.workspaceDir.collectAsState()
    var files by remember { mutableStateOf(emptyList<File>()) }
    
    LaunchedEffect(workspace) {
        val dir = File(workspace)
        if (!dir.exists()) dir.mkdirs()
        files = dir.listFiles()?.toList()?.sortedBy { !it.isDirectory } ?: emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .background(Color(0xFF000000), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "WORKSPACE: $workspace",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (file.isDirectory) Color(0xFFE6C84C) else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = file.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                        if (file.name.endsWith(".apk")) {
                            Button(
                                onClick = { viewModel.installApk(file.absolutePath) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Install", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

content = content + filemanager_view

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
