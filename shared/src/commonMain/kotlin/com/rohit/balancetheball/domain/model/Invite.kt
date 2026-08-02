package com.rohit.balancetheball.domain.model

enum class InviteStatus {
    PENDING, DECLINED;

    companion object {
        fun fromWireValue(value: String?): InviteStatus = when (value) {
            "declined" -> DECLINED
            else -> PENDING
        }
    }

    fun toWireValue(): String = when (this) {
        PENDING -> "pending"
        DECLINED -> "declined"
    }
}
