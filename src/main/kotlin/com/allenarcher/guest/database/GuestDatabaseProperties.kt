package com.allenarcher.guest.database

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "guest-database")
class GuestDatabaseProperties {
    var roomCombos: Map<String, List<String>> = emptyMap()
}
