package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.*
import com.allenarcher.guest.database.services.GuestService
import com.allenarcher.guest.database.services.StayService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
class GuestServiceTests {

    @Autowired lateinit var guestService: GuestService
    @Autowired lateinit var stayService: StayService
    @Autowired lateinit var guestRepository: GuestRepository
    @Autowired lateinit var stayRepository: StayRepository

    @BeforeEach
    fun setUp() {
        stayRepository.deleteAll()
        guestRepository.deleteAll()
    }

    private fun createEnrichedStay(stayExternalId: Long, guestExternalId: Long, checkIn: LocalDate, checkOut: LocalDate, room: String, status: StayStatus = StayStatus.SCHEDULED, confirmationCode: String? = null) {
        stayService.createStay(CreateStayRequest(
            externalId = stayExternalId,
            confirmationCode = confirmationCode,
            primaryGuestName = "Alice Smith",
            status = status,
            checkIn = checkIn,
            checkOut = checkOut,
            invoice = CreateInvoiceRequest(
                items = listOf(InvoiceItemRequest("Room", room, 1, BigDecimal("150.00"), checkIn)),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            )
        ))
        stayService.enrichStays(listOf(EnrichStayRequest(
            stay = EnrichStayData(externalId = stayExternalId),
            guest = EnrichGuestData(externalId = guestExternalId, name = "Alice Smith")
        )))
    }

    private fun fullRequest() = CreateGuestRequest(
        name = "Alice",
        externalId = 5001L,
        phones = listOf(PhoneRequest("555-100-0001")),
        emails = listOf(EmailRequest("alice@example.com")),
        addresses = listOf(AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"))
    )

    @Test
    fun `createGuest persists and returns guest with id`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice", 9001L))
        assertNotNull(response.id)
        assertEquals("Alice", response.name)
    }

    @Test
    fun `createGuest persists to database`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice", 9001L))
        val saved = guestRepository.findById(response.id)
        assertEquals("Alice", saved.get().name)
    }

    @Test
    fun `createGuest returns phones`() {
        val response = guestService.createGuest(fullRequest())
        assertEquals(1, response.phones.size)
        assertEquals("5551000001", response.phones[0].number)
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
    fun `createGuest returns externalId`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice", 9001L))
        assertEquals(9001L, response.externalId)
    }

    @Test
    fun `getGuestHistory returns correct count and last stay`() {
        createEnrichedStay(1001L, 5001L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), "Willow Cottage")
        createEnrichedStay(1002L, 5001L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 5), "Jade Vine Suite")
        val history = guestService.getGuestHistory(5001L)
        assertEquals(2, history.previousStayCount)
        assertEquals("Jade Vine Suite", history.lastStay?.room)
        assertEquals(LocalDate.of(2025, 6, 1), history.lastStay?.checkIn)
        assertEquals(LocalDate.of(2025, 6, 5), history.lastStay?.checkOut)
    }

    @Test
    fun `getGuestHistory excludes canceled stays`() {
        createEnrichedStay(1001L, 5001L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), "Willow Cottage")
        createEnrichedStay(1002L, 5001L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 5), "Jade Vine Suite", StayStatus.CANCELED)
        val history = guestService.getGuestHistory(5001L)
        assertEquals(1, history.previousStayCount)
        assertEquals("Willow Cottage", history.lastStay?.room)
    }

    @Test
    fun `getGuestHistory returns null lastStay when no stays`() {
        val history = guestService.getGuestHistory(9999L)
        assertEquals(0, history.previousStayCount)
        assertNull(history.lastStay)
    }

    @Test
    fun `createGuest with no phones emails or addresses returns empty lists`() {
        val response = guestService.createGuest(CreateGuestRequest("Alice", 9001L))
        assertEquals(emptyList<PhoneResponse>(), response.phones)
        assertEquals(emptyList<EmailResponse>(), response.emails)
        assertEquals(emptyList<AddressResponse>(), response.addresses)
    }
}