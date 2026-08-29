package com.example.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.GenerateContentResponse
import org.json.JSONObject

class GeminiService(private val apiKey: String, modelName: String = "gemini-2.5-flash") {

    private val systemInstruction = content {
        text("You are an autonomous on-device terminal engineer. You can build directories, write scripts/modules (e.g., Magisk/KernelSU modules), set chmod permissions, run builds, and verify outcomes via function calling. Use the tools provided instead of just answering with markdown. If you need root, use as_root=true. If a command fails, try to diagnose and fix it. You have full access to a rooted Android environment via libsu.")
    }

    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        tools = listOf(AgentTools.allTools),
        systemInstruction = systemInstruction
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
