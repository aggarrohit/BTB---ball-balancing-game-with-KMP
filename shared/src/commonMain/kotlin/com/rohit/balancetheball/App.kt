package com.rohit.balancetheball

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.rohit.balancetheball.presentation.createaccount.CreateAccountScreen
import com.rohit.balancetheball.presentation.game.GameScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentUser by remember { mutableStateOf<String?>(null) }

        if (currentUser == null) {
            CreateAccountScreen(
                onAccountCreated = { username -> currentUser = username }
            )
        } else {
            GameScreen(username = currentUser!!)
        }
    }
}