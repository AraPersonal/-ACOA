import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

# Update saveSettings to also instantiate with sessionType if we track it?
# Let's add a _currentSessionType flow
fields_to_add = """    private val _currentSessionType = MutableStateFlow("NORMAL")
    val currentSessionType: StateFlow<String> = _currentSessionType.asStateFlow()

    private val _assetVersions = MutableStateFlow<List<AssetVersion>>(emptyList())
    val assetVersions: StateFlow<List<AssetVersion>> = _assetVersions.asStateFlow()

    fun setSessionType(type: String) {
        _currentSessionType.value = type
        viewModelScope.launch {
            repository.getSessionsByType(type).collect { sessions ->
                if (sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                } else {
                    val newId = repository.insertSession(ChatSession(title = "New $type Session", sessionType = type))
                    _currentSessionId.value = newId
                }
            }
        }
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
"""

# Find a good place to insert these
init_idx = content.find("init {")
content = content[:init_idx] + fields_to_add + "\n" + content[init_idx:]

# Also fix GeminiService instantiations in the file
content = content.replace("geminiService = GeminiService(key, model, temp, pat, workspace)", "geminiService = GeminiService(key, model, temp, pat, workspace, _currentSessionType.value)")

# Replace the tool handling block
old_tools = """                        "read_file" -> {
                            val path = args.optString("path")
                            val logStr = "> read_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeReadFile(path)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] read_file\\nPath: $path\\nOutput:\\n$out"))
                            JSONObject(out)
                        }
                        else -> {"""
                        
new_tools = """                        "read_file" -> {
                            val path = args.optString("path")
                            val logStr = "> read_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeReadFile(path)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] read_file\\nPath: $path\\nOutput:\\n$out"))
                            JSONObject(out)
                        }
                        "mount_system_rw" -> {
                            val logStr = "> mount_system_rw"
                            logToTerminal(logStr)
                            val out = AgentTools.executeMountSystemRw()
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] mount_system_rw\\nOutput:\\n$out"))
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
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] backup_file\\nPath: $path\\nOutput:\\n$out"))
                            outObj
                        }
                        "restore_file" -> {
                            val path = args.optString("path")
                            val logStr = "> restore_file: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeRestoreFile(path)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] restore_file\\nPath: $path\\nOutput:\\n$out"))
                            JSONObject(out)
                        }
                        "modify_prop" -> {
                            val propName = args.optString("prop_name")
                            val propValue = args.optString("prop_value")
                            val logStr = "> modify_prop: $propName=$propValue"
                            logToTerminal(logStr)
                            val out = AgentTools.executeModifyProp(propName, propValue)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] modify_prop\\nName: $propName\\nValue: $propValue\\nOutput:\\n$out"))
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
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] run_gradlew\\nTask: $task\\nOutput:\\n$out"))
                            outObj
                        }
                        "write_code" -> {
                            val path = args.optString("path")
                            val contentStr = args.optString("content")
                            val logStr = "> write_code: $path"
                            logToTerminal(logStr)
                            val out = AgentTools.executeWriteFile(path, contentStr)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] write_code\\nPath: $path\\nOutput:\\n$out"))
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
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] scaffold_magisk_module\\nModuleId: $moduleId\\nOutput:\\n$out"))
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
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] zip_module\\nModuleId: $moduleId\\nOutput:\\n$out"))
                            outObj
                        }
                        else -> {"""
                        
content = content.replace(old_tools, new_tools)

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)
