import re

with open("app/src/main/java/com/example/data/GeminiService.kt", "r") as f:
    content = f.read()

# Add sessionType parameter
content = content.replace("workspaceDir: String = \"/sdcard/NexusWorkspace\"\n) {", "workspaceDir: String = \"/sdcard/NexusWorkspace\",\n    private val sessionType: String = \"NORMAL\"\n) {")

# Update systemInstruction logic
system_instruction_replacement = """    private val systemInstruction = content {
        val baseInstruction = \"\"\"
You are an autonomous on-device terminal engineer. Use the tools provided instead of just answering with markdown. If a command fails, try to diagnose and fix it. You have full access to a rooted Android environment via libsu.

WORKSPACE INSTRUCTIONS:
Your designated workspace is: $workspaceDir
All operations, unless specified otherwise, should be inside this workspace.

GIT AUTHENTICATION:
Use the provided GitHub PAT for remote operations. Example: `git clone https://${githubPat}@github.com/user/repo.git`. Execute these using the `run_shell` tool.
        \"\"\".trimIndent()

        val typeInstruction = when (sessionType) {
            "SYSTEM_TWEAK" -> "You are managing system tweaks. ALWAYS call `backup_file` before modifying any file in /system, /vendor, or /data so the user can rollback. Then modify using `modify_prop` or writing the file."
            "APK_BUILDER" -> "You are an APK builder. You can compile Android projects using `run_gradlew assembleDebug`. Ensure the build environment is set up first using `setup_build_environment`."
            "MODULE_BUILDER" -> "You are a Magisk/KernelSU module builder. Use `scaffold_magisk_module` to create a new module, edit its contents, and then use `zip_module`."
            "TERMINAL" -> "You are a raw shell interface assistant. You can execute raw commands."
            else -> "You are a general AI assistant. You don't have access to dangerous tools."
        }
        text(baseInstruction + "\\n\\n" + typeInstruction)
    }"""
    
# Find the bounds of the existing systemInstruction
start_idx = content.find("private val systemInstruction = content {")
end_idx = content.find("    private val model = GenerativeModel(")
content = content[:start_idx] + system_instruction_replacement + "\n\n" + content[end_idx:]

# Update the GenerativeModel instantiation
content = content.replace("tools = listOf(AgentTools.allTools),", "tools = if (sessionType == \"NORMAL\") emptyList() else listOf(AgentTools.getToolsForSession(sessionType)),")

with open("app/src/main/java/com/example/data/GeminiService.kt", "w") as f:
    f.write(content)

