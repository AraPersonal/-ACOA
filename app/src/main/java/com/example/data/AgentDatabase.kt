package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatSession::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
}
