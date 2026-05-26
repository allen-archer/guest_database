package com.allenarcher.guest.database.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.io.File

@Service
class BackupService(
    private val jdbcTemplate: JdbcTemplate,
    @Value($$"${DB_PATH:./data/guest_database.db}") private val dbPath: String
) {
    fun backup(): String {
        val backupFile = File(File(dbPath).canonicalFile.parent, "backup_guest_database.db")
        backupFile.delete()
        jdbcTemplate.execute("VACUUM INTO '${backupFile.absolutePath}'")
        return backupFile.absolutePath
    }
}
