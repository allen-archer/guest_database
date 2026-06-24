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
        return GuestHistoryResponse(previousStayCount = stays.size, lastStay = stays.firstOrNull()?.toLastStayResponse())
    }

    @Transactional(readOnly = true)
    fun searchGuests(
        name: String?,
        email: String?,
        city: String?,
        state: String?,
        street: String?,
        phone: String?,
        zip: String?
    ): List<GuestSearchResponse> {
        val digits = phone?.replace(Regex("[^0-9]"), "")?.takeIf { it.isNotEmpty() }
        return guestRepository.searchGuests(
            name = name?.takeIf { it.isNotBlank() },
            email = email?.takeIf { it.isNotBlank() },
            city = city?.takeIf { it.isNotBlank() },
            state = state?.takeIf { it.isNotBlank() },
            street = street?.takeIf { it.isNotBlank() },
            phone = digits,
            zip = zip?.takeIf { it.isNotBlank() }
        ).map { guest ->
            val stays = stayRepository.findByGuest_ExternalIdAndStatusNotAndCheckOutBeforeOrderByCheckInDesc(guest.externalId, StayStatus.CANCELED, LocalDate.now())
            guest.toSearchResponse(stays.size, stays.firstOrNull()?.toLastStayResponse())
        }
    }

    fun clear() {
        guestRepository.deleteAll()
    }
}
