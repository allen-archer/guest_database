package com.allenarcher.guest.database

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "guest-database")
class GuestDatabaseProperties {
    var roomCombos: Map<String, List<String>> = emptyMap()
    var rememberMeKey: String = "changeme-key"
    var admins: String = "admin:changeme"
    var readers: String = "reader:changeme"

    fun parseUsers(csv: String): List<Pair<String, String>> =
        csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map {
            val parts = it.split(":", limit = 2)
            parts[0].trim() to parts[1].trim()
        }
}
