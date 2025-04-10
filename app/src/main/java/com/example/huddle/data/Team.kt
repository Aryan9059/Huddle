package com.example.huddle.data

data class Team (
    val teamId: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val admin: List<String> = emptyList(),
    val image: String = "",
)