package com.allenarcher.guest.database.models

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

fun Guest.toResponse() = GuestResponse(
    id = id!!,
    externalId = externalId,
    name = name,
    notes = notes,
    phones = phones.map { it.toResponse() },
    emails = emails.map { it.toResponse() },
    addresses = addresses.map { it.toResponse() }
)