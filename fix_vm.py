import re

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "r") as f:
    content = f.read()

bad_set_session = """    fun setSessionType(type: String) {
        _currentSessionType.value = type
        viewModelScope.launch {
            repository.getSessionsByType(type).collect { sessions ->
                if (sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                } else {
                    val newId = repository.insertSession(ChatSession(title = "New $type Session", sessionType = type))
                    _currentSessionId.value = newId
                }
            }
        }
        reinitGeminiService()
    }"""

good_set_session = """    fun setSessionType(type: String) {
        _currentSessionType.value = type
        viewModelScope.launch {
            val sessions = kotlinx.coroutines.flow.firstOrNull(repository.getSessionsByType(type))
            if (!sessions.isNullOrEmpty()) {
                _currentSessionId.value = sessions.first().id
            } else {
                val newId = repository.insertSession(ChatSession(title = "New $type Session", sessionType = type))
                _currentSessionId.value = newId
            }
        }
        reinitGeminiService()
    }
    
    fun createNewSession(type: String) {
        _currentSessionType.value = type
        viewModelScope.launch {
            val newId = repository.insertSession(ChatSession(title = "New $type Session", sessionType = type))
            _currentSessionId.value = newId
        }
        reinitGeminiService()
    }
    
    fun loadSession(sessionId: Int, type: String) {
        _currentSessionId.value = sessionId
        _currentSessionType.value = type
        reinitGeminiService()
    }"""
    
content = content.replace(bad_set_session, good_set_session)
content = content.replace("import kotlinx.coroutines.flow.flatMapLatest", "import kotlinx.coroutines.flow.flatMapLatest\nimport kotlinx.coroutines.flow.firstOrNull")

with open("app/src/main/java/com/example/ui/AgentViewModel.kt", "w") as f:
    f.write(content)

