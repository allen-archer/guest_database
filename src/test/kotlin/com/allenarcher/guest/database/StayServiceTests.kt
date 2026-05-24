package com.allenarcher.guest.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private fun savedGuest(name: String) = guestRepository.save(Guest(name = name))

    private fun createStayRequest(
        guestIds: List<Long>,
        checkIn: LocalDate,
        checkOut: LocalDate
    ) = CreateStayRequest(
        checkIn = checkIn,
        checkOut = checkOut,
        guestIds = guestIds,
        invoice = CreateInvoiceRequest(
            items = listOf(InvoiceItemRequest("Room", BigDecimal("150.00"))),
            stateTax = BigDecimal("0.06"),
            countyTax = BigDecimal("0.01")
        )
    )

    @Test
    fun `stay within range is returned`() {
        val guest = savedGuest("Alice")
        stayService.createStay(createStayRequest(
            guestIds = listOf(guest.id!!),
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
        val guest = savedGuest("Alice")
        stayService.createStay(createStayRequest(
            guestIds = listOf(guest.id!!),
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
        val guest = savedGuest("Alice")
        stayService.createStay(createStayRequest(
            guestIds = listOf(guest.id!!),
            checkIn = LocalDate.of(2026, 3, 1),
            checkOut = LocalDate.of(2026, 3, 5)
        ))
        stayService.createStay(createStayRequest(
            guestIds = listOf(guest.id!!),
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
    fun `createStay returns correct dates and guests`() {
        val alice = savedGuest("Alice")
        val bob = savedGuest("Bob")
        val response = stayService.createStay(createStayRequest(
            guestIds = listOf(alice.id!!, bob.id!!),
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3)
        ))
        assertEquals(LocalDate.of(2026, 6, 1), response.checkIn)
        assertEquals(LocalDate.of(2026, 6, 3), response.checkOut)
        assertEquals(2, response.guests.size)
        assertTrue(response.guests.any { it.name == "Alice" })
        assertTrue(response.guests.any { it.name == "Bob" })
    }

    @Test
    fun `createStay invoice is unpaid by default`() {
        val guest = savedGuest("Alice")
        val response = stayService.createStay(createStayRequest(
            guestIds = listOf(guest.id!!),
            checkIn = LocalDate.of(2026, 6, 1),
            checkOut = LocalDate.of(2026, 6, 3)
        ))
        assertFalse(response.invoice.paid)
        assertEquals(BigDecimal("150.00"), response.invoice.items.first().price)
    }

    @Test
    fun `createStay throws when guest id does not exist`() {
        assertThrows(IllegalArgumentException::class.java) {
            stayService.createStay(createStayRequest(
                guestIds = listOf(99999L),
                checkIn = LocalDate.of(2026, 6, 1),
                checkOut = LocalDate.of(2026, 6, 3)
            ))
        }
    }
}