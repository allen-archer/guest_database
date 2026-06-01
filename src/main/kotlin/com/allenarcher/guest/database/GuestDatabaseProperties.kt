package com.allenarcher.guest.database

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "guest-database")
class GuestDatabaseProperties {
    var roomCombos: Map<String, List<String>> = emptyMap()
    var rememberMeKey: String = "changeme-key"
    var adminUser: String = "admin"
    var adminPassword: String = "changeme"
    var readerUser: String = "reader"
    var readerPassword: String = "changeme"
}
