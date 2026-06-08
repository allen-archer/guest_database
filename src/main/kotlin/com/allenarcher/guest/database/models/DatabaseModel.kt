package com.allenarcher.guest.database.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

enum class StayStatus { SCHEDULED, CANCELED }

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
    var externalId: Long?,
    @Column(unique = true)
    var confirmationCode: String?,
    var primaryGuestName: String,
    @Enumerated(EnumType.STRING)
    var status: StayStatus = StayStatus.SCHEDULED,
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
    var type: String,
    var name: String?,
    var quantity: Int,
    var amount: BigDecimal,
    var date: LocalDate
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
    var country: String,
    var addedAt: LocalDate = LocalDate.now()
) {
    override fun equals(other: Any?) = other is Address &&
        street == other.street && city == other.city && state == other.state && zip == other.zip && country == other.country
    override fun hashCode() = arrayOf(street, city, state, zip, country).contentHashCode()
}