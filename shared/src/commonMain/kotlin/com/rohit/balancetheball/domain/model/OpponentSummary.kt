package com.rohit.balancetheball.domain.model

data class OpponentSummary(
    val uid: String,
    val username: String,
    val gamesPlayedTogether: Int,
    val lastPlayedAt: Long
)
