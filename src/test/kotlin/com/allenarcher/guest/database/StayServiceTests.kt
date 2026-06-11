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
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
class StayServiceTests {

    @Autowired lateinit var stayService: StayService
    @Autowired lateinit var stayRepository: StayRepository
    @Autowired lateinit var guestRepository: GuestRepository
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

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
        roomName: String = "Jade Vine Suite",
        guest: UpsertGuestData? = null
    ) = UpsertStayRequest(
        externalId = externalId,
        confirmationCode = confirmationCode,
        primaryGuestName = primaryGuestName,
        checkIn = checkIn,
        checkOut = checkOut,
        invoice = CreateInvoiceRequest(
            items = listOf(InvoiceItemRequest("Room", roomName, 1, BigDecimal("150.00"), checkIn)),
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
                items = listOf(InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), date = LocalDate.of(2026, 6, 1))),
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
    fun `upsertStays enriches stay created by confirmation email`() {
        stayService.upsertByConfirmation(confirmationRequest(confirmationCode = "CONF001"))
        val result = stayService.upsertStays(listOf(upsertStayRequest(
            externalId = 9001L,
            confirmationCode = "CONF001",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))[0]
        assertEquals(1, stayRepository.count())
        assertEquals(9001L, result.externalId)
        assertNotNull(result.guest)
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
        val result = stayService.cancelStay(listOf(RoomDateRequest(room = "Jade Vine Suite", date = LocalDate.of(2026, 6, 1))))
        assertEquals(StayStatus.CANCELED, result.status)
    }

    @Test
    fun `cancelStay throws when no stay matches`() {
        stayService.upsertStays(listOf(upsertStayRequest(checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5))))
        assertThrows(IllegalArgumentException::class.java) {
            stayService.cancelStay(listOf(RoomDateRequest(room = "Nonexistent Room", date = LocalDate.of(2026, 6, 1))))
        }
    }

    @Test
    fun `cancelStay does not cancel a stay with a different room name`() {
        stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), roomName = "Jade Vine Suite"),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5), roomName = "Dogwood Suite")
        ))
        stayService.cancelStay(listOf(RoomDateRequest(room = "Jade Vine Suite", date = LocalDate.of(2026, 6, 1))))
        val remaining = stayService.getStaysInRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(StayStatus.CANCELED, remaining.first { it.externalId == 1001L }.status)
        assertEquals(StayStatus.SCHEDULED, remaining.first { it.externalId == 1002L }.status)
    }

    @Test
    fun `cancelStay does not cancel a stay with a different check-in date`() {
        stayService.upsertStays(listOf(
            upsertStayRequest(externalId = 1001L, checkIn = LocalDate.of(2026, 6, 1), checkOut = LocalDate.of(2026, 6, 5)),
            upsertStayRequest(externalId = 1002L, checkIn = LocalDate.of(2026, 7, 1), checkOut = LocalDate.of(2026, 7, 5))
        ))
        stayService.cancelStay(listOf(RoomDateRequest(room = "Jade Vine Suite", date = LocalDate.of(2026, 6, 1))))
        val remaining = stayService.getStaysInRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(StayStatus.CANCELED, remaining.first { it.externalId == 1001L }.status)
        assertEquals(StayStatus.SCHEDULED, remaining.first { it.externalId == 1002L }.status)
    }

    @Test
    fun `cancelStay throws when room nights do not exactly match`() {
        stayService.upsertStays(listOf(UpsertStayRequest(
            externalId = 1001L,
            primaryGuestName = "Alice",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3),
            invoice = CreateInvoiceRequest(
                items = listOf(
                    InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1)),
                    InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 2))
                ),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            )
        )))
        assertThrows(IllegalArgumentException::class.java) {
            stayService.cancelStay(listOf(
                RoomDateRequest(room = "Jade Vine Suite", date = LocalDate.of(2026, 6, 1))
            ))
        }
    }

    @Test
    fun `cancelStay cancels a multi-room stay`() {
        stayService.upsertStays(listOf(UpsertStayRequest(
            externalId = 1001L,
            primaryGuestName = "Alice",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            invoice = CreateInvoiceRequest(
                items = listOf(
                    InvoiceItemRequest("Room", "Jade Vine Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1)),
                    InvoiceItemRequest("Room", "Gum Tree Suite", 1, BigDecimal("150.00"), LocalDate.of(2026, 6, 1))
                ),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            )
        )))
        val result = stayService.cancelStay(listOf(
            RoomDateRequest(room = "Jade Vine Suite", LocalDate.of(2026, 6, 1)),
            RoomDateRequest(room = "Gum Tree Suite", LocalDate.of(2026, 6, 1))
        ))
        assertEquals(StayStatus.CANCELED, result.status)
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
        stayService.cancelStay(listOf(RoomDateRequest(room = "Jade Vine Suite", date = LocalDate.of(2026, 7, 1))))
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

    private fun stayWithConfirmationCode(confirmationCode: String, rooms: List<InvoiceItemRequest>) = UpsertStayRequest(
        externalId = 1001L,
        primaryGuestName = "Alice",
        confirmationCode = confirmationCode,
        checkIn = LocalDate.of(2026, 6, 1),
        checkOut = LocalDate.of(2026, 6, 1).plusDays(rooms.size.toLong()),
        invoice = CreateInvoiceRequest(items = rooms, stateTax = BigDecimal("0.06"), countyTax = BigDecimal("0.01"))
    )

    private fun roomItem(name: String, date: LocalDate) =
        InvoiceItemRequest("Room", name, 1, BigDecimal("150.00"), date)

    @Test
    fun `updateInvoice replaces invoice items`() {
        stayService.upsertStays(listOf(stayWithConfirmationCode("CONF001", listOf(
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 1))
        ))))
        val result = stayService.updateInvoice(UpdateInvoiceRequest(
            confirmationId = "CONF001",
            invoice = CreateInvoiceRequest(
                items = listOf(
                    roomItem("Dogwood Suite", LocalDate.of(2026, 6, 1)),
                    roomItem("Dogwood Suite", LocalDate.of(2026, 6, 2))
                ),
                stateTax = BigDecimal("12.00"),
                countyTax = BigDecimal("4.00")
            )
        ))
        assertEquals(2, result.invoice.items.size)
        assertTrue(result.invoice.items.all { it.name == "Dogwood Suite" })
        assertEquals(BigDecimal("12.00"), result.invoice.stateTax)
        assertEquals(BigDecimal("4.00"), result.invoice.countyTax)
    }

    @Test
    fun `updateInvoice derives checkIn and checkOut from invoice item dates`() {
        stayService.upsertStays(listOf(stayWithConfirmationCode("CONF001", listOf(
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 1))
        ))))
        val result = stayService.updateInvoice(UpdateInvoiceRequest(
            confirmationId = "CONF001",
            invoice = CreateInvoiceRequest(
                items = listOf(
                    roomItem("Gum Tree Suite", LocalDate.of(2026, 8, 1)),
                    roomItem("Gum Tree Suite", LocalDate.of(2026, 8, 2))
                ),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            )
        ))
        assertEquals(LocalDate.of(2026, 8, 1), result.checkIn)
        assertEquals(LocalDate.of(2026, 8, 3), result.checkOut)
    }

    @Test
    fun `updateInvoice throws when confirmation code not found`() {
        assertThrows(IllegalArgumentException::class.java) {
            stayService.updateInvoice(UpdateInvoiceRequest(
                confirmationId = "NONEXISTENT",
                invoice = CreateInvoiceRequest(emptyList(), BigDecimal("0.00"), BigDecimal("0.00"))
            ))
        }
    }

    @Test
    fun `updateInvoice leaves no orphaned invoice items`() {
        stayService.upsertStays(listOf(stayWithConfirmationCode("CONF001", listOf(
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 1)),
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 2)),
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 3))
        ))))
        stayService.updateInvoice(UpdateInvoiceRequest(
            confirmationId = "CONF001",
            invoice = CreateInvoiceRequest(
                items = listOf(roomItem("Dogwood Suite", LocalDate.of(2026, 6, 1))),
                stateTax = BigDecimal("0.06"),
                countyTax = BigDecimal("0.01")
            )
        ))
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice_items", Int::class.java)!!
        assertEquals(1, count)
    }

    private fun confirmationRequest(
        confirmationCode: String = "CONF001",
        primaryGuestName: String = "Alice",
        checkIn: LocalDate = LocalDate.of(2026, 6, 1),
        checkOut: LocalDate = LocalDate.of(2026, 6, 5),
        items: List<InvoiceItemRequest> = listOf(roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 1))),
        stateTax: BigDecimal = BigDecimal("0.06"),
        countyTax: BigDecimal = BigDecimal("0.01")
    ) = UpsertByConfirmationRequest(
        confirmationCode = confirmationCode,
        primaryGuestName = primaryGuestName,
        checkIn = checkIn,
        checkOut = checkOut,
        invoice = CreateInvoiceRequest(items = items, stateTax = stateTax, countyTax = countyTax)
    )

    @Test
    fun `upsertByConfirmation creates new stay when confirmation code not found`() {
        val result = stayService.upsertByConfirmation(confirmationRequest())
        assertEquals("CONF001", result.confirmationCode)
        assertEquals("Alice", result.primaryGuestName)
        assertEquals(LocalDate.of(2026, 6, 1), result.checkIn)
        assertEquals(LocalDate.of(2026, 6, 5), result.checkOut)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation new stay has null externalId`() {
        val result = stayService.upsertByConfirmation(confirmationRequest())
        assertNull(result.externalId)
    }

    @Test
    fun `upsertByConfirmation returns existing stay unchanged when nothing changed`() {
        stayService.upsertByConfirmation(confirmationRequest())
        val dbCountBefore = stayRepository.count()
        stayService.upsertByConfirmation(confirmationRequest())
        assertEquals(dbCountBefore, stayRepository.count())
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation updates when primaryGuestName changes`() {
        stayService.upsertByConfirmation(confirmationRequest())
        val result = stayService.upsertByConfirmation(confirmationRequest(primaryGuestName = "Bob"))
        assertEquals("Bob", result.primaryGuestName)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation updates when checkIn changes`() {
        stayService.upsertByConfirmation(confirmationRequest())
        val result = stayService.upsertByConfirmation(confirmationRequest(checkIn = LocalDate.of(2026, 6, 2)))
        assertEquals(LocalDate.of(2026, 6, 2), result.checkIn)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation updates when checkOut changes`() {
        stayService.upsertByConfirmation(confirmationRequest())
        val result = stayService.upsertByConfirmation(confirmationRequest(checkOut = LocalDate.of(2026, 6, 7)))
        assertEquals(LocalDate.of(2026, 6, 7), result.checkOut)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation updates when invoice items change`() {
        stayService.upsertByConfirmation(confirmationRequest())
        val result = stayService.upsertByConfirmation(confirmationRequest(
            items = listOf(
                roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 1)),
                roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 2))
            )
        ))
        assertEquals(2, result.invoice.items.size)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation updates when tax changes`() {
        stayService.upsertByConfirmation(confirmationRequest())
        val result = stayService.upsertByConfirmation(confirmationRequest(stateTax = BigDecimal("9.00"), countyTax = BigDecimal("3.00")))
        assertEquals(BigDecimal("9.00"), result.invoice.stateTax)
        assertEquals(BigDecimal("3.00"), result.invoice.countyTax)
        assertEquals(1, stayRepository.count())
    }

    @Test
    fun `upsertByConfirmation does not overwrite externalId on existing stay`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            externalId = 1001L,
            confirmationCode = "CONF001",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5)
        )))
        val result = stayService.upsertByConfirmation(confirmationRequest(primaryGuestName = "Bob"))
        assertEquals(1001L, result.externalId)
    }

    @Test
    fun `upsertByConfirmation does not overwrite guest on existing stay`() {
        stayService.upsertStays(listOf(upsertStayRequest(
            externalId = 1001L,
            confirmationCode = "CONF001",
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 5),
            guest = fullGuestData()
        )))
        val result = stayService.upsertByConfirmation(confirmationRequest(primaryGuestName = "Bob"))
        assertNotNull(result.guest)
        assertEquals(5001L, result.guest!!.externalId)
    }

    @Test
    fun `upsertByConfirmation leaves no orphaned invoice items`() {
        stayService.upsertByConfirmation(confirmationRequest(items = listOf(
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 1)),
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 2)),
            roomItem("Jade Vine Suite", LocalDate.of(2026, 6, 3))
        )))
        stayService.upsertByConfirmation(confirmationRequest(items = listOf(
            roomItem("Dogwood Suite", LocalDate.of(2026, 6, 1))
        )))
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice_items", Int::class.java)!!
        assertEquals(1, count)
    }
}
