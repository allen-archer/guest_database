package com.allenarcher.guest.database.models

fun EnrichGuestData.toDatabase() = Guest(
    externalId = externalId,
    name = name,
    notes = notes,
    phones = phones.map { it.toDatabase() }.toMutableList(),
    emails = emails.map { it.toDatabase() }.toMutableList(),
    addresses = addresses.map { it.toDatabase() }.toMutableList()
)

fun CreateGuestRequest.toDatabase() = Guest(
    externalId = externalId,
    name = name,
    notes = notes,
    phones = phones.map { it.toDatabase() }.toMutableList(),
    emails = emails.map { it.toDatabase() }.toMutableList(),
    addresses = addresses.map { it.toDatabase() }.toMutableList()
)

fun PhoneRequest.toDatabase() = Phone(number = number.replace(Regex("[^0-9]"), ""))

fun EmailRequest.toDatabase() = Email(address = address)

fun AddressRequest.toDatabase() = Address(street = street, city = city, state = state, zip = zip, country = country)

fun InvoiceItemRequest.toDatabase() = InvoiceItem(type = type, name = name, quantity = quantity, amount = amount, date = date)

fun CreateInvoiceRequest.toDatabase(stay: Stay) = Invoice(
    stay = stay,
    items = items.map { it.toDatabase() }.toMutableList(),
    stateTax = stateTax,
    countyTax = countyTax
)

fun CreateStayRequest.toDatabase(): Stay {
    val stay = Stay(
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
        checkOut = checkOut
    )
    stay.invoice = invoice.toDatabase(stay)
    return stay
}