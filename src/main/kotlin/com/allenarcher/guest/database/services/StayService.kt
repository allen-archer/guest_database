package com.allenarcher.guest.database.services

import com.allenarcher.guest.database.GuestRepository
import com.allenarcher.guest.database.StayRepository
import com.allenarcher.guest.database.models.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StayService(
    private val stayRepository: StayRepository,
    private val guestRepository: GuestRepository
) {
    @Transactional
    fun createStay(request: CreateStayRequest): StayResponse =
        stayRepository.save(request.toDatabase()).toResponse()

    @Transactional(readOnly = true)
    fun getStaysInRange(from: LocalDate, to: LocalDate): List<StayResponse> =
        stayRepository.findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(from, to)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getStaysWithoutGuest(): List<StayResponse> =
        stayRepository.findByGuestIsNull().map { it.toResponse() }

    @Transactional
    fun enrichStays(requests: List<EnrichStayRequest>): List<StayResponse> =
        requests.map { request ->
            val guest = guestRepository.findByExternalId(request.guest.externalId)
                ?.also { // If guest was found in the database, use it and update it with any new data
                    // Name and notes are replaced with new data
                    it.name = request.guest.name
                    it.notes = request.guest.notes
                    // Phones, emails, and addresses accumulate any new data instead of replacing
                    it.phones.addAll(request.guest.phones.map { p -> p.toDatabase() }.filterNot { p -> p in it.phones })
                    it.emails.addAll(request.guest.emails.map { e -> e.toDatabase() }.filterNot { e -> e in it.emails })
                    it.addresses.addAll(request.guest.addresses.map { a -> a.toDatabase() }.filterNot { a -> a in it.addresses })
                }
                ?: request.guest.toDatabase() // If no guest found in database, create a new one
            // Save the guest
            guestRepository.save(guest)
            // Update the existing stay with new data
            val stay = stayRepository.findByExternalId(request.stay.externalId)
                ?: throw IllegalArgumentException("Stay not found: externalId=${request.stay.externalId}")
            stay.guest = guest
            stay.additionalGuestName = request.stay.additionalGuestName
            stay.specialAccommodations = request.stay.specialAccommodations
            stay.dietaryRestrictions = request.stay.dietaryRestrictions
            stay.arrivalTime = request.stay.arrivalTime
            stay.housekeepingNotes = request.stay.housekeepingNotes
            stay.reasonForStay = request.stay.reasonForStay
            stayRepository.save(stay).toResponse()
        }

    fun clear() {
        stayRepository.deleteAll()
    }
}