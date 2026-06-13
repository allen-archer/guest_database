package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.Guest
import com.allenarcher.guest.database.models.Stay
import com.allenarcher.guest.database.models.StayStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface GuestRepository : JpaRepository<Guest, Long> {
    fun findByExternalId(externalId: Long): Guest?
}

interface StayRepository : JpaRepository<Stay, Long> {
    fun findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(
        from: LocalDate,
        to: LocalDate
    ): List<Stay>

    fun findByCheckInGreaterThanEqualAndCheckOutLessThanEqualAndStatus(
        from: LocalDate,
        to: LocalDate,
        status: StayStatus
    ): List<Stay>

    fun findByExternalId(externalId: Long): Stay?
    fun findByConfirmationCode(confirmationCode: String): Stay?
    fun findByGuestIsNull(): List<Stay>
    fun findByCheckInEquals(checkIn: LocalDate): List<Stay>
    fun findByGuest_ExternalIdAndStatusNotAndCheckOutBeforeOrderByCheckInDesc(guestExternalId: Long, status: StayStatus, checkOut: LocalDate): List<Stay>
}