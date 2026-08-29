package com.example.data

import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

object AgentTools {
    val runShell = FunctionDeclaration(
        name = "run_shell",
        description = "Runs a command in background/root via libsu and returns combined stdout, stderr, and exit code.",
        parameters = listOf(
            Schema(
                name = "command",
                description = "The shell command to execute",
                format = "string",
                type = com.google.ai.client.generativeai.type.FunctionType.STRING,
            ),
            Schema(
                name = "as_root",
                description = "Run as root?",
                format = "boolean",
                type = com.google.ai.client.generativeai.type.FunctionType.BOOLEAN,
            )
        ),
        requiredParameters = listOf("command", "as_root")
    )
    val writeFile = FunctionDeclaration(
        name = "write_file",
        description = "Writes text/scripts to the filesystem.",
        parameters = listOf(
            Schema(name = "path", description = "The absolute path of the file to write to", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "content", description = "The content to write", type = com.google.ai.client.generativeai.type.FunctionType.STRING)
        ),
        requiredParameters = listOf("path", "content")
    )
    val readFile = FunctionDeclaration(
        name = "read_file",
        description = "Reads file content from filesystem.",
        parameters = listOf(Schema(name = "path", description = "path", type = com.google.ai.client.generativeai.type.FunctionType.STRING)),
        requiredParameters = listOf("path")
    )
    val setupBuildEnv = FunctionDeclaration(
        name = "setup_build_environment",
        description = "Downloads and sets up aarch64 OpenJDK and Android Command Line Tools in the workspace .build-tools folder.",
        parameters = listOf(Schema(name = "workspace_dir", description = "workspace_dir", type = com.google.ai.client.generativeai.type.FunctionType.STRING)),
        requiredParameters = listOf("workspace_dir")
    )
    val mountSystemRw = FunctionDeclaration(
        name = "mount_system_rw",
        description = "Mounts the system partition as read-write.",
        parameters = emptyList(),
        requiredParameters = emptyList()
    )
    val backupFile = FunctionDeclaration(
        name = "backup_file",
        description = "Backs up a file. Use this BEFORE modifying anything in /system, /vendor, or /data.",
        parameters = listOf(Schema(name = "path", description = "path", type = com.google.ai.client.generativeai.type.FunctionType.STRING)),
        requiredParameters = listOf("path")
    )
    val restoreFile = FunctionDeclaration(
        name = "restore_file",
        description = "Restores a backup of a file.",
        parameters = listOf(Schema(name = "path", description = "path", type = com.google.ai.client.generativeai.type.FunctionType.STRING)),
        requiredParameters = listOf("path")
    )
    val modifyProp = FunctionDeclaration(
        name = "modify_prop",
        description = "Modifies a build.prop property via resetprop or file editing.",
        parameters = listOf(
            Schema(name = "prop_name", description = "prop_name", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "prop_value", description = "prop_value", type = com.google.ai.client.generativeai.type.FunctionType.STRING)
        ),
        requiredParameters = listOf("prop_name", "prop_value")
    )
    val runGradlew = FunctionDeclaration(
        name = "run_gradlew",
        description = "Runs a gradle command in the workspace.",
        parameters = listOf(
            Schema(name = "task", description = "task", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "workspace_dir", description = "workspace_dir", type = com.google.ai.client.generativeai.type.FunctionType.STRING)
        ),
        requiredParameters = listOf("task", "workspace_dir")
    )
    val writeCode = FunctionDeclaration(
        name = "write_code",
        description = "Writes code to a file. Wrapper around write_file.",
        parameters = listOf(
            Schema(name = "path", description = "path", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "content", description = "content", type = com.google.ai.client.generativeai.type.FunctionType.STRING)
        ),
        requiredParameters = listOf("path", "content")
    )
    val scaffoldMagiskModule = FunctionDeclaration(
        name = "scaffold_magisk_module",
        description = "Generates module.prop, customize.sh, post-fs-data.sh for a Magisk module.",
        parameters = listOf(
            Schema(name = "module_id", description = "module_id", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "module_name", description = "module_name", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "module_version", description = "module_version", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "module_author", description = "module_author", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "module_description", description = "module_description", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "workspace_dir", description = "workspace_dir", type = com.google.ai.client.generativeai.type.FunctionType.STRING)
        ),
        requiredParameters = listOf("module_id", "module_name", "module_version", "module_author", "module_description", "workspace_dir")
    )
    val zipModule = FunctionDeclaration(
        name = "zip_module",
        description = "Zips the scaffolded Magisk module.",
        parameters = listOf(
            Schema(name = "module_id", description = "module_id", type = com.google.ai.client.generativeai.type.FunctionType.STRING),
            Schema(name = "workspace_dir", description = "workspace_dir", type = com.google.ai.client.generativeai.type.FunctionType.STRING)
        ),
        requiredParameters = listOf("module_id", "workspace_dir")
    )

    fun getToolsForSession(type: String): Tool {
        val list = when (type) {
            "SYSTEM_TWEAK" -> listOf(mountSystemRw, backupFile, restoreFile, modifyProp, runShell, writeFile, readFile)
            "APK_BUILDER" -> listOf(setupBuildEnv, runGradlew, writeCode, runShell, readFile)
            "MODULE_BUILDER" -> listOf(scaffoldMagiskModule, zipModule, writeCode, runShell, readFile)
            "TERMINAL" -> listOf(runShell, writeFile, readFile)
            else -> emptyList() // NORMAL has no tools
        }
        return Tool(list)
    }

    suspend fun executeRunShell(command: String, asRoot: Boolean, workspaceDir: String): String = withContext(Dispatchers.IO) {
        try {
            val buildToolsDir = File(workspaceDir, ".build-tools")
            val javaDir = File(buildToolsDir, "jdk")
            val sdkDir = File(buildToolsDir, "sdk")
            
            val envCmds = if (javaDir.exists() && sdkDir.exists()) {
                "export JAVA_HOME='$javaDir'\nexport ANDROID_HOME='$sdkDir'\nexport PATH=\"\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH\"\n"
            } else {
                ""
            }
            
            val finalCommand = envCmds + command
            val shell = if (asRoot) Shell.cmd(finalCommand) else Shell.sh(finalCommand)
            val result = shell.exec()
            val output = result.out.joinToString("\n") + "\n" + result.err.joinToString("\n")
            JSONObject(mapOf("stdout_stderr" to output, "exit_code" to result.code)).toString()
        } catch (e: Exception) {
            JSONObject(mapOf("error" to e.message)).toString()
        }
    }

    suspend fun executeWriteFile(path: String, content: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            JSONObject(mapOf("success" to true)).toString()
        } catch (e: Exception) {
            try {
                val escapeContent = content.replace("'", "'\\''")
                val cmd = "mkdir -p '${File(path).parent}' && echo '$escapeContent' > '$path'"
                val result = Shell.cmd(cmd).exec()
                if (result.isSuccess) JSONObject(mapOf("success" to true)).toString() else JSONObject(mapOf("success" to false)).toString()
            } catch (ex: Exception) {
                JSONObject(mapOf("success" to false)).toString()
            }
        }
    }

    suspend fun executeReadFile(path: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                JSONObject(mapOf("content" to file.readText())).toString()
            } else {
                val result = Shell.cmd("cat '$path'").exec()
                if (result.isSuccess) JSONObject(mapOf("content" to result.out.joinToString("\n"))).toString() else JSONObject(mapOf("error" to "failed")).toString()
            }
        } catch (e: Exception) {
            JSONObject(mapOf("error" to e.message)).toString()
        }
    }

    suspend fun executeSetupBuildEnv(workspaceDir: String): String = withContext(Dispatchers.IO) {
        try {
            val buildToolsDir = File(workspaceDir, ".build-tools")
            buildToolsDir.mkdirs()
            val javaDir = File(buildToolsDir, "jdk")
            val sdkDir = File(buildToolsDir, "sdk")
            val cmds = mutableListOf<String>()
            
            if (!File(javaDir, "bin/java").exists()) {
                cmds.add("mkdir -p '$javaDir' && wget -qO jdk.tar.gz 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.12_7.tar.gz' && tar -xzf jdk.tar.gz -C '$javaDir' --strip-components=1 && rm jdk.tar.gz")
            }
            if (!File(sdkDir, "cmdline-tools/latest/bin/sdkmanager").exists()) {
                cmds.add("mkdir -p '$sdkDir/cmdline-tools' && wget -qO cmdline-tools.zip 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' && unzip -q cmdline-tools.zip -d '$sdkDir/cmdline-tools' && mv '$sdkDir/cmdline-tools/cmdline-tools' '$sdkDir/cmdline-tools/latest' && rm cmdline-tools.zip")
            }
            if (cmds.isEmpty()) return@withContext JSONObject(mapOf("success" to true)).toString()
            
            val result = Shell.sh(cmds.joinToString("\n")).exec()
            JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\n"))).toString()
        } catch (e: Exception) {
            JSONObject(mapOf("success" to false)).toString()
        }
    }

    suspend fun executeMountSystemRw(): String = withContext(Dispatchers.IO) {
        val result = Shell.cmd("mount -o rw,remount /system || mount -o rw,remount /").exec()
        JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\n") + "\n" + result.err.joinToString("\n"))).toString()
    }
    
    suspend fun executeBackupFile(path: String): String = withContext(Dispatchers.IO) {
        val backupPath = "$path.bak"
        val result = Shell.cmd("cp -p '$path' '$backupPath'").exec()
        JSONObject(mapOf("success" to result.isSuccess, "backup_path" to backupPath, "output" to result.out.joinToString("\n") + "\n" + result.err.joinToString("\n"))).toString()
    }
    
    suspend fun executeRestoreFile(path: String): String = withContext(Dispatchers.IO) {
        val backupPath = "$path.bak"
        val result = Shell.cmd("cp -p '$backupPath' '$path'").exec()
        JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\n") + "\n" + result.err.joinToString("\n"))).toString()
    }
    
    suspend fun executeModifyProp(propName: String, propValue: String): String = withContext(Dispatchers.IO) {
        val result = Shell.cmd("resetprop -n '$propName' '$propValue'").exec()
        JSONObject(mapOf("success" to result.isSuccess, "output" to result.out.joinToString("\n") + "\n" + result.err.joinToString("\n"))).toString()
    }
    
    suspend fun executeRunGradlew(task: String, workspaceDir: String): String = withContext(Dispatchers.IO) {
        val result = executeRunShell("./gradlew $task", false, workspaceDir)
        result
    }
    
    suspend fun executeScaffoldMagiskModule(moduleId: String, moduleName: String, moduleVersion: String, moduleAuthor: String, moduleDescription: String, workspaceDir: String): String = withContext(Dispatchers.IO) {
        val dir = File(workspaceDir, moduleId)
        dir.mkdirs()
        File(dir, "module.prop").writeText("id=$moduleId\nname=$moduleName\nversion=$moduleVersion\nversionCode=1\nauthor=$moduleAuthor\ndescription=$moduleDescription")
        File(dir, "customize.sh").writeText("#!/system/bin/sh\n# customize.sh")
        File(dir, "post-fs-data.sh").writeText("#!/system/bin/sh\n# post-fs-data.sh")
        JSONObject(mapOf("success" to true, "path" to dir.absolutePath)).toString()
    }
    
    suspend fun executeZipModule(moduleId: String, workspaceDir: String): String = withContext(Dispatchers.IO) {
        val cmd = "cd '$workspaceDir/$moduleId' && zip -r '../$moduleId.zip' ."
        val result = Shell.cmd(cmd).exec()
        JSONObject(mapOf("success" to result.isSuccess, "zip_path" to "$workspaceDir/$moduleId.zip", "output" to result.out.joinToString("\n") + "\n" + result.err.joinToString("\n"))).toString()
    }
}
