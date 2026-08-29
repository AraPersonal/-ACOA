import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

# Fields to add
fields_to_add = """
    private val _temperature = MutableStateFlow(1.0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _githubPat = MutableStateFlow("")
    val githubPat: StateFlow<String> = _githubPat.asStateFlow()

    private val _workspaceDir = MutableStateFlow("/sdcard/NexusWorkspace")
    val workspaceDir: StateFlow<String> = _workspaceDir.asStateFlow()
"""
content = content.replace("private val _currentModelName = MutableStateFlow(\"gemini-2.5-flash\")\n    val currentModelName: StateFlow<String> = _currentModelName.asStateFlow()", 
"private val _currentModelName = MutableStateFlow(\"gemini-2.5-flash\")\n    val currentModelName: StateFlow<String> = _currentModelName.asStateFlow()\n" + fields_to_add)

# In initPrefs:
init_prefs_old = """        val key = sharedPreferences.getString("api_key", null)
        _apiKey.value = key
        if (!key.isNullOrBlank()) {
            geminiService = GeminiService(key, _currentModelName.value)
        } else {
            _showSettings.value = true
        }"""
init_prefs_new = """        val key = sharedPreferences.getString("api_key", null)
        val model = sharedPreferences.getString("model_name", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        val temp = sharedPreferences.getFloat("temperature", 1.0f)
        val pat = sharedPreferences.getString("github_pat", "") ?: ""
        val workspace = sharedPreferences.getString("workspace_dir", "/sdcard/NexusWorkspace") ?: "/sdcard/NexusWorkspace"
        
        _apiKey.value = key
        _currentModelName.value = model
        _temperature.value = temp
        _githubPat.value = pat
        _workspaceDir.value = workspace

        if (!key.isNullOrBlank()) {
            geminiService = GeminiService(key, model, temp, pat, workspace)
        } else {
            _showSettings.value = true
        }"""
content = content.replace(init_prefs_old, init_prefs_new)

# In saveApiKey (let's rename or add saveSettings)
save_settings_old = """    fun saveApiKey(context: Context, key: String) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "agent_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().putString("api_key", key).apply()
        _apiKey.value = key
        geminiService = GeminiService(key, _currentModelName.value)
        _showSettings.value = false
    }"""
save_settings_new = """    fun saveSettings(context: Context, key: String, model: String, temp: Float, pat: String, workspace: String) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "agent_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit()
            .putString("api_key", key)
            .putString("model_name", model)
            .putFloat("temperature", temp)
            .putString("github_pat", pat)
            .putString("workspace_dir", workspace)
            .apply()
            
        _apiKey.value = key
        _currentModelName.value = model
        _temperature.value = temp
        _githubPat.value = pat
        _workspaceDir.value = workspace
        
        geminiService = GeminiService(key, model, temp, pat, workspace)
        _showSettings.value = false
    }
    
    fun saveApiKey(context: Context, key: String) {
        saveSettings(context, key, _currentModelName.value, _temperature.value, _githubPat.value, _workspaceDir.value)
    }"""
content = content.replace(save_settings_old, save_settings_new)

# Fix Tool execution to save logs correctly, let's just make sure it passes onToolExecute
# and adds the badge logic... wait, UI handles the badge logic. We need to store FunctionCalls.
# AgentViewModel already has _terminalLogs and _isToolRunning etc.

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)
