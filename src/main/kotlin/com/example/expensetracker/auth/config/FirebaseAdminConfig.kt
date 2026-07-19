package com.example.expensetracker.auth.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseAdminConfig {

    @Bean
    fun firebaseApp(): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let {
            return it
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()

        return FirebaseApp.initializeApp(options)
    }
}