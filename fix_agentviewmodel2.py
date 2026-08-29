import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("val out = AgentTools.executeRunShell(cmd, asRoot)", "val out = AgentTools.executeRunShell(cmd, asRoot, _workspaceDir.value)")

setup_case = """
                        "setup_build_environment" -> {
                            val workspaceDir = args.optString("workspace_dir", _workspaceDir.value)
                            val logStr = "> setup_build_environment: $workspaceDir"
                            logToTerminal(logStr)
                            val out = AgentTools.executeSetupBuildEnv(workspaceDir)
                            logToTerminal(out)
                            repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "tool", content = "[TOOL_CALL] setup_build_environment\\nWorkspace: $workspaceDir\\nOutput:\\n$out"))
                            JSONObject(out)
                        }
"""
content = content.replace("\"read_file\" -> {", setup_case + "                        \"read_file\" -> {")

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)
