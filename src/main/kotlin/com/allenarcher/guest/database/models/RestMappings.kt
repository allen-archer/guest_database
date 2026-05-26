package com.allenarcher.guest.database.models

fun Stay.toResponse() = StayResponse(
    id = id!!,
    externalId = externalId,
    primaryGuestName = primaryGuestName,
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

fun InvoiceItem.toResponse() = InvoiceItemResponse(name = name, price = price)

fun Invoice.toResponse() = InvoiceResponse(
    id = id!!,
    items = items.map { it.toResponse() },
    stateTax = stateTax,
    countyTax = countyTax,
)

fun Guest.toResponse() = GuestResponse(
    id = id!!,
    externalId = externalId,
    name = name,
    notes = notes,
    phones = phones.map { PhoneResponse(number = it.number, addedAt = it.addedAt) },
    emails = emails.map { EmailResponse(address = it.address, addedAt = it.addedAt) },
    addresses = addresses.map { AddressResponse(street = it.street, city = it.city, state = it.state, zip = it.zip, addedAt = it.addedAt) }
)