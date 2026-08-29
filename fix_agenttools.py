import re

with open("app/src/main/java/com/example/data/AgentTools.kt", "r") as f:
    content = f.read()

setup_build_environment = """
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
"""

content = content.replace("val allTools = Tool(\n        functionDeclarations = listOf(runShell, writeFile, readFile)\n    )", 
setup_build_environment + """
    val allTools = Tool(
        functionDeclarations = listOf(runShell, writeFile, readFile, setupBuildEnv)
    )
""")

setup_build_env_impl = """
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
"""

content = content.replace("suspend fun executeRunShell(command: String, asRoot: Boolean): String = withContext(Dispatchers.IO) {\n        try {\n            val shell = if (asRoot) Shell.cmd(command) else Shell.sh(command)", 
"""
    suspend fun executeRunShell(command: String, asRoot: Boolean, workspaceDir: String): String = withContext(Dispatchers.IO) {
        try {
            val buildToolsDir = File(workspaceDir, ".build-tools")
            val javaDir = File(buildToolsDir, "jdk")
            val sdkDir = File(buildToolsDir, "sdk")
            
            val envCmds = if (javaDir.exists() && sdkDir.exists()) {
                "export JAVA_HOME='$javaDir'\nexport ANDROID_HOME='$sdkDir'\nexport PATH=\"\\$JAVA_HOME/bin:\\$ANDROID_HOME/cmdline-tools/latest/bin:\\$PATH\"\n"
            } else {
                ""
            }
            
            val finalCommand = envCmds + command
            val shell = if (asRoot) Shell.cmd(finalCommand) else Shell.sh(finalCommand)
""")

content = content + setup_build_env_impl

with open("app/src/main/java/com/example/data/AgentTools.kt", "w") as f:
    f.write(content)
