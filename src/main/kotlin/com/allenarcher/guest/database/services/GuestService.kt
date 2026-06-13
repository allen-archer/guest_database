package com.allenarcher.guest.database.services

import com.allenarcher.guest.database.GuestRepository
import com.allenarcher.guest.database.StayRepository
import com.allenarcher.guest.database.models.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GuestService(
    private val guestRepository: GuestRepository,
    private val stayRepository: StayRepository
) {
    fun createGuest(request: CreateGuestRequest): GuestResponse {
        val guest = guestRepository.save(request.toDatabase())
        return guest.toResponse()
    }

    @Transactional(readOnly = true)
    fun getGuestHistory(guestExternalId: Long): GuestHistoryResponse {
        val stays = stayRepository.findByGuest_ExternalIdAndStatusNotAndCheckOutBeforeOrderByCheckInDesc(guestExternalId, StayStatus.CANCELED, LocalDate.now())
        val lastStay = stays.firstOrNull()?.let {
            LastStayResponse(
                rooms = it.invoice?.items?.filter { item -> item.type == "Room" }?.mapNotNull { item -> item.name }?.distinct() ?: emptyList(),
                checkIn = it.checkIn,
                checkOut = it.checkOut
            )
        }
        return GuestHistoryResponse(previousStayCount = stays.size, lastStay = lastStay)
    }

    fun clear() {
        guestRepository.deleteAll()
    }
}
