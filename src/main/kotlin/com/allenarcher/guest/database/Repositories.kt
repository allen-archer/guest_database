package com.allenarcher.guest.database

import com.allenarcher.guest.database.models.Guest
import com.allenarcher.guest.database.models.Stay
import com.allenarcher.guest.database.models.StayStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface GuestRepository : JpaRepository<Guest, Long> {
    fun findByExternalId(externalId: Long): Guest?

    @Query("""
        SELECT DISTINCT g FROM Guest g WHERE
        (:name IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:email IS NULL OR EXISTS (SELECT e FROM g.emails e WHERE LOWER(e.address) LIKE LOWER(CONCAT('%', :email, '%'))))
        AND (:city IS NULL OR EXISTS (SELECT a FROM g.addresses a WHERE LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))))
        AND (:state IS NULL OR EXISTS (SELECT a FROM g.addresses a WHERE LOWER(a.state) = LOWER(:state)))
        AND (:street IS NULL OR EXISTS (SELECT a FROM g.addresses a WHERE LOWER(a.street) LIKE LOWER(CONCAT('%', :street, '%'))))
        AND (:phone IS NULL OR EXISTS (SELECT p FROM g.phones p WHERE p.number LIKE CONCAT('%', :phone, '%')))
        AND (:zip IS NULL OR EXISTS (SELECT p FROM g.addresses p WHERE p.zip LIKE CONCAT('%', :zip, '%')))
    """)
    fun searchGuests(
        @Param("name") name: String?,
        @Param("email") email: String?,
        @Param("city") city: String?,
        @Param("state") state: String?,
        @Param("street") street: String?,
        @Param("phone") phone: String?,
        @Param("zip") zip: String?
    ): List<Guest>
}

interface StayRepository : JpaRepository<Stay, Long> {
    fun findByCheckInGreaterThanEqualAndCheckOutLessThanEqual(
        from: LocalDate,
        to: LocalDate
    ): List<Stay>

    fun findByCheckInGreaterThanEqualAndCheckInLessThanEqualAndStatus(
        from: LocalDate,
        to: LocalDate,
        status: StayStatus
    ): List<Stay>

    fun findByCheckOutGreaterThanEqualAndCheckOutLessThanEqualAndCheckInBeforeAndStatus(
        from: LocalDate,
        to: LocalDate,
        checkInBefore: LocalDate,
        status: StayStatus
    ): List<Stay>

    fun findByExternalId(externalId: Long): Stay?
    fun findByConfirmationCode(confirmationCode: String): Stay?
    fun findByGuestIsNull(): List<Stay>
    fun findByCheckInEquals(checkIn: LocalDate): List<Stay>
    fun findByGuest_ExternalIdAndStatusNotAndCheckOutBeforeOrderByCheckInDesc(guestExternalId: Long, status: StayStatus, checkOut: LocalDate): List<Stay>
    fun findByCheckInBeforeAndCheckOutAfterAndStatus(checkIn: LocalDate, checkOut: LocalDate, status: StayStatus): List<Stay>
    fun findByStatusNotAndCheckOutLessThanEqualOrderByCheckOutDesc(status: StayStatus, checkOut: LocalDate): List<Stay>
}