package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.AgentDatabase
import com.example.data.AgentRepository

interface AppContainer {
    val agentRepository: AgentRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AgentDatabase by lazy {
        Room.databaseBuilder(context, AgentDatabase::class.java, "agent_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    override val agentRepository: AgentRepository by lazy {
        AgentRepository(database.agentDao())
    }
}
