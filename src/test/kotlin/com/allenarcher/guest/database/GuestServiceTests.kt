package com.allenarcher.guest.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class GuestServiceTests {

    @Autowired lateinit var guestService: GuestService
    @Autowired lateinit var guestRepository: GuestRepository

    @BeforeEach
    fun setUp() {
        guestRepository.deleteAll()
    }

    @Test
    fun `createGuest persists and returns guest with id`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice"))
        assertNotNull(response.id)
        assertEquals("Alice", response.name)
    }

    @Test
    fun `createGuest persists to database`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice"))
        val saved = guestRepository.findById(response.id)
        assertEquals("Alice", saved.get().name)
    }
}