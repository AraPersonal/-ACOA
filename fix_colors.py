import re

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

content = content.replace("textColor = Color.White", "focusedTextColor = Color.White, unfocusedTextColor = Color.White")
with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
