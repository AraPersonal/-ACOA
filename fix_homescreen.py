import re

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

# Replace SettingsDialog invocation
content = content.replace("SettingsDialog(onDismiss = { viewModel.setShowSettings(false) }, onSave = { viewModel.saveApiKey(context, it) })", "AccountProjectsDialog(viewModel = viewModel, onDismiss = { viewModel.setShowSettings(false) })")

# Replace SettingsDialog implementation
old_settings_dialog = """@Composable
fun SettingsDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    
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
                Text(
                    text = "API Configuration",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Gemini API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (key.isNotBlank()) onSave(key) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}"""

new_account_dialog = """@Composable
fun AccountProjectsDialog(viewModel: AgentViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val authMode by viewModel.authMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val email by viewModel.googleAccountEmail.collectAsState()
    val projects by viewModel.availableProjects.collectAsState()
    val currentProject by viewModel.googleProjectId.collectAsState()
    
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
                            onClick = { viewModel.signInWithGoogle(context, "YOUR_WEB_CLIENT_ID") },
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
}"""

content = content.replace(old_settings_dialog, new_account_dialog)

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)

