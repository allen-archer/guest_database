package com.allenarcher.guest.database.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class Guest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,
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
    var externalId: Long? = null,
    var checkIn: LocalDate,
    var checkOut: LocalDate,
    @ManyToMany
    @JoinTable(
        name = "stay_guests",
        joinColumns = [JoinColumn(name = "stay_id")],
        inverseJoinColumns = [JoinColumn(name = "guest_id")]
    )
    var guests: MutableList<Guest> = mutableListOf(),
    @OneToOne(mappedBy = "stay", cascade = [CascadeType.ALL])
    var invoice: Invoice? = null
)

@Entity
class Invoice(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var externalId: Long? = null,
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
)

@Embeddable
data class Email(
    var address: String,
    var addedAt: LocalDate = LocalDate.now()
)

@Embeddable
data class Address(
    var street: String,
    var city: String,
    var state: String,
    var zip: String,
    var addedAt: LocalDate = LocalDate.now()
)

fun Stay.toResponse() = StayResponse(
    id = id!!,
    externalId = externalId,
    checkIn = checkIn,
    checkOut = checkOut,
    guests = guests.map { it.toResponse() },
    invoice = invoice!!.toResponse()
)

fun Invoice.toResponse() = InvoiceResponse(
    id = id!!,
    externalId = externalId,
    items = items.map { InvoiceItemResponse(name = it.name, price = it.price) },
    stateTax = stateTax,
    countyTax = countyTax,
)

fun Guest.toResponse() = GuestResponse(
    id = id!!,
    name = name,
    phones = phones.map { PhoneResponse(number = it.number, addedAt = it.addedAt) },
    emails = emails.map { EmailResponse(address = it.address, addedAt = it.addedAt) },
    addresses = addresses.map { AddressResponse(street = it.street, city = it.city, state = it.state, zip = it.zip, addedAt = it.addedAt) }
)