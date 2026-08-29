import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

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

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)
