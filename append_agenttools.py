import re

with open("app/src/main/java/com/example/data/AgentTools.kt", "r") as f:
    content = f.read()
    
# Remove the closing bracket
content = content.rsplit('}', 1)[0]

new_methods = """
    suspend fun executeMountSystemRw(): String = withContext(Dispatchers.IO) {
        val result = Shell.cmd("mount -o rw,remount /system || mount -o rw,remount /").exec()
        JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\\n") + "\\n" + result.err.joinToString("\\n"))).toString()
    }
    
    suspend fun executeBackupFile(path: String): String = withContext(Dispatchers.IO) {
        val backupPath = "$path.bak"
        val result = Shell.cmd("cp -p '$path' '$backupPath'").exec()
        JSONObject(mapOf("success" to result.isSuccess, "backup_path" to backupPath, "output" to result.out.joinToString("\\n") + "\\n" + result.err.joinToString("\\n"))).toString()
    }
    
    suspend fun executeRestoreFile(path: String): String = withContext(Dispatchers.IO) {
        val backupPath = "$path.bak"
        val result = Shell.cmd("cp -p '$backupPath' '$path'").exec()
        JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\\n") + "\\n" + result.err.joinToString("\\n"))).toString()
    }
    
    suspend fun executeModifyProp(propName: String, propValue: String): String = withContext(Dispatchers.IO) {
        val result = Shell.cmd("resetprop -n '$propName' '$propValue'").exec()
        JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\\n") + "\\n" + result.err.joinToString("\\n"))).toString()
    }
    
    suspend fun executeRunGradlew(task: String, workspaceDir: String): String = withContext(Dispatchers.IO) {
        val result = executeRunShell("./gradlew $task", false, workspaceDir)
        result
    }
    
    suspend fun executeScaffoldMagiskModule(moduleId: String, moduleName: String, moduleVersion: String, moduleAuthor: String, moduleDescription: String, workspaceDir: String): String = withContext(Dispatchers.IO) {
        val dir = File(workspaceDir, moduleId)
        dir.mkdirs()
        File(dir, "module.prop").writeText("id=$moduleId\\nname=$moduleName\\nversion=$moduleVersion\\nversionCode=1\\nauthor=$moduleAuthor\\ndescription=$moduleDescription")
        File(dir, "customize.sh").writeText("#!/system/bin/sh\\n# customize.sh")
        File(dir, "post-fs-data.sh").writeText("#!/system/bin/sh\\n# post-fs-data.sh")
        JSONObject(mapOf("success" to true, "path" to dir.absolutePath)).toString()
    }
    
    suspend fun executeZipModule(moduleId: String, workspaceDir: String): String = withContext(Dispatchers.IO) {
        val cmd = "cd '$workspaceDir/$moduleId' && zip -r '../$moduleId.zip' ."
        val result = Shell.cmd(cmd).exec()
        JSONObject(mapOf("success" to result.isSuccess, "zip_path" to "$workspaceDir/$moduleId.zip", "output" to result.out.joinToString("\\n") + "\\n" + result.err.joinToString("\\n"))).toString()
    }
}
"""
with open("app/src/main/java/com/example/data/AgentTools.kt", "w") as f:
    f.write(content + new_methods)

