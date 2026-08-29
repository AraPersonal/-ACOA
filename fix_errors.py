import re

# Fix AgentViewModel.kt
with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("kotlinx.coroutines.flow.firstOrNull(repository.getSessionsByType(type))", "repository.getSessionsByType(type).firstOrNull()")

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)

# Fix HomeScreen.kt
with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.foundation.border", "import androidx.compose.foundation.border\nimport androidx.compose.foundation.BorderStroke")

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)

