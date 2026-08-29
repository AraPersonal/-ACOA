with open("app/src/main/java/com/example/data/AgentTools.kt", "r") as f:
    content = f.read()

bad_env = '"export JAVA_HOME=\'$javaDir\'\\nexport ANDROID_HOME=\'$sdkDir\'\\nexport PATH=\\"\\\\$JAVA_HOME/bin:\\\\$ANDROID_HOME/cmdline-tools/latest/bin:\\\\$PATH\\"\\n"'
good_env = '"export JAVA_HOME=\'$javaDir\'\\nexport ANDROID_HOME=\'$sdkDir\'\\nexport PATH=\\"\\$JAVA_HOME/bin:\\$ANDROID_HOME/cmdline-tools/latest/bin:\\$PATH\\"\\n"'

content = content.replace(bad_env, good_env)

with open("app/src/main/java/com/example/data/AgentTools.kt", "w") as f:
    f.write(content)
