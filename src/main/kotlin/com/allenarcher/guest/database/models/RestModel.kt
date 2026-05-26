package com.allenarcher.guest.database.models

import java.math.BigDecimal
import java.time.LocalDate

data class CreateGuestRequest(
    val name: String,
    val externalId: Long,
    val phones: List<PhoneRequest> = emptyList(),
    val emails: List<EmailRequest> = emptyList(),
    val addresses: List<AddressRequest> = emptyList()
)

data class CreateStayRequest(
    val externalId: Long,
    val primaryGuestName: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val invoice: CreateInvoiceRequest
)

data class CreateInvoiceRequest(
    val items: List<InvoiceItemRequest>,
    val stateTax: BigDecimal,
    val countyTax: BigDecimal
)

data class InvoiceItemRequest(val name: String, val price: BigDecimal)

data class PhoneRequest(val number: String)

data class EmailRequest(val address: String)

data class AddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val zip: String
)

data class GuestResponse(
    val id: Long,
    val externalId: Long,
    val name: String,
    val phones: List<PhoneResponse>,
    val emails: List<EmailResponse>,
    val addresses: List<AddressResponse>
)

data class StayResponse(
    val id: Long,
    val externalId: Long,
    val primaryGuestName: String,
    val additionalGuestName: String?,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guest: GuestResponse?,
    val invoice: InvoiceResponse
)

data class InvoiceResponse(
    val id: Long,
    val items: List<InvoiceItemResponse>,
    val stateTax: BigDecimal,
    val countyTax: BigDecimal,
)

data class InvoiceItemResponse(val name: String, val price: BigDecimal)

data class PhoneResponse(val number: String, val addedAt: LocalDate)

data class EmailResponse(val address: String, val addedAt: LocalDate)

data class AddressResponse(
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val addedAt: LocalDate
)

fun CreateGuestRequest.toDatabase() = Guest(
    externalId = externalId,
    name = name,
    phones = phones.map { it.toDatabase() }.toMutableList(),
    emails = emails.map { it.toDatabase() }.toMutableList(),
    addresses = addresses.map { it.toDatabase() }.toMutableList()
)

fun PhoneRequest.toDatabase() = Phone(number = number)

fun EmailRequest.toDatabase() = Email(address = address)

fun AddressRequest.toDatabase() = Address(street = street, city = city, state = state, zip = zip)

fun InvoiceItemRequest.toDatabase() = InvoiceItem(name = name, price = price)

fun CreateInvoiceRequest.toDatabase(stay: Stay) = Invoice(
    stay = stay,
    items = items.map { it.toDatabase() }.toMutableList(),
    stateTax = stateTax,
    countyTax = countyTax
)

fun CreateStayRequest.toDatabase(): Stay {
    val stay = Stay(externalId = externalId, primaryGuestName = primaryGuestName, checkIn = checkIn, checkOut = checkOut)
    stay.invoice = invoice.toDatabase(stay)
    return stay
}