package com.chatapp.demo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allow requests from file:// (for local HTML files) and localhost
        configuration.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "null" // This allows file:// protocol
        )

        // Allow all origins (use this for development only)
        // configuration.allowedOriginPatterns = listOf("*")

        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true

        // Allow all HTTP methods
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        )

        // Allow all headers
        configuration.allowedHeaders = listOf("*")

        // Expose headers to the client
        configuration.exposedHeaders = listOf(
            "Authorization",
            "Content-Type"
        )

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}