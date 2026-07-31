package com.rohit.balancetheball.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserTest {

    @Test
    fun `user holds username and createdAt`() {
        val user = User(username = "TestPlayer", createdAt = 1000L)
        assertEquals("TestPlayer", user.username)
        assertEquals(1000L, user.createdAt)
    }

    @Test
    fun `user defaults createdAt to zero`() {
        val user = User(username = "Player")
        assertEquals(0L, user.createdAt)
    }

    @Test
    fun `users with same username and timestamp are equal`() {
        val a = User("Player", 999L)
        val b = User("Player", 999L)
        assertEquals(a, b)
    }
}
