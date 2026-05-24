package com.allenarcher.guest.database

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface GuestRepository : JpaRepository<Guest, Long>

interface StayRepository : JpaRepository<Stay, Long> {
    fun findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(
        from: LocalDate,
        to: LocalDate
    ): List<Stay>
}