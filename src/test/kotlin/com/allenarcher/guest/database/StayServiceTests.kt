package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.*
import com.allenarcher.guest.database.services.StayService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
class StayServiceTests {

    @Autowired lateinit var stayService: StayService
    @Autowired lateinit var stayRepository: StayRepository
    @Autowired lateinit var guestRepository: GuestRepository

    @BeforeEach
    fun setUp() {
        stayRepository.deleteAll()
        guestRepository.deleteAll()
    }

    private fun createStayRequest(
        externalId: Long = 1001L,
        primaryGuestName: String = "Alice",
        confirmationCode: String? = null,
        checkIn: LocalDate,
        checkOut: LocalDate,
    ) = CreateStayRequest(
        externalId = externalId,
        confirmationCode = confirmationCode,
        primaryGuestName = primaryGuestName,
        checkIn = checkIn,
        checkOut = checkOut,
        invoice = CreateInvoiceRequest(
            items = listOf(InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1))),
            stateTax = BigDecimal("0.06"),
            countyTax = BigDecimal("0.01")
        )
    )

    @Test
    fun `stay within range is returned`() {
        stayService.createStay(createStayRequest(
            checkIn = LocalDate.of(2026, 3, 1),
            checkOut = LocalDate.of(2026, 3, 5)
        ))
        val results = stayService.getStaysInRange(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 12, 31)
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `stay outside range is not returned`() {
        stayService.createStay(createStayRequest(
            checkIn = LocalDate.of(2025, 6, 1),
            checkOut = LocalDate.of(2025, 6, 5)
        ))
        val results = stayService.getStaysInRange(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 12, 31)
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun `only stays within range are returned`() {
        stayService.createStay(createStayRequest(
            externalId = 1001L,
            checkIn = LocalDate.of(2026, 3, 1),
            checkOut = LocalDate.of(2026, 3, 5)
        ))
        stayService.createStay(createStayRequest(
            externalId = 1002L,
            checkIn = LocalDate.of(2025, 6, 1),
            checkOut = LocalDate.of(2025, 6, 5)
        ))
        val results = stayService.getStaysInRange(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 12, 31)
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `createStay returns correct dates and primaryGuestName`() {
        val response = stayService.createStay(createStayRequest(
            primaryGuestName = "Bob",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3)
        ))
        assertEquals(LocalDate.of(2026, 6, 1), response.checkIn)
        assertEquals(LocalDate.of(2026, 6, 3), response.checkOut)
        assertEquals("Bob", response.primaryGuestName)
    }

    @Test
    fun `createStay returns externalId`() {
        val response = stayService.createStay(createStayRequest(
            externalId = 1001L,
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3)
        ))
        assertEquals(1001L, response.externalId)
    }

    private fun enrichRequest(
        stayExternalId: Long = 1001L,
        guestExternalId: Long = 5001L
    ) = EnrichStayRequest(
        stay = EnrichStayData(
            externalId = stayExternalId,
            additionalGuestName = "Bob Smith",
            specialAccommodations = "Ground floor",
            dietaryRestrictions = "Gluten free",
            arrivalTime = "3:00 PM",
            housekeepingNotes = "No disturbance",
            reasonForStay = "Anniversary"
        ),
        guest = EnrichGuestData(
            externalId = guestExternalId,
            name = "Alice Smith",
            notes = "Prefers extra towels",
            phones = listOf(PhoneRequest("555-100-0001")),
            emails = listOf(EmailRequest("alice@example.com")),
            addresses = listOf(AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"))
        )
    )

    @Test
    fun `enrichStays creates guest and links to stay`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        val results = stayService.enrichStays(listOf(enrichRequest()))
        assertEquals(1, results.size)
        assertNotNull(results[0].guest)
        assertEquals(5001L, results[0].guest!!.externalId)
        assertEquals("Alice Smith", results[0].guest!!.name)
    }

    @Test
    fun `enrichStays updates stay fields`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        val result = stayService.enrichStays(listOf(enrichRequest()))[0]
        assertEquals("Bob Smith", result.additionalGuestName)
        assertEquals("Ground floor", result.specialAccommodations)
        assertEquals("Gluten free", result.dietaryRestrictions)
        assertEquals("3:00 PM", result.arrivalTime)
        assertEquals("No disturbance", result.housekeepingNotes)
        assertEquals("Anniversary", result.reasonForStay)
    }

    @Test
    fun `enrichStays updates existing guest name and notes`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.enrichStays(listOf(enrichRequest()))
        val updated = enrichRequest().copy(
            guest = enrichRequest().guest.copy(name = "Alice Updated", notes = "New note")
        )
        val result = stayService.enrichStays(listOf(updated))[0]
        assertEquals("Alice Updated", result.guest!!.name)
        assertEquals("New note", result.guest.notes)
        assertEquals(1, guestRepository.count())
    }

    @Test
    fun `enrichStays accumulates new phones emails and addresses on existing guest`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.enrichStays(listOf(enrichRequest()))
        val updated = enrichRequest().copy(
            guest = enrichRequest().guest.copy(
                phones = listOf(PhoneRequest("555-100-0001"), PhoneRequest("555-999-9999")),
                emails = listOf(EmailRequest("alice@example.com"), EmailRequest("alice2@example.com")),
                addresses = listOf(
                    AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"),
                    AddressRequest("456 Oak Ave", "Springfield", "IL", "62702", "US")
                )
            )
        )
        val result = stayService.enrichStays(listOf(updated))[0]
        assertEquals(2, result.guest!!.phones.size)
        assertEquals(2, result.guest.emails.size)
        assertEquals(2, result.guest.addresses.size)
    }

    @Test
    fun `enrichStays does not duplicate existing phones emails or addresses`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.enrichStays(listOf(enrichRequest()))
        val result = stayService.enrichStays(listOf(enrichRequest()))[0]
        assertEquals(1, result.guest!!.phones.size)
        assertEquals(1, result.guest.emails.size)
        assertEquals(1, result.guest.addresses.size)
    }

    @Test
    fun `enrichStays handles batch`() {
        stayService.createStay(createStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.createStay(createStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 7, 1), checkOut = LocalDate.of(2026, 7, 5)))
        val results = stayService.enrichStays(listOf(
            enrichRequest(stayExternalId = 1001L, guestExternalId = 5001L),
            enrichRequest(stayExternalId = 1002L, guestExternalId = 5002L)
        ))
        assertEquals(2, results.size)
        assertEquals(2, guestRepository.count())
    }

    @Test
    fun `enrichStays throws when stay not found`() {
        assertThrows(IllegalArgumentException::class.java) {
            stayService.enrichStays(listOf(enrichRequest(stayExternalId = 9999L)))
        }
    }

    @Test
    fun `cancelStay sets status to CANCELED`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        val result = stayService.cancelStay(1001L)
        assertEquals(StayStatus.CANCELED, result.status)
    }

    @Test
    fun `cancelStay throws when stay not found`() {
        assertThrows(IllegalArgumentException::class.java) {
            stayService.cancelStay(9999L)
        }
    }

    @Test
    fun `getStaysBriefing returns scheduled stays in range`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        val results = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1, results.size)
    }

    @Test
    fun `getStaysBriefing excludes canceled stays`() {
        stayService.createStay(createStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.createStay(createStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 7, 1), checkOut = LocalDate.of(2026, 7, 5)))
        stayService.cancelStay(1002L)
        val results = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1, results.size)
    }

    @Test
    fun `getStaysBriefing excludes stays outside range`() {
        stayService.createStay(createStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.createStay(createStayRequest(externalId = 1002L, checkIn = LocalDate.of(2025, 6, 1), checkOut = LocalDate.of(2025, 6, 5)))
        val results = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1, results.size)
    }

    @Test
    fun `getStaysBriefing returns correct fields`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.enrichStays(listOf(enrichRequest()))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals("Alice", result.primaryGuestName)
        assertEquals(LocalDate.of(2026, 6, 1), result.checkIn)
        assertEquals(LocalDate.of(2026, 6, 5), result.checkOut)
        assertEquals(4L, result.nights)
        assertEquals("Jade Vine Suite", result.room)
        assertEquals("Prefers extra towels", result.guestNotes)
        assertEquals(listOf("5551000001"), result.phones)
    }

    @Test
    fun `getStaysBriefing returns zero previousStayCount for first time guest`() {
        stayService.createStay(createStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.enrichStays(listOf(enrichRequest()))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(0, result.previousStayCount)
        assertNull(result.lastStay)
    }

    @Test
    fun `getStaysBriefing returns correct history for returning guest`() {
        stayService.createStay(createStayRequest(externalId = 1001L, checkIn = LocalDate.of(2025, 1, 1), checkOut = LocalDate.of(2025, 1, 3)))
        stayService.createStay(createStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)))
        stayService.enrichStays(listOf(enrichRequest(stayExternalId = 1001L, guestExternalId = 5001L)))
        stayService.enrichStays(listOf(enrichRequest(stayExternalId = 1002L, guestExternalId = 5001L)))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(1, result.previousStayCount)
        assertEquals("Jade Vine Suite", result.lastStay?.room)
        assertEquals(LocalDate.of(2025, 1, 1), result.lastStay?.checkIn)
    }

    @Test
    fun `getStaysWithoutGuest returns stays with no guest`() {
        stayService.createStay(createStayRequest(
            externalId = 1001L,
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5)
        ))
        val results = stayService.getStaysWithoutGuest()
        assertEquals(1, results.size)
        assertEquals(null, results[0].guest)
    }

}
