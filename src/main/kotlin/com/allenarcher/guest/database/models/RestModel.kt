package com.allenarcher.guest.database

import java.math.BigDecimal
import java.time.LocalDate

data class CreateGuestRequest(val name: String)

data class CreateStayRequest(
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestIds: List<Long>,
    val invoice: CreateInvoiceRequest
)

data class CreateInvoiceRequest(
    val items: List<InvoiceItemRequest>,
    val stateTax: BigDecimal,
    val countyTax: BigDecimal
)

data class InvoiceItemRequest(val name: String, val price: BigDecimal)

data class GuestResponse(val id: Long, val name: String)

data class StayResponse(
    val id: Long,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guests: List<GuestResponse>,
    val invoice: InvoiceResponse
)

data class InvoiceResponse(
    val id: Long,
    val items: List<InvoiceItemResponse>,
    val stateTax: BigDecimal,
    val countyTax: BigDecimal,
    val paid: Boolean
)

data class InvoiceItemResponse(val name: String, val price: BigDecimal)