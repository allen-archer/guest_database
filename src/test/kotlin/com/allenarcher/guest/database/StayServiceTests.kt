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

    private fun upsertStayRequest(
        externalId: Long = 1001L,
        primaryGuestName: String = "Alice",
        confirmationCode: String? = null,
        checkIn: LocalDate,
        checkOut: LocalDate,
        guest: UpsertGuestData? = null
    ) = UpsertStayRequest(
        externalId = externalId,
        confirmationCode = confirmationCode,
        primaryGuestName = primaryGuestName,
        checkIn = checkIn,
        checkOut = checkOut,
        invoice = CreateInvoiceRequest(
            items = listOf(InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1))),
            stateTax = BigDecimal("0.06"),
            countyTax = BigDecimal("0.01")
        ),
        guest = guest
    )

    private fun fullGuestData(externalId: Long = 5001L) = UpsertGuestData(
        externalId = externalId,
        name = "Alice Smith",
        notes = "Prefers extra towels",
        phones = listOf(PhoneRequest("555-100-0001")),
        emails = listOf(EmailRequest("alice@example.com")),
        addresses = listOf(AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"))
    )

    @Test
    fun `stay within range is returned`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 3, 1),
            checkOut = LocalDate.of(2026, 3, 5)
        )))
        val results = stayService.getStaysInRange(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 12, 31)
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `stay outside range is not returned`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2025, 6, 1),
            checkOut = LocalDate.of(2025, 6, 5)
        )))
        val results = stayService.getStaysInRange(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 12, 31)
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun `only stays within range are returned`() {
        stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 3, 1), checkOut = LocalDate.of(2026, 3, 5)),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2025, 6, 1), checkOut = LocalDate.of(2025, 6, 5))
        ))
        val results = stayService.getStaysInRange(
            from = LocalDate.of(2026, 1, 1),
            to = LocalDate.of(2026, 12, 31)
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `upsertStays returns correct dates and primaryGuestName`() {
        val response = stayService.upsertStays(listOf(upsertStayRequest(
            primaryGuestName = "Bob",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3)
        )))[0]
        assertEquals(LocalDate.of(2026, 6, 1), response.checkIn)
        assertEquals(LocalDate.of(2026, 6, 3), response.checkOut)
        assertEquals("Bob", response.primaryGuestName)
    }

    @Test
    fun `upsertStays returns externalId`() {
        val response = stayService.upsertStays(listOf(upsertStayRequest(
            externalId = 1001L,
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3)
        )))[0]
        assertEquals(1001L, response.externalId)
    }

    @Test
    fun `upsertStays creates guest and links to stay`() {
        val result = stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))[0]
        assertNotNull(result.guest)
        assertEquals(5001L, result.guest!!.externalId)
        assertEquals("Alice Smith", result.guest.name)
    }

    @Test
    fun `upsertStays updates stay fields on second call`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5))))
        val result = stayService.upsertStays(listOf(UpsertStayRequest(
            externalId = 1001L,
            primaryGuestName = "Alice",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            additionalGuestName = "Bob Smith",
            specialAccommodations = "Ground floor",
            dietaryRestrictions = "Gluten free",
            arrivalTime = "3:00 PM",
            housekeepingNotes = "No disturbance",
            reasonForStay = "Anniversary",
            invoice = CreateInvoiceRequest(
                items = listOf(InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1))),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            ),
            guest = fullGuestData()
        )))[0]
        assertEquals("Bob Smith", result.additionalGuestName)
        assertEquals("Ground floor", result.specialAccommodations)
        assertEquals("Gluten free", result.dietaryRestrictions)
        assertEquals("3:00 PM", result.arrivalTime)
        assertEquals("No disturbance", result.housekeepingNotes)
        assertEquals("Anniversary", result.reasonForStay)
    }

    @Test
    fun `upsertStays links guest on second call`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5))))
        val result = stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))[0]
        assertNotNull(result.guest)
        assertEquals("Alice Smith", result.guest!!.name)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertStays updates existing guest name and notes`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), guest = fullGuestData())))
        val result = stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData().copy(name = "Alice Updated", notes = "New note")
        )))[0]
        assertEquals("Alice Updated", result.guest!!.name)
        assertEquals("New note", result.guest.notes)
        assertEquals(1, guestRepository.count())
    }

    @Test
    fun `upsertStays accumulates new phones emails and addresses on existing guest`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), guest = fullGuestData())))
        val result = stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData().copy(
                phones = listOf(PhoneRequest("555-100-0001"), PhoneRequest("555-999-9999")),
                emails = listOf(EmailRequest("alice@example.com"), EmailRequest("alice2@example.com")),
                addresses = listOf(
                    AddressRequest("123 Main St", "Springfield", "IL", "62701", "US"),
                    AddressRequest("456 Oak Ave", "Springfield", "IL", "62702", "US")
                )
            )
        )))[0]
        assertEquals(2, result.guest!!.phones.size)
        assertEquals(2, result.guest.emails.size)
        assertEquals(2, result.guest.addresses.size)
    }

    @Test
    fun `upsertStays does not duplicate existing phones emails or addresses`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), guest = fullGuestData())))
        val result = stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))[0]
        assertEquals(1, result.guest!!.phones.size)
        assertEquals(1, result.guest.emails.size)
        assertEquals(1, result.guest.addresses.size)
    }

    @Test
    fun `upsertStays handles batch`() {
        val results = stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), guest = fullGuestData(5001L)),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 7, 1), checkOut = LocalDate.of(2026, 7, 5), guest = fullGuestData(5002L))
        ))
        assertEquals(2, results.size)
        assertEquals(2, guestRepository.count())
    }

    @Test
    fun `cancelStay sets status to CANCELED`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5))))
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
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5))))
        val results = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1, results.size)
    }

    @Test
    fun `getStaysBriefing excludes canceled stays`() {
        stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 7, 1), checkOut = LocalDate.of(2026, 7, 5))
        ))
        stayService.cancelStay(1002L)
        val results = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1, results.size)
    }

    @Test
    fun `getStaysBriefing excludes stays outside range`() {
        stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2025, 6, 1), checkOut = LocalDate.of(2025, 6, 5))
        ))
        val results = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1, results.size)
    }

    @Test
    fun `getStaysBriefing returns correct fields`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals("Alice", result.primaryGuestName)
        assertEquals(LocalDate.of(2026, 6, 1), result.checkIn)
        assertEquals(LocalDate.of(2026, 6, 5), result.checkOut)
        assertEquals(4L, result.nights)
        assertEquals(listOf(RoomNights("Jade Vine Suite", 1)), result.rooms)
        assertEquals(emptyList<String>(), result.addons)
        assertEquals("Prefers extra towels", result.guestNotes)
        assertEquals(listOf("5551000001"), result.phones)
    }

    @Test
    fun `getStaysBriefing returns addons for non-room items`() {
        stayService.upsertStays(listOf(UpsertStayRequest(
            externalId = 1001L,
            primaryGuestName = "Alice",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            invoice = CreateInvoiceRequest(
                items = listOf(
                    InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1)),
                    InvoiceItemRequest("Mugs", null, 2, BigDecimal("20.00"), LocalDate.of(2026, 6, 1))
                ),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            )
        )))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(listOf("Mugs"), result.addons)
    }

    @Test
    fun `getStaysBriefing returns zero previousStayCount for first time guest`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(0, result.previousStayCount)
        assertNull(result.lastStay)
    }

    @Test
    fun `getStaysBriefing returns correct history for returning guest`() {
        stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2025, 1, 1), checkOut = LocalDate.of(2025, 1, 3), guest = fullGuestData()),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), guest = fullGuestData())
        ))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(1, result.previousStayCount)
        assertEquals(listOf("Jade Vine Suite"), result.lastStay?.rooms)
        assertEquals(LocalDate.of(2025, 1, 1), result.lastStay?.checkIn)
    }

    @Test
    fun `getStaysWithoutGuest returns stays with no guest`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            externalId = 1001L,
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5)
        )))
        val results = stayService.getStaysWithoutGuest()
        assertEquals(1, results.size)
        assertEquals(null, results[0].guest)
    }

    private fun briefingRequestWithRoom(roomName: String, nights: Int = 2) = UpsertStayRequest(
        externalId = 1001L,
        primaryGuestName = "Test Guest",
        checkIn = LocalDate.of(2026, 6, 1),
        checkOut = LocalDate.of(2026, 6, 1).plusDays(nights.toLong()),
        invoice = CreateInvoiceRequest(
            items = (1..nights).map { i ->
                InvoiceItemRequest("Room", roomName, 1, BigDecimal("150.00"), LocalDate.of(2026, 6, i))
            },
            stateTax = BigDecimal("0.06"),
            countyTax = BigDecimal("0.01")
        )
    )

    @Test
    fun `getStaysBriefing expands combo room into individual rooms`() {
        stayService.upsertStays(listOf(briefingRequestWithRoom("Dogwood-Maple Suite", 2)))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(listOf(RoomNights("Dogwood Suite", 2), RoomNights("Maple Suite", 2)), result.rooms)
    }

    @Test
    fun `getStaysBriefing expands whole main house into all rooms`() {
        stayService.upsertStays(listOf(briefingRequestWithRoom("Whole Main House", 3)))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(
            listOf(
                RoomNights("Dogwood Suite", 3),
                RoomNights("Jade Vine Suite", 3),
                RoomNights("Gum Tree Suite", 3),
                RoomNights("Maple Suite", 3),
            ),
            result.rooms
        )
    }

    @Test
    fun `getStaysBriefing leaves non-combo room name unchanged`() {
        stayService.upsertStays(listOf(briefingRequestWithRoom("Jade Vine Suite", 2)))
        val result = stayService.getStaysBriefing(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))[0]
        assertEquals(listOf(RoomNights("Jade Vine Suite", 2)), result.rooms)
    }
}
