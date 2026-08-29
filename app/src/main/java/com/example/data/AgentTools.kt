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

    val allTools = Tool(
        functionDeclarations = listOf(runShell, writeFile, readFile)
    )

    suspend fun executeRunShell(command: String, asRoot: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val shell = if (asRoot) Shell.cmd(command) else Shell.sh(command)
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
}
