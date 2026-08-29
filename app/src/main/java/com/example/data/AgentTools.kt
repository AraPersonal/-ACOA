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
        description = "Writes text/scripts to the filesystem (handles directory creation and root fallback if standard I/O fails).",
        parameters = listOf(
            Schema(
                name = "path",
                description = "The absolute path of the file to write to",
                format = "string",
                type = com.google.ai.client.generativeai.type.FunctionType.STRING,
            ),
            Schema(
                name = "content",
                description = "The content to write",
                format = "string",
                type = com.google.ai.client.generativeai.type.FunctionType.STRING,
            )
        ),
        requiredParameters = listOf("path", "content")
    )

    val readFile = FunctionDeclaration(
        name = "read_file",
        description = "Reads file content from filesystem.",
        parameters = listOf(
            Schema(
                name = "path",
                description = "The absolute path of the file to read",
                format = "string",
                type = com.google.ai.client.generativeai.type.FunctionType.STRING,
            )
        ),
        requiredParameters = listOf("path")
    )

    
    val setupBuildEnv = FunctionDeclaration(
        name = "setup_build_environment",
        description = "Downloads and sets up aarch64 OpenJDK and Android Command Line Tools in the workspace .build-tools folder if they don't exist.",
        parameters = listOf(
            Schema(
                name = "workspace_dir",
                description = "The absolute path of the workspace directory",
                format = "string",
                type = com.google.ai.client.generativeai.type.FunctionType.STRING,
            )
        ),
        requiredParameters = listOf("workspace_dir")
    )

    val allTools = Tool(
        functionDeclarations = listOf(runShell, writeFile, readFile, setupBuildEnv)
    )


    
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
            val output = result.out.joinToString("\n")
            JSONObject(mapOf(
                "stdout_stderr" to output,
                "exit_code" to result.code
            )).toString()
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
            // Fallback to root write
            try {
                val escapeContent = content.replace("'", "'\\''")
                val cmd = "mkdir -p '${File(path).parent}' && echo '$escapeContent' > '$path'"
                val result = Shell.cmd(cmd).exec()
                if (result.isSuccess) {
                    JSONObject(mapOf("success" to true, "note" to "used root fallback")).toString()
                } else {
                    JSONObject(mapOf("success" to false, "error" to result.out.joinToString("\n"))).toString()
                }
            } catch (ex: Exception) {
                JSONObject(mapOf("success" to false, "error" to ex.message)).toString()
            }
        }
    }

    suspend fun executeReadFile(path: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val content = file.readText()
                JSONObject(mapOf("content" to content)).toString()
            } else {
                // Fallback to root read
                val result = Shell.cmd("cat '$path'").exec()
                if (result.isSuccess) {
                    JSONObject(mapOf("content" to result.out.joinToString("\n"))).toString()
                } else {
                    JSONObject(mapOf("error" to "Failed to read file")).toString()
                }
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
                cmds.add("echo 'Downloading OpenJDK 17 aarch64...'")
                cmds.add("mkdir -p '$javaDir'")
                cmds.add("wget -qO jdk.tar.gz 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.12_7.tar.gz'")
                cmds.add("tar -xzf jdk.tar.gz -C '$javaDir' --strip-components=1")
                cmds.add("rm jdk.tar.gz")
            } else {
                cmds.add("echo 'JDK already exists.'")
            }
            
            if (!File(sdkDir, "cmdline-tools/latest/bin/sdkmanager").exists()) {
                cmds.add("echo 'Downloading Android Command Line Tools...'")
                cmds.add("mkdir -p '$sdkDir/cmdline-tools'")
                cmds.add("wget -qO cmdline-tools.zip 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip'")
                cmds.add("unzip -q cmdline-tools.zip -d '$sdkDir/cmdline-tools'")
                cmds.add("mv '$sdkDir/cmdline-tools/cmdline-tools' '$sdkDir/cmdline-tools/latest'")
                cmds.add("rm cmdline-tools.zip")
            } else {
                cmds.add("echo 'SDK Manager already exists.'")
            }
            
            if (cmds.isEmpty()) {
                return@withContext JSONObject(mapOf("success" to true, "message" to "Build environment already set up.")).toString()
            }
            
            val script = cmds.joinToString("\n")
            val result = Shell.sh(script).exec()
            val output = result.out.joinToString("\n") + "\n" + result.err.joinToString("\n")
            
            if (result.isSuccess) {
                JSONObject(mapOf("success" to true, "output" to output)).toString()
            } else {
                JSONObject(mapOf("success" to false, "error" to output)).toString()
            }
        } catch (e: Exception) {
            JSONObject(mapOf("success" to false, "error" to e.message)).toString()
        }
    }

}