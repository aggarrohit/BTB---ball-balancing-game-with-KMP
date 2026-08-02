package com.rohit.balancetheball.core.push

/** Parsed out of a tapped invite notification's data payload — see functions/index.js for the wire shape. */
data class PendingInvite(
    val inviteId: String,
    val fromUid: String,
    val fromUsername: String,
    val roomCode: String
)
