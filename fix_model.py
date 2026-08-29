import re

# Fix AgentViewModel.kt
with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

content = content.replace('"gemini-2.5-flash"', '"gemini-3.6-flash"')

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)

# Fix HomeScreen.kt
with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

old_settings_view = """@Composable
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it }, label = { Text("Gemini API Key", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }"""

new_settings_view = """@OptIn(ExperimentalMaterial3Api::class)
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
            }"""

if old_settings_view in content:
    content = content.replace(old_settings_view, new_settings_view)
else:
    print("WARNING: Old settings view not found.")

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)

