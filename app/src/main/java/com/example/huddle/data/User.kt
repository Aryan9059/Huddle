package com.example.huddle.data

data class User(
    val id: String = "",
    val name: String = "",
    val profile: String = "",
    val email: String = "",
    val lastSeen: Long = 0,
    val teams: List<String> = emptyList(),
)
