package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.AgentTools
import com.example.data.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AgentApplication
import com.example.data.AgentRepository
import com.example.data.ChatMessageEntity
import com.example.data.ChatSession
import com.example.data.AssetVersion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

data class SystemStats(
    val cpuUsagePercent: Float = 0f,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f
)

class AgentViewModel(private val repository: AgentRepository) : ViewModel() {
    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()

    val allSessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _terminalLogs = MutableStateFlow("")
    val terminalLogs: StateFlow<String> = _terminalLogs.asStateFlow()

    private val _isToolRunning = MutableStateFlow(false)
    val isToolRunning: StateFlow<Boolean> = _isToolRunning.asStateFlow()
    
    private val _currentToolName = MutableStateFlow<String?>(null)
    val currentToolName: StateFlow<String?> = _currentToolName.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _currentModelName = MutableStateFlow("gemini-3.6-flash")
    val currentModelName: StateFlow<String> = _currentModelName.asStateFlow()

    private val _temperature = MutableStateFlow(1.0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _githubPat = MutableStateFlow("")
    val githubPat: StateFlow<String> = _githubPat.asStateFlow()

    private val _workspaceDir = MutableStateFlow("/sdcard/NexusWorkspace")
    val workspaceDir: StateFlow<String> = _workspaceDir.asStateFlow()


    private var geminiService: GeminiService? = null
    private var generationJob: Job? = null
    
    
    private val _authMode = MutableStateFlow("API_KEY")
    val authMode: StateFlow<String> = _authMode.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()
    
    private val _googleIdToken = MutableStateFlow<String?>(null)

    private val _googleProjectId = MutableStateFlow<String?>(null)
    val googleProjectId: StateFlow<String?> = _googleProjectId.asStateFlow()

    private val _availableProjects = MutableStateFlow<List<String>>(emptyList())
    val availableProjects: StateFlow<List<String>> = _availableProjects.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

        private val _currentSessionType = MutableStateFlow("NORMAL")
    val currentSessionType: StateFlow<String> = _currentSessionType.asStateFlow()

    private val _assetVersions = MutableStateFlow<List<AssetVersion>>(emptyList())
    val assetVersions: StateFlow<List<AssetVersion>> = _assetVersions.asStateFlow()

    fun setSessionType(type: String) {
        _currentSessionType.value = type
        viewModelScope.launch {
            val sessions = repository.getSessionsByType(type).firstOrNull()
            if (!sessions.isNullOrEmpty()) {
                _currentSessionId.value = sessions.first().id
            } else {
                val newId = repository.insertSession(ChatSession(title = "New $type Session", sessionType = type))
                _currentSessionId.value = newId
            }
        }
        reinitGeminiService()
    }
    
    fun createNewSession(type: String) {
        _currentSessionType.value = type
        viewModelScope.launch {
            val newId = repository.insertSession(ChatSession(title = "New $type Session", sessionType = type))
            _currentSessionId.value = newId
        }
        reinitGeminiService()
    }
    
    fun loadSession(sessionId: Int, type: String) {
        _currentSessionId.value = sessionId
        _currentSessionType.value = type
        reinitGeminiService()
    }
    
    fun reinitGeminiService() {
        val key = _apiKey.value
        if (key != null) {
            geminiService = GeminiService(key, _currentModelName.value, _temperature.value, _githubPat.value, _workspaceDir.value, _currentSessionType.value)
        }
    }
    
    fun loadAssetVersions(type: String) {
        viewModelScope.launch {
            repository.getAssetVersionsByType(type).collect { versions ->
                _assetVersions.value = versions
            }
        }
    }
    
    fun restoreBackup(version: AssetVersion) {
        viewModelScope.launch(Dispatchers.IO) {
            AgentTools.executeRestoreFile(version.filePath)
            logToTerminal("Restored ${version.filePath} from backup.")
        }
    }

init {
        viewModelScope.launch {
            allSessions.collect { sessions ->
                if (sessions.isEmpty() && _currentSessionId.value == null) {
                    val newId = repository.insertSession(ChatSession(title = "New Chat"))
                    _currentSessionId.value = newId
                } else if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                }
            }
        }
    }

    
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    fun clearAuthErrorMessage() {
        _authErrorMessage.value = null
    }

    fun setAuthMode(mode: String) {
        _authMode.value = mode
    }

    fun setGoogleProjectId(projectId: String) {
        _googleProjectId.value = projectId
    }

    fun signInWithGoogle(context: Context, serverClientId: String) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    _googleAccountEmail.value = googleIdTokenCredential.id
                    _googleIdToken.value = googleIdTokenCredential.idToken
                    fetchGoogleCloudProjects(googleIdTokenCredential.idToken)
                }
            } catch (e: GetCredentialException) {
                android.util.Log.e("GoogleSignIn", "GetCredentialException failed", e)
                _authErrorMessage.value = "Credential error: ${e.message}"
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn", "Auth failed", e)
                _authErrorMessage.value = "Sign-In Failed: ${e.message}"
            }
        }
    }

    private fun fetchGoogleCloudProjects(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://cloudresourcemanager.googleapis.com/v1/projects")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val projects = json.optJSONArray("projects")
                    val projectList = mutableListOf<String>()
                    if (projects != null) {
                        for (i in 0 until projects.length()) {
                            val project = projects.getJSONObject(i)
                            projectList.add(project.getString("projectId"))
                        }
                    }
                    _availableProjects.value = projectList
                    if (projectList.isNotEmpty() && _googleProjectId.value == null) {
                        _googleProjectId.value = projectList.first()
                    }
                } else {
                    _terminalLogs.value += "\nFailed to fetch projects. Code: ${connection.responseCode}"
                }
            } catch (e: Exception) {
                _terminalLogs.value += "\nError fetching projects: ${e.message}"
            }
        }
    }

    fun initPrefs(context: Context) {
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
        
        val key = sharedPreferences.getString("api_key", null)
        val model = sharedPreferences.getString("model_name", "gemini-3.6-flash") ?: "gemini-3.6-flash"
        val temp = sharedPreferences.getFloat("temperature", 1.0f)
        val pat = sharedPreferences.getString("github_pat", "") ?: ""
        val workspace = sharedPreferences.getString("workspace_dir", "/sdcard/NexusWorkspace") ?: "/sdcard/NexusWorkspace"
        
        _apiKey.value = key
        _currentModelName.value = model
        _temperature.value = temp
        _githubPat.value = pat
        _workspaceDir.value = workspace

        if (!key.isNullOrBlank()) {
            geminiService = GeminiService(key, model, temp, pat, workspace, _currentSessionType.value)
        } else {
            _showSettings.value = true
        }
    }
    
    fun saveSettings(context: Context, key: String, model: String, temp: Float, pat: String, workspace: String) {
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
        
        geminiService = GeminiService(key, model, temp, pat, workspace, _currentSessionType.value)
        _showSettings.value = false
    }
    
    fun saveApiKey(context: Context, key: String) {
        saveSettings(context, key, _currentModelName.value, _temperature.value, _githubPat.value, _workspaceDir.value)
    }

    fun setModel(modelName: String) {
        _currentModelName.value = modelName
        _apiKey.value?.let { key ->
            geminiService = GeminiService(key, modelName)
        }
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newId = repository.insertSession(ChatSession(title = "New Chat"))
            _currentSessionId.value = newId
            _apiKey.value?.let { key ->
                geminiService = GeminiService(key, _currentModelName.value)
            }
        }
    }

    fun switchSession(sessionId: Int) {
        _currentSessionId.value = sessionId
        _apiKey.value?.let { key ->
            // Re-instantiate service to clear chat history context for the new session,
            // optionally we can feed previous messages to startChat(history = ...)
            geminiService = GeminiService(key, _currentModelName.value)
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
            }
        }
    }

    private fun logToTerminal(log: String) {
        _terminalLogs.value += "\n$log"
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        _isToolRunning.value = false
        _currentToolName.value = null
        logToTerminal("> Generation cancelled by user.")
    }

    fun sendMessage(message: String) {
        val service = geminiService ?: return
        val sessionId = _currentSessionId.value ?: return
        
        viewModelScope.launch {
            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "user", content = message))
        }

        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            try {
                val response = service.sendMessage(message) { toolName, args ->
                    _isToolRunning.value = true
                    _currentToolName.value = toolName
                    
                    val result = when (toolName) {
                        "run_shell" -> {
                            val cmd = args.optString("command")
                            val asRoot = args.optBoolean("as_root")
                            val logStr = "> run_shell(asRoot=$asRoot): $cmd"
                            logToTerminal(logStr)
                            val out = AgentTools.executeRunShell(cmd, asRoot, _workspaceDir.value)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] run_shell\nCommand: $cmd\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "write_file" -> {
                            val path = args.optString("path")
                            val content = args.optString("content")
                            val logStr = "> write_file: $path\n$content"
                            logToTerminal(logStr)
                            val out = AgentTools.executeWriteFile(path, content)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] write_file\nPath: $path\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        
                        "setup_build_environment" -> {
                            val workspaceDir = args.optString("workspace_dir", _workspaceDir.value)
                            val logStr = "> setup_build_environment: $workspaceDir"
                            logToTerminal(logStr)
                            val out = AgentTools.executeSetupBuildEnv(workspaceDir)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] setup_build_environment\nWorkspace: $workspaceDir\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "read_file" -> {
                            val path = args.optString("path")
                            val logStr = "> read_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeReadFile(path)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] read_file\nPath: $path\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "mount_system_rw" -> {
                            val logStr = "> mount_system_rw"
                            logToTerminal(logStr)
                            val out = AgentTools.executeMountSystemRw()
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] mount_system_rw\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "backup_file" -> {
                            val path = args.optString("path")
                            val logStr = "> backup_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeBackupFile(path)
                            val outObj = JSONObject(out)
                            if (outObj.optBoolean("success")) {
                                repository.insertAssetVersion(AssetVersion(sessionId = sessionId, filePath = path, versionNumber = 1, type = "BACKUP"))
                            }
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] backup_file\nPath: $path\nOutput:\n$out"))
                            outObj
                        }
                        "restore_file" -> {
                            val path = args.optString("path")
                            val logStr = "> restore_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeRestoreFile(path)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] restore_file\nPath: $path\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "modify_prop" -> {
                            val propName = args.optString("prop_name")
                            val propValue = args.optString("prop_value")
                            val logStr = "> modify_prop: $propName=$propValue"
                            logToTerminal(logStr)
                            val out = AgentTools.executeModifyProp(propName, propValue)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] modify_prop\nName: $propName\nValue: $propValue\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "run_gradlew" -> {
                            val task = args.optString("task")
                            val workspaceDir = args.optString("workspace_dir", _workspaceDir.value)
                            val logStr = "> run_gradlew: $task"
                            logToTerminal(logStr)
                            val out = AgentTools.executeRunGradlew(task, workspaceDir)
                            val outObj = JSONObject(out)
                            if (outObj.optInt("exit_code") == 0) {
                                repository.insertAssetVersion(AssetVersion(sessionId = sessionId, filePath = "$workspaceDir/app/build/outputs/apk/debug/app-debug.apk", versionNumber = 1, type = "APK"))
                            }
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] run_gradlew\nTask: $task\nOutput:\n$out"))
                            outObj
                        }
                        "write_code" -> {
                            val path = args.optString("path")
                            val contentStr = args.optString("content")
                            val logStr = "> write_code: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeWriteFile(path, contentStr)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] write_code\nPath: $path\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "scaffold_magisk_module" -> {
                            val moduleId = args.optString("module_id")
                            val moduleName = args.optString("module_name")
                            val moduleVersion = args.optString("module_version")
                            val moduleAuthor = args.optString("module_author")
                            val moduleDescription = args.optString("module_description")
                            val workspaceDir = args.optString("workspace_dir", _workspaceDir.value)
                            val logStr = "> scaffold_magisk_module: $moduleId"
                            logToTerminal(logStr)
                            val out = AgentTools.executeScaffoldMagiskModule(moduleId, moduleName, moduleVersion, moduleAuthor, moduleDescription, workspaceDir)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] scaffold_magisk_module\nModuleId: $moduleId\nOutput:\n$out"))
                            JSONObject(out)
                        }
                        "zip_module" -> {
                            val moduleId = args.optString("module_id")
                            val workspaceDir = args.optString("workspace_dir", _workspaceDir.value)
                            val logStr = "> zip_module: $moduleId"
                            logToTerminal(logStr)
                            val out = AgentTools.executeZipModule(moduleId, workspaceDir)
                            val outObj = JSONObject(out)
                            if (outObj.optBoolean("success")) {
                                repository.insertAssetVersion(AssetVersion(sessionId = sessionId, filePath = "$workspaceDir/$moduleId.zip", versionNumber = 1, type = "MODULE"))
                            }
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] zip_module\nModuleId: $moduleId\nOutput:\n$out"))
                            outObj
                        }
                        else -> {
                            logToTerminal("> Unknown tool: $toolName")
                            JSONObject().put("error", "Unknown tool")
                        }
                    }
                    _isToolRunning.value = false
                    _currentToolName.value = null
                    result
                }
                
                repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "model", content = response))
            } catch (e: kotlinx.coroutines.CancellationException) {
                repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "model", content = "[Cancelled]"))
                throw e
            } catch (e: Exception) {
                repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "model", content = "Error: ${e.message}"))
                logToTerminal("Exception: ${e.stackTraceToString()}")
            } finally {
                _isGenerating.value = false
                _isToolRunning.value = false
                _currentToolName.value = null
            }
        }
    }

    private var monitorJob: Job? = null

    
    fun installApk(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cmd = "pm install '$path'"
                val result = com.topjohnwu.superuser.Shell.cmd(cmd).exec()
                if (result.isSuccess) {
                    _terminalLogs.value += "\n[System] Successfully installed $path"
                } else {
                    _terminalLogs.value += "\n[System] Failed to install $path: ${result.err.joinToString("\n")}"
                }
            } catch (e: Exception) {
                _terminalLogs.value += "\n[System] Error installing APK: ${e.message}"
            }
        }
    }

    fun startSystemMonitor(context: android.content.Context) {
        if (monitorJob != null && monitorJob!!.isActive) return
        monitorJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var prevIdle = 0L
            var prevTotal = 0L
            
            val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()

            while(isActive) {
                // CPU
                var currentCpu = 0f
                try {
                    val result = com.topjohnwu.superuser.Shell.sh("cat /proc/stat").exec()
                    val cpuLine = result.out.firstOrNull { it.startsWith("cpu ") }
                    if (cpuLine != null) {
                        val toks = cpuLine.split(" +".toRegex())
                        val idle1 = toks[4].toLong()
                        val idle2 = toks.getOrNull(5)?.toLong() ?: 0L
                        val idle = idle1 + idle2
                        
                        var total = 0L
                        for (i in 1..8) {
                            total += toks.getOrNull(i)?.toLong() ?: 0L
                        }
                        
                        val diffIdle = idle - prevIdle
                        val diffTotal = total - prevTotal
                        if (prevTotal != 0L && diffTotal != 0L) {
                            currentCpu = ((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()) * 100f
                        }
                        prevIdle = idle
                        prevTotal = total
                    }
                } catch(e: Exception) { currentCpu = 0f }
                
                // RAM
                activityManager.getMemoryInfo(memoryInfo)
                val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
                val availRamMb = memoryInfo.availMem / (1024 * 1024)
                val usedRamMb = totalRamMb - availRamMb
                
                // Storage
                val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                val totalStorageGb = stat.totalBytes.toFloat() / (1024 * 1024 * 1024)
                val availableStorageGb = stat.availableBytes.toFloat() / (1024 * 1024 * 1024)
                val usedStorageGb = totalStorageGb - availableStorageGb
                
                _systemStats.value = SystemStats(
                    cpuUsagePercent = currentCpu.coerceIn(0f, 100f),
                    ramUsedMb = usedRamMb,
                    ramTotalMb = totalRamMb,
                    storageUsedGb = usedStorageGb,
                    storageTotalGb = totalStorageGb
                )
                
                kotlinx.coroutines.delay(2000)
            }
        }
    }
    
    fun stopSystemMonitor() {
        monitorJob?.cancel()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as AgentApplication)
                AgentViewModel(application.container.agentRepository)
            }
        }
    }
}
