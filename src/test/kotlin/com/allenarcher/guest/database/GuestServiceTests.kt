package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.*
import com.allenarcher.guest.database.services.GuestService
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

    private fun fullRequest() = CreateGuestRequest(
        name = "Alice",
        phones = listOf(PhoneRequest("555-100-0001")),
        emails = listOf(EmailRequest("alice@example.com")),
        addresses = listOf(AddressRequest("123 Main St", "Springfield", "IL", "62701"))
    )

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

    @Test
    fun `createGuest returns phones`() {
        val response = guestService.createGuest(fullRequest())
        assertEquals(1, response.phones.size)
        assertEquals("555-100-0001", response.phones[0].number)
    }

    @Test
    fun `createGuest returns emails`() {
        val response = guestService.createGuest(fullRequest())
        assertEquals(1, response.emails.size)
        assertEquals("alice@example.com", response.emails[0].address)
    }

    @Test
    fun `createGuest returns addresses`() {
        val response = guestService.createGuest(fullRequest())
        assertEquals(1, response.addresses.size)
        with(response.addresses[0]) {
            assertEquals("123 Main St", street)
            assertEquals("Springfield", city)
            assertEquals("IL", state)
            assertEquals("62701", zip)
        }
    }

    @Test
    fun `createGuest with no phones emails or addresses returns empty lists`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice"))
        assertEquals(emptyList<PhoneResponse>(), response.phones)
        assertEquals(emptyList<EmailResponse>(), response.emails)
        assertEquals(emptyList<AddressResponse>(), response.addresses)
    }
}