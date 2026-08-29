import re

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

content = content.replace("OptIn(ExperimentalMaterial3Api::class)\n            TopAppBar(", "TopAppBar(")
content = content.replace("@Composable\nfun HomeScreen", "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun HomeScreen")

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
