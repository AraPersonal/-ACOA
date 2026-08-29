import re

# Fix AgentTools.kt
with open("app/src/main/java/com/example/data/AgentTools.kt", "r") as f:
    content = f.read()

# Add empty descriptions where missing
content = re.sub(r'Schema\(name = "([^"]+)", type = ', r'Schema(name = "\1", description = "\1", type = ', content)
with open("app/src/main/java/com/example/data/AgentTools.kt", "w") as f:
    f.write(content)

# Fix HomeScreen.kt
with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

content = content.replace("SmallTopAppBar(", "OptIn(ExperimentalMaterial3Api::class)\n            TopAppBar(")
content = content.replace("TopAppBarDefaults.smallTopAppBarColors", "TopAppBarDefaults.topAppBarColors")
content = content.replace("import androidx.compose.foundation.border", "import androidx.compose.foundation.border\nimport androidx.compose.foundation.BorderStroke")

# Fix outlinedTextFieldColors
content = content.replace("TextFieldDefaults.outlinedTextFieldColors", "OutlinedTextFieldDefaults.colors")

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
