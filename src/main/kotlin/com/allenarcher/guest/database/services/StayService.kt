package com.allenarcher.guest.database.services

import com.allenarcher.guest.database.*
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
    fun createStay(request: CreateStayRequest): StayResponse {
        val guests = guestRepository.findAllById(request.guestIds).toMutableList()
        if (guests.size != request.guestIds.size) {
            throw IllegalArgumentException("One or more guest IDs not found")
        }
        return stayRepository.save(request.toDatabase(guests)).toResponse()
    }

    @Transactional(readOnly = true)
    fun getStaysInRange(from: LocalDate, to: LocalDate): List<StayResponse> =
        stayRepository.findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(from, to)
            .map { it.toResponse() }

    fun clear() {
        stayRepository.deleteAll()
    }
}