package com.allenarcher.guest.database.services

import com.allenarcher.guest.database.GuestRepository
import com.allenarcher.guest.database.models.*
import org.springframework.stereotype.Service

@Service
class GuestService(private val guestRepository: GuestRepository) {
    fun createGuest(request: CreateGuestRequest): GuestResponse {
        val guest = guestRepository.save(request.toDatabase())
        return guest.toResponse()
    }

    fun clear() {
        guestRepository.deleteAll()
    }
}
