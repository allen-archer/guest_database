package com.allenarcher.guest.database.models

import java.time.temporal.ChronoUnit

fun Stay.toResponse() = StayResponse(
    id = id!!,
    externalId = externalId,
    confirmationCode = confirmationCode,
    primaryGuestName = primaryGuestName,
    status = status,
    additionalGuestName = additionalGuestName,
    specialAccommodations = specialAccommodations,
    dietaryRestrictions = dietaryRestrictions,
    arrivalTime = arrivalTime,
    housekeepingNotes = housekeepingNotes,
    reasonForStay = reasonForStay,
    checkIn = checkIn,
    checkOut = checkOut,
    guest = guest?.toResponse(),
    invoice = invoice!!.toResponse()
)

fun InvoiceItem.toResponse() = InvoiceItemResponse(type = type, name = name, quantity = quantity, amount = amount, date = date)

fun Invoice.toResponse() = InvoiceResponse(
    id = id!!,
    items = items.map { it.toResponse() },
    stateTax = stateTax,
    countyTax = countyTax,
)

fun Phone.toResponse() = PhoneResponse(number = number, addedAt = addedAt)

fun Email.toResponse() = EmailResponse(address = address, addedAt = addedAt)

fun Address.toResponse() = AddressResponse(street = street, city = city, state = state, zip = zip, country = country, addedAt = addedAt)

fun Stay.toBriefingResponse(previousStays: List<Stay>) = StayBriefingResponse(
    primaryGuestName = primaryGuestName,
    additionalGuestName = additionalGuestName,
    specialAccommodations = specialAccommodations,
    dietaryRestrictions = dietaryRestrictions,
    arrivalTime = arrivalTime,
    housekeepingNotes = housekeepingNotes,
    reasonForStay = reasonForStay,
    checkIn = checkIn,
    checkOut = checkOut,
    nights = ChronoUnit.DAYS.between(checkIn, checkOut),
    room = invoice?.items?.firstOrNull { it.type == "Room" }?.name,
    guestNotes = guest?.notes,
    phones = guest?.phones?.map { it.number } ?: emptyList(),
    previousStayCount = previousStays.size,
    lastStay = previousStays.firstOrNull()?.let {
        LastStayResponse(
            room = it.invoice?.items?.firstOrNull { item -> item.type == "Room" }?.name,
            checkIn = it.checkIn,
            checkOut = it.checkOut
        )
    }
)

fun Guest.toResponse() = GuestResponse(
    id = id!!,
    externalId = externalId,
    name = name,
    notes = notes,
    phones = phones.map { it.toResponse() },
    emails = emails.map { it.toResponse() },
    addresses = addresses.map { it.toResponse() }
)