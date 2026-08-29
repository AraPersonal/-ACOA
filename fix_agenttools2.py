import re

with open("app/src/main/java/com/example/data/AgentTools.kt", "r") as f:
    content = f.read()

# Fix the envCmds block
bad_env = "export JAVA_HOME='$javaDir'\nexport ANDROID_HOME='$sdkDir'\nexport PATH=\"\\$JAVA_HOME/bin:\\$ANDROID_HOME/cmdline-tools/latest/bin:\\$PATH\"\n"
good_env = "\"export JAVA_HOME='$javaDir'\\nexport ANDROID_HOME='$sdkDir'\\nexport PATH=\\\"\\\\$JAVA_HOME/bin:\\\\$ANDROID_HOME/cmdline-tools/latest/bin:\\\\$PATH\\\"\\n\""
content = content.replace('"' + bad_env + '"', good_env)

# Fix the newlines inside joinToString
content = content.replace('joinToString("\n")', 'joinToString("\\n")')
content = content.replace('") + "\n" + result.err.joinToString("', '") + "\\n" + result.err.joinToString("')
content = content.replace('joinToString("\n") + "\n" + result.err.joinToString("\n")', 'joinToString("\\n") + "\\n" + result.err.joinToString("\\n")')

# Move executeSetupBuildEnv INSIDE the object
content = content.replace("    }\n}\n\n    suspend fun executeSetupBuildEnv", "    }\n\n    suspend fun executeSetupBuildEnv")
content = content + "\n}"

with open("app/src/main/java/com/example/data/AgentTools.kt", "w") as f:
    f.write(content)
