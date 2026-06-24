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

    private fun createFullStay(stayExternalId: Long, guestExternalId: Long, checkIn: LocalDate, checkOut: LocalDate, room: String, status: StayStatus = StayStatus.SCHEDULED) {
        stayService.upsertStays(listOf(UpsertStayRequest(
            externalId = stayExternalId,
            primaryGuestName = "Alice Smith",
            status = status,
            checkIn = checkIn,
            checkOut = checkOut,
            invoice = CreateInvoiceRequest(
                items = listOf(InvoiceItemRequest("Room", room, 1, BigDecimal("150.00"), checkIn)),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            ),
            guest = CreateGuestRequest(name = "Alice Smith", externalId = guestExternalId)
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
        createFullStay(1001L, 5001L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), "Willow Cottage")
        createFullStay(1002L, 5001L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 5), "Jade Vine Suite")
        val history = guestService.getGuestHistory(5001L)
        assertEquals(2, history.previousStayCount)
        assertEquals("Jade Vine Suite", history.lastStay?.rooms?.firstOrNull())
        assertEquals(LocalDate.of(2025, 6, 1), history.lastStay?.checkIn)
        assertEquals(LocalDate.of(2025, 6, 5), history.lastStay?.checkOut)
    }

    @Test
    fun `getGuestHistory excludes canceled stays`() {
        createFullStay(1001L, 5001L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), "Willow Cottage")
        createFullStay(1002L, 5001L, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 5), "Jade Vine Suite", StayStatus.CANCELED)
        val history = guestService.getGuestHistory(5001L)
        assertEquals(1, history.previousStayCount)
        assertEquals("Willow Cottage", history.lastStay?.rooms?.firstOrNull())
    }

    @Test
    fun `getGuestHistory does not count future stays`() {
        createFullStay(1001L, 5001L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), "Willow Cottage")
        createFullStay(1002L, 5001L, LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "Jade Vine Suite")
        val history = guestService.getGuestHistory(5001L)
        assertEquals(1, history.previousStayCount)
        assertEquals("Willow Cottage", history.lastStay?.rooms?.firstOrNull())
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

    private fun search(
        name: String? = null,
        email: String? = null,
        city: String? = null,
        state: String? = null,
        street: String? = null,
        phone: String? = null,
        zip: String? = null
    ) = guestService.searchGuests(name, email, city, state, street, phone, zip)

    @Test
    fun `searchGuests by name returns matching guest`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(name = "Alice").size)
    }

    @Test
    fun `searchGuests by name is case insensitive`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(name = "alice").size)
    }

    @Test
    fun `searchGuests by name partial match`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(name = "lic").size)
    }

    @Test
    fun `searchGuests by email returns matching guest`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(email = "alice@example.com").size)
    }

    @Test
    fun `searchGuests by email partial match`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(email = "example").size)
    }

    @Test
    fun `searchGuests by phone full number`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(phone = "5551000001").size)
    }

    @Test
    fun `searchGuests by phone partial digits`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(phone = "1000").size)
    }

    @Test
    fun `searchGuests by phone strips non-digits from input`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(phone = "555-100-0001").size)
    }

    @Test
    fun `searchGuests by city returns matching guest`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(city = "Springfield").size)
    }

    @Test
    fun `searchGuests by city is case insensitive`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(city = "springfield").size)
    }

    @Test
    fun `searchGuests by state returns matching guest`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(state = "IL").size)
    }

    @Test
    fun `searchGuests by state is case insensitive`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(state = "il").size)
    }

    @Test
    fun `searchGuests by street returns matching guest`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(street = "123 Main").size)
    }

    @Test
    fun `searchGuests by zip returns matching guest`() {
        guestService.createGuest(fullRequest())
        assertEquals(1, search(zip = "62701").size)
    }

    @Test
    fun `searchGuests multiple criteria narrows results`() {
        guestService.createGuest(fullRequest())
        guestService.createGuest(CreateGuestRequest("Bob", 9002L,
            addresses = listOf(AddressRequest("456 Oak Ave", "Springfield", "IL", "62702", "US"))))
        val results = search(name = "Alice", city = "Springfield")
        assertEquals(1, results.size)
        assertEquals("Alice", results[0].name)
    }

    @Test
    fun `searchGuests returns empty when no match`() {
        guestService.createGuest(fullRequest())
        assertEquals(0, search(name = "Nonexistent").size)
    }

    @Test
    fun `searchGuests returns empty when one criterion does not match`() {
        guestService.createGuest(fullRequest())
        assertEquals(0, search(name = "Alice", city = "Nonexistent City").size)
    }
}