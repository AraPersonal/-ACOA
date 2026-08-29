import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

install_apk = """
    fun installApk(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cmd = "pm install '$path'"
                val result = com.topjohnwu.superuser.Shell.cmd(cmd).exec()
                if (result.isSuccess) {
                    _terminalLogs.value += "\\n[System] Successfully installed $path"
                } else {
                    _terminalLogs.value += "\\n[System] Failed to install $path: ${result.err.joinToString("\\n")}"
                }
            } catch (e: Exception) {
                _terminalLogs.value += "\\n[System] Error installing APK: ${e.message}"
            }
        }
    }
"""

content = content.replace("fun startSystemMonitor(context: android.content.Context) {", install_apk + "\n    fun startSystemMonitor(context: android.content.Context) {")

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)

