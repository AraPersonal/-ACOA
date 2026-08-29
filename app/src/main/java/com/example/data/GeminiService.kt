package com.example.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONObject

class GeminiService(
    private val apiKey: String, 
    modelName: String = "gemini-2.5-flash",
    temperature: Float = 1.0f,
    githubPat: String = "",
    workspaceDir: String = "/sdcard/NexusWorkspace",
    private val sessionType: String = "NORMAL"
) {
        private val systemInstruction = content {
        val baseInstruction = """
You are an autonomous on-device terminal engineer. Use the tools provided instead of just answering with markdown. If a command fails, try to diagnose and fix it. You have full access to a rooted Android environment via libsu.

WORKSPACE INSTRUCTIONS:
Your designated workspace is: $workspaceDir
All operations, unless specified otherwise, should be inside this workspace.

GIT AUTHENTICATION:
Use the provided GitHub PAT for remote operations. Example: `git clone https://${githubPat}@github.com/user/repo.git`. Execute these using the `run_shell` tool.
        """.trimIndent()

        val typeInstruction = when (sessionType) {
            "SYSTEM_TWEAK" -> "You are managing system tweaks. ALWAYS call `backup_file` before modifying any file in /system, /vendor, or /data so the user can rollback. Then modify using `modify_prop` or writing the file."
            "APK_BUILDER" -> "You are an APK builder. You can compile Android projects using `run_gradlew assembleDebug`. Ensure the build environment is set up first using `setup_build_environment`."
            "MODULE_BUILDER" -> "You are a Magisk/KernelSU module builder. Use `scaffold_magisk_module` to create a new module, edit its contents, and then use `zip_module`."
            "TERMINAL" -> "You are a raw shell interface assistant. You can execute raw commands."
            else -> "You are a general AI assistant. You don't have access to dangerous tools."
        }
        text(baseInstruction + "\n\n" + typeInstruction)
    }

    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        tools = if (sessionType == "NORMAL") emptyList() else listOf(AgentTools.getToolsForSession(sessionType)),
        systemInstruction = systemInstruction,
        generationConfig = generationConfig {
            this.temperature = temperature
        }
    )

    private val chat = model.startChat()

    suspend fun sendMessage(
        message: String,
        onToolExecute: suspend (String, JSONObject) -> JSONObject
    ): String {
        var response = chat.sendMessage(message)
        return handleFunctionCalls(response, onToolExecute)
    }

    private suspend fun handleFunctionCalls(
        response: GenerateContentResponse,
        onToolExecute: suspend (String, JSONObject) -> JSONObject
    ): String {
        var currentResponse = response
        while (currentResponse.functionCalls.isNotEmpty()) {
            val functionResponses = currentResponse.functionCalls.map { functionCall ->
                val name = functionCall.name
                val args = JSONObject(functionCall.args)
                val result = onToolExecute(name, args)
                FunctionResponsePart(name, result)
            }
            currentResponse = chat.sendMessage(content {
                functionResponses.forEach { part ->
                    part(part)
                }
            })
        }
        return currentResponse.text ?: "No text response."
    }
}
