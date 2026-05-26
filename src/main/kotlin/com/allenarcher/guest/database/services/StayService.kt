package com.allenarcher.guest.database.services

import com.allenarcher.guest.database.StayRepository
import com.allenarcher.guest.database.models.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StayService(
    private val stayRepository: StayRepository
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

    fun clear() {
        stayRepository.deleteAll()
    }
}