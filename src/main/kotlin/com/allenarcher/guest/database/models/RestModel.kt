package com.allenarcher.guest.database.models

import java.math.BigDecimal
import java.time.LocalDate

data class CreateGuestRequest(
    val name: String,
    val externalId: Long,
    val notes: String? = null,
    val phones: List<PhoneRequest> = emptyList(),
    val emails: List<EmailRequest> = emptyList(),
    val addresses: List<AddressRequest> = emptyList()
)

data class CreateStayRequest(
    val externalId: Long,
    val primaryGuestName: String,
    val status: StayStatus = StayStatus.SCHEDULED,
    val additionalGuestName: String? = null,
    val specialAccommodations: String? = null,
    val dietaryRestrictions: String? = null,
    val arrivalTime: String? = null,
    val housekeepingNotes: String? = null,
    val reasonForStay: String? = null,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val invoice: CreateInvoiceRequest
)

data class CreateInvoiceRequest(
    val items: List<InvoiceItemRequest>,
    val stateTax: BigDecimal,
    val countyTax: BigDecimal
)

data class EnrichStayRequest(
    val stay: EnrichStayData,
    val guest: EnrichGuestData
)

data class EnrichStayData(
    val externalId: Long,
    val additionalGuestName: String? = null,
    val specialAccommodations: String? = null,
    val dietaryRestrictions: String? = null,
    val arrivalTime: String? = null,
    val housekeepingNotes: String? = null,
    val reasonForStay: String? = null
)

data class EnrichGuestData(
    val externalId: Long,
    val name: String,
    val notes: String? = null,
    val phones: List<PhoneRequest> = emptyList(),
    val emails: List<EmailRequest> = emptyList(),
    val addresses: List<AddressRequest> = emptyList()
)

data class InvoiceItemRequest(val type: String, val name: String?, val quantity: Int, val amount: BigDecimal, val date: LocalDate)

data class PhoneRequest(val number: String)

data class EmailRequest(val address: String)

data class AddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val country: String
)

data class GuestResponse(
    val id: Long,
    val externalId: Long,
    val name: String,
    val notes: String?,
    val phones: List<PhoneResponse>,
    val emails: List<EmailResponse>,
    val addresses: List<AddressResponse>
)

data class GuestHistoryResponse(
    val previousStayCount: Int,
    val lastStay: LastStayResponse?
)

data class LastStayResponse(
    val room: String?,
    val checkIn: LocalDate,
    val checkOut: LocalDate
)

data class StayResponse(
    val id: Long,
    val externalId: Long,
    val primaryGuestName: String,
    val status: StayStatus,
    val additionalGuestName: String?,
    val specialAccommodations: String?,
    val dietaryRestrictions: String?,
    val arrivalTime: String?,
    val housekeepingNotes: String?,
    val reasonForStay: String?,
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

data class InvoiceItemResponse(val type: String, val name: String?, val quantity: Int, val amount: BigDecimal, val date: LocalDate)

data class PhoneResponse(val number: String, val addedAt: LocalDate)

data class EmailResponse(val address: String, val addedAt: LocalDate)

data class AddressResponse(
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val country: String,
    val addedAt: LocalDate
)