package com.example.data

import kotlinx.coroutines.flow.Flow

class AgentRepository(private val dao: AgentDao) {
    val allSessions: Flow<List<ChatSession>> = dao.getAllSessions()

    suspend fun insertSession(session: ChatSession): Int {
        return dao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: ChatSession) = dao.updateSession(session)

    suspend fun deleteSession(session: ChatSession) {
        dao.deleteMessagesForSession(session.id)
        dao.deleteSession(session)
    }

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessageEntity>> {
        return dao.getMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: ChatMessageEntity) = dao.insertMessage(message)
}
