package com.allenarcher.guest.database

import org.springframework.stereotype.Service

@Service
class GuestService(private val guestRepository: GuestRepository) {

    fun createGuest(request: CreateGuestRequest): GuestResponse {
        val guest = guestRepository.save(Guest(name = request.name))
        return guest.toResponse()
    }
}

fun Guest.toResponse() = GuestResponse(id = id!!, name = name)