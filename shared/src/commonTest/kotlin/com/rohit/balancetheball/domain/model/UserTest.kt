package com.rohit.balancetheball.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserTest {

    @Test
    fun `user holds uid, username and createdAt`() {
        val user = User(uid = "uid-1", username = "TestPlayer", createdAt = 1000L)
        assertEquals("uid-1", user.uid)
        assertEquals("TestPlayer", user.username)
        assertEquals(1000L, user.createdAt)
    }

    @Test
    fun `user defaults createdAt and email`() {
        val user = User(uid = "uid-1", username = "Player")
        assertEquals(0L, user.createdAt)
        assertEquals(null, user.email)
    }

    @Test
    fun `users with same fields are equal`() {
        val a = User(uid = "uid-1", username = "Player", createdAt = 999L)
        val b = User(uid = "uid-1", username = "Player", createdAt = 999L)
        assertEquals(a, b)
    }
}
