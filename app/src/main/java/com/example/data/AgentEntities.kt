package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val sessionType: String = "NORMAL", // NORMAL, TERMINAL, SYSTEM_TWEAK, APK_BUILDER, MODULE_BUILDER
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "asset_versions")
data class AssetVersion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val filePath: String,
    val versionNumber: Int,
    val type: String, // BACKUP, APK, MODULE
    val timestamp: Long = System.currentTimeMillis()
)
