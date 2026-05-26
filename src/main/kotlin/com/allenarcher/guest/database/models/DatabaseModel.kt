package com.allenarcher.guest.database.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class Guest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(unique = true)
    var externalId: Long,
    var name: String,
    var notes: String? = null,
    @ElementCollection
    @CollectionTable(name = "guest_phones", joinColumns = [JoinColumn(name = "guest_id")])
    var phones: MutableList<Phone> = mutableListOf(),
    @ElementCollection
    @CollectionTable(name = "guest_emails", joinColumns = [JoinColumn(name = "guest_id")])
    var emails: MutableList<Email> = mutableListOf(),
    @ElementCollection
    @CollectionTable(name = "guest_addresses", joinColumns = [JoinColumn(name = "guest_id")])
    var addresses: MutableList<Address> = mutableListOf()
)

@Entity
class Stay(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(unique = true)
    var externalId: Long,
    var primaryGuestName: String,
    var additionalGuestName: String? = null,
    var specialAccommodations: String? = null,
    var dietaryRestrictions: String? = null,
    var arrivalTime: String? = null,
    var housekeepingNotes: String? = null,
    var reasonForStay: String? = null,
    var checkIn: LocalDate,
    var checkOut: LocalDate,
    @ManyToOne
    @JoinColumn(name = "guest_id")
    var guest: Guest? = null,
    @OneToOne(mappedBy = "stay", cascade = [CascadeType.ALL])
    var invoice: Invoice? = null
)

@Entity
class Invoice(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @OneToOne
    @JoinColumn(name = "stay_id")
    var stay: Stay,
    @ElementCollection
    @CollectionTable(name = "invoice_items", joinColumns = [JoinColumn(name = "invoice_id")])
    var items: MutableList<InvoiceItem> = mutableListOf(),
    var stateTax: BigDecimal,
    var countyTax: BigDecimal,
)

@Embeddable
data class InvoiceItem(
    var name: String,
    var price: BigDecimal
)

@Embeddable
data class Phone(
    var number: String,
    var addedAt: LocalDate = LocalDate.now()
) {
    override fun equals(other: Any?) = other is Phone && number == other.number
    override fun hashCode() = number.hashCode()
}

@Embeddable
data class Email(
    var address: String,
    var addedAt: LocalDate = LocalDate.now()
) {
    override fun equals(other: Any?) = other is Email && address == other.address
    override fun hashCode() = address.hashCode()
}

@Embeddable
data class Address(
    var street: String,
    var city: String,
    var state: String,
    var zip: String,
    var addedAt: LocalDate = LocalDate.now()
) {
    override fun equals(other: Any?) = other is Address &&
        street == other.street && city == other.city && state == other.state && zip == other.zip
    override fun hashCode() = arrayOf(street, city, state, zip).contentHashCode()
}

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

fun Invoice.toResponse() = InvoiceResponse(
    id = id!!,
    items = items.map { InvoiceItemResponse(name = it.name, price = it.price) },
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