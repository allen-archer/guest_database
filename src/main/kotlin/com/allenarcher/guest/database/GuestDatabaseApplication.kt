package com.allenarcher.guest.database

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GuestDatabaseApplication

fun main(args: Array<String>) {
	runApplication<GuestDatabaseApplication>(*args)
}
