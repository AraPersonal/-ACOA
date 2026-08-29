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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _currentModelName = MutableStateFlow("gemini-2.5-flash")
    val currentModelName: StateFlow<String> = _currentModelName.asStateFlow()

    private var geminiService: GeminiService? = null
    private var generationJob: Job? = null
    
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

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
        _apiKey.value = key
        if (!key.isNullOrBlank()) {
            geminiService = GeminiService(key, _currentModelName.value)
        } else {
            _showSettings.value = true
        }
    }
    
    fun saveApiKey(context: Context, key: String) {
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
                            val out = AgentTools.executeRunShell(cmd, asRoot)
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
                        "read_file" -> {
                            val path = args.optString("path")
                            val logStr = "> read_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeReadFile(path)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] read_file\nPath: $path\nOutput:\n$out"))
                            JSONObject(out)
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
