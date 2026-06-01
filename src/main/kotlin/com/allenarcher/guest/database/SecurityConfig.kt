package com.allenarcher.guest.database

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(private val properties: GuestDatabaseProperties) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(encoder: PasswordEncoder): UserDetailsService {
        val admin = User.builder()
            .username(properties.adminUser)
            .password(encoder.encode(properties.adminPassword))
            .roles("ADMIN", "READER")
            .build()
        val reader = User.builder()
            .username(properties.readerUser)
            .password(encoder.encode(properties.readerPassword))
            .roles("READER")
            .build()
        return InMemoryUserDetailsManager(admin, reader)
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.GET, "/**").hasAnyRole("READER", "ADMIN")
                it.anyRequest().hasRole("ADMIN")
            }
            .formLogin { }
            .httpBasic { }
            .rememberMe { it.key(properties.rememberMeKey).tokenValiditySeconds(60 * 60 * 24 * 14) }
        return http.build()
    }
}