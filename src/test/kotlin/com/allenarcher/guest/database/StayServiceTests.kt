package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.*
import com.allenarcher.guest.database.services.StayService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @BeforeEach
    fun setUp() {
        stayRepository.deleteAll()
    }

    private fun createStayRequest(
        externalId: Long = 1001L,
        primaryGuestName: String = "Alice",
        checkIn: LocalDate,
        checkOut: LocalDate,
    ) = CreateStayRequest(
        externalId = externalId,
        primaryGuestName = primaryGuestName,
        checkIn = checkIn,
        checkOut = checkOut,
        invoice = CreateInvoiceRequest(
            items = listOf(InvoiceItemRequest("Room", BigDecimal("150.00"))),
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
