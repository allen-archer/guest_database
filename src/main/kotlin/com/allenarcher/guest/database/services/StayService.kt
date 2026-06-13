package com.allenarcher.guest.database.services

import com.allenarcher.guest.database.GuestDatabaseProperties
import com.allenarcher.guest.database.GuestRepository
import com.allenarcher.guest.database.StayRepository
import com.allenarcher.guest.database.models.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StayService(
    private val stayRepository: StayRepository,
    private val guestRepository: GuestRepository,
    private val properties: GuestDatabaseProperties
) {
    @Transactional
    fun upsertStays(requests: List<UpsertStayRequest>): List<StayResponse> =
        requests.map { request ->
            val stay = stayRepository.findByExternalId(request.externalId)
                ?: (request.confirmationCode?.let { stayRepository.findByConfirmationCode(it) } ?: request.toDatabase())
            stay.externalId = request.externalId
            stay.confirmationCode = request.confirmationCode
            stay.primaryGuestName = request.primaryGuestName
            stay.status = request.status
            stay.additionalGuestName = request.additionalGuestName
            stay.specialAccommodations = request.specialAccommodations
            stay.dietaryRestrictions = request.dietaryRestrictions
            stay.arrivalTime = request.arrivalTime
            stay.housekeepingNotes = request.housekeepingNotes
            stay.reasonForStay = request.reasonForStay
            stay.checkIn = request.checkIn
            stay.checkOut = request.checkOut
            stay.invoice?.also {
                it.stateTax = request.invoice.stateTax
                it.countyTax = request.invoice.countyTax
                it.items.clear()
                it.items.addAll(request.invoice.items.map { item -> item.toDatabase() })
            } ?: run { stay.invoice = request.invoice.toDatabase(stay) }
            request.guest?.let { guestData ->
                val guest = guestRepository.findByExternalId(guestData.externalId)
                    ?.also { // If guest was found in the database, use it and update it with any new data
                        // Name and notes are replaced with new data
                        it.name = guestData.name
                        it.notes = guestData.notes
                        // Phones, emails, and addresses accumulate any new data instead of replacing
                        it.phones.addAll(guestData.phones.map { p -> p.toDatabase() }.filterNot { p -> p in it.phones })
                        it.emails.addAll(guestData.emails.map { e -> e.toDatabase() }.filterNot { e -> e in it.emails })
                        it.addresses.addAll(guestData.addresses.map { a -> a.toDatabase() }.filterNot { a -> a in it.addresses })
                    }
                    ?: guestData.toDatabase() // If no guest found in database, create a new one
                stay.guest = guestRepository.save(guest)
                // Save the guest
            }
            stayRepository.save(stay).toResponse()
        }

    @Transactional(readOnly = true)
    fun getStaysInRange(from: LocalDate, to: LocalDate): List<StayResponse> =
        stayRepository.findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(from, to)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getStaysBriefing(from: LocalDate, to: LocalDate): List<StayBriefingResponse> =
        stayRepository.findByCheckInGreaterThanEqualAndCheckOutLessThanEqualAndStatus(from, to, StayStatus.SCHEDULED)
            .map { stay ->
                val previousStays = stay.guest?.let { guest ->
                    stayRepository.findByGuest_ExternalIdAndStatusNotAndCheckOutBeforeOrderByCheckInDesc(guest.externalId, StayStatus.CANCELED, stay.checkIn)
                } ?: emptyList()
                stay.toBriefingResponse(previousStays, properties.roomCombos)
            }

    @Transactional(readOnly = true)
    fun getStaysWithoutGuest(): List<StayResponse> =
        stayRepository.findByGuestIsNull().map { it.toResponse() }

    @Transactional
    fun upsertByConfirmation(request: UpsertByConfirmationRequest): StayResponse {
        val existing = stayRepository.findByConfirmationCode(request.confirmationCode)
            ?: return stayRepository.save(request.toDatabase()).toResponse()

        val requestItems = request.invoice.items.map { it.toDatabase() }
        val inv = existing.invoice
        val changed = existing.primaryGuestName != request.primaryGuestName ||
            existing.checkIn != request.checkIn ||
            existing.checkOut != request.checkOut ||
            inv == null || inv.stateTax != request.invoice.stateTax || inv.countyTax != request.invoice.countyTax || inv.items != requestItems

        if (!changed) return existing.toResponse()

        existing.primaryGuestName = request.primaryGuestName
        existing.checkIn = request.checkIn
        existing.checkOut = request.checkOut
        inv?.also { it.stateTax = request.invoice.stateTax; it.countyTax = request.invoice.countyTax; it.items.clear(); it.items.addAll(requestItems) }
            ?: run { existing.invoice = request.invoice.toDatabase(existing) }

        return stayRepository.save(existing).toResponse()
    }

    @Transactional
    fun updateInvoice(request: UpdateInvoiceRequest): StayResponse {
        val stay = stayRepository.findByConfirmationCode(request.confirmationId)
            ?: throw IllegalArgumentException("Stay not found: confirmationId=${request.confirmationId}")
        val items = request.invoice.items.map { it.toDatabase() }
        stay.invoice?.also {
            it.stateTax = request.invoice.stateTax
            it.countyTax = request.invoice.countyTax
            it.items.clear()
            it.items.addAll(items)
        } ?: run { stay.invoice = request.invoice.toDatabase(stay) }
        val dates = items.map { it.date }
        stay.checkIn = dates.min()
        stay.checkOut = dates.max().plusDays(1)
        return stayRepository.save(stay).toResponse()
    }

    @Transactional
    fun cancelStay(rooms: List<RoomDateRequest>): StayResponse {
        val requestedCounts = rooms.groupingBy { it.room }.eachCount()
        val stay = rooms.map { it.date }.distinct()
            .flatMap { stayRepository.findByCheckInEquals(it) }
            .find { stay ->
                val stayCounts = stay.invoice?.items
                    ?.filter { it.type == "Room" }
                    ?.groupingBy { it.name ?: "" }
                    ?.eachCount() ?: emptyMap()
                stayCounts == requestedCounts
            } ?: throw IllegalArgumentException("No stay found with room nights exactly matching: $requestedCounts")
        stay.status = StayStatus.CANCELED
        return stayRepository.save(stay).toResponse()
    }

    fun clear() {
        stayRepository.deleteAll()
    }
}