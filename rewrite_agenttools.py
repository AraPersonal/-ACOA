import re

with open("app/src/main/java/com/example/data/AgentTools.kt", "r") as f:
    content = f.read()

# I will write a script to append all the new tools to AgentTools object and update the `allTools` or maybe we need separate lists of tools for different sessions!
